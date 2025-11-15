package tiameds.com.tiameds.controller.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.config.JwtProperties;
import tiameds.com.tiameds.config.RateLimitConfig;
import tiameds.com.tiameds.dto.auth.LoginRequest;
import tiameds.com.tiameds.dto.auth.LoginResponse;
import tiameds.com.tiameds.dto.auth.SendOtpRequest;
import tiameds.com.tiameds.dto.auth.VerifyOtpRequest;
import tiameds.com.tiameds.dto.lab.ModuleDTO;
import tiameds.com.tiameds.entity.ModuleEntity;
import tiameds.com.tiameds.entity.RefreshToken;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.auth.RefreshTokenException;
import tiameds.com.tiameds.services.auth.RefreshTokenService;
import tiameds.com.tiameds.services.auth.UserDetailsServiceImpl;
import tiameds.com.tiameds.services.auth.OtpService;
import tiameds.com.tiameds.services.email.EmailService;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.JwtUtil;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class  AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitConfig.RateLimitService rateLimitService;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;
    private final EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                                    HttpServletRequest request) {
        String usernameOrEmail = loginRequest.getUsername();
        String clientIp = getClientIpAddress(request);

        if (!rateLimitService.isIpAllowed(clientIp)) {
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts from this IP address. Please try again later.");
        }
        if (!rateLimitService.isUserAllowed(usernameOrEmail)) {
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts for this user. Please try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, loginRequest.getPassword())
            );
        } catch (BadCredentialsException ex) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Incorrect username or password");
        }

        User user = findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // Extract email from database (not from client input)
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "User email not found. Please contact support.");
        }

        // Check rate limiting for OTP requests
        if (otpService.isRateLimited(email)) {
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, 
                "Too many OTP requests. Please try again after 5 minutes.");
        }

        try {
            // Generate OTP
            String otp = otpService.generateOtp();
            
            // Save hashed OTP to database
            otpService.saveOtp(email, otp);
            
            // Send OTP via email (SMTP)
            emailService.sendOtpEmail(email, otp);
            
            log.info("OTP sent successfully to email: {} after successful password authentication", email);
            
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("message", "OTP sent to your registered email address");
            
            return successResponse(HttpStatus.OK, "OTP sent to your registered email", data);
        } catch (EmailService.EmailException e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to send OTP email. Please try again later.");
        } catch (Exception e) {
            log.error("Unexpected error sending OTP: {}", e.getMessage(), e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "An error occurred while sending OTP. Please try again later.");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(HttpServletRequest request) {
        String refreshTokenValue = extractCookieValue(request.getCookies(), jwtProperties.getRefreshCookieName());
        if (!StringUtils.hasText(refreshTokenValue)) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Refresh token is missing");
        }

        Claims claims;
        try {
            claims = jwtUtil.parseRefreshToken(refreshTokenValue);
        } catch (ExpiredJwtException ex) {
            return respondWithExpiredRefreshToken("Refresh token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            return respondWithExpiredRefreshToken("Refresh token is invalid");
        }

        String username = claims.getSubject();
        Integer tokenVersionClaim = getTokenVersion(claims);
        UUID tokenId = getTokenId(claims);

        if (!StringUtils.hasText(username) || tokenId == null) {
            return respondWithExpiredRefreshToken("Refresh token is malformed");
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return respondWithExpiredRefreshToken("User not found");
        }

        if (!Objects.equals(tokenVersionClaim, user.getTokenVersion())) {
            return respondWithExpiredRefreshToken("Token version mismatch");
        }

        RefreshToken storedToken;
        try {
            storedToken = refreshTokenService.validateToken(tokenId, refreshTokenValue);
        } catch (RefreshTokenException ex) {
            return respondWithRefreshTokenError(ex.getStatus(), ex.getMessage());
        }

        if (!storedToken.getUser().getId().equals(user.getId())) {
            return respondWithExpiredRefreshToken("Refresh token user mismatch");
        }

        refreshTokenService.revokeToken(storedToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        LoginResponse loginResponse = buildLoginResponse(user);
        JwtUtil.JwtToken newAccessToken = jwtUtil.generateAccessToken(userDetails.getUsername(), user.getTokenVersion());
        JwtUtil.RefreshJwtToken newRefreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername(), user.getTokenVersion());

        refreshTokenService.saveRefreshToken(user, newRefreshToken.id(), newRefreshToken.value(), newRefreshToken.expiresAt());

        ResponseCookie accessCookie = createTokenCookie(jwtProperties.getAccessCookieName(), newAccessToken.value(), newAccessToken.expiresAt());
        ResponseCookie refreshCookie = createTokenCookie(jwtProperties.getRefreshCookieName(), newRefreshToken.value(), newRefreshToken.expiresAt());

        return successResponse(HttpStatus.OK, "Token refreshed successfully", loginResponse, accessCookie, refreshCookie);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(Authentication authentication,
                                                                   HttpServletRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "User not found");
        }

        String refreshTokenValue = extractCookieValue(request.getCookies(), jwtProperties.getRefreshCookieName());
        if (StringUtils.hasText(refreshTokenValue)) {
            try {
                Claims claims = jwtUtil.parseRefreshToken(refreshTokenValue);
                UUID tokenId = getTokenId(claims);
                if (tokenId != null) {
                    refreshTokenService.revokeToken(tokenId);
                }
            } catch (ExpiredJwtException ex) {
                UUID tokenId = getTokenId(ex.getClaims());
                if (tokenId != null) {
                    refreshTokenService.revokeToken(tokenId);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Skipping refresh token revocation due to invalid token: {}", ex.getMessage());
            }
        }

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        ResponseCookie clearedAccessCookie = expireCookie(jwtProperties.getAccessCookieName());
        ResponseCookie clearedRefreshCookie = expireCookie(jwtProperties.getRefreshCookieName());

        Map<String, Object> data = new HashMap<>();
        data.put("tokenVersion", user.getTokenVersion());

        return successResponse(HttpStatus.OK, "Logged out successfully", data, clearedAccessCookie, clearedRefreshCookie);
    }

    private Optional<User> findByUsernameOrEmail(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier));
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUsername(user.getUsername());
        loginResponse.setEmail(user.getEmail());
        loginResponse.setFirstName(user.getFirstName());
        loginResponse.setLastName(user.getLastName());
        loginResponse.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        loginResponse.setVerified(user.isVerified());
        loginResponse.setModules(user.getModules().stream()
                .map(this::toModuleDto)
                .collect(Collectors.toList()));
        loginResponse.setPhone(user.getPhone());
        loginResponse.setAddress(user.getAddress());
        loginResponse.setCity(user.getCity());
        loginResponse.setState(user.getState());
        loginResponse.setZip(user.getZip());
        loginResponse.setCountry(user.getCountry());
        loginResponse.setEnabled(user.isEnabled());
        return loginResponse;
    }

    private ModuleDTO toModuleDto(ModuleEntity module) {
        return new ModuleDTO(module.getId(), module.getName());
    }

    private ResponseCookie createTokenCookie(String name, String value, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path(jwtProperties.getCookiePath())
                .maxAge(maxAgeSeconds)
                .sameSite(jwtProperties.getSameSite());
        if (StringUtils.hasText(jwtProperties.getCookieDomain())) {
            builder.domain(jwtProperties.getCookieDomain());
        }
        return builder.build();
    }

    private ResponseCookie expireCookie(String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path(jwtProperties.getCookiePath())
                .maxAge(0)
                .sameSite(jwtProperties.getSameSite());
        if (StringUtils.hasText(jwtProperties.getCookieDomain())) {
            builder.domain(jwtProperties.getCookieDomain());
        }
        return builder.build();
    }

    private ResponseEntity<ApiResponse<LoginResponse>> respondWithExpiredRefreshToken(String message) {
        ResponseCookie clearedAccessCookie = expireCookie(jwtProperties.getAccessCookieName());
        ResponseCookie clearedRefreshCookie = expireCookie(jwtProperties.getRefreshCookieName());
        return this.<LoginResponse>errorResponse(HttpStatus.UNAUTHORIZED, message, clearedAccessCookie, clearedRefreshCookie);
    }

    private ResponseEntity<ApiResponse<LoginResponse>> respondWithRefreshTokenError(HttpStatus status, String message) {
        ResponseCookie clearedAccessCookie = expireCookie(jwtProperties.getAccessCookieName());
        ResponseCookie clearedRefreshCookie = expireCookie(jwtProperties.getRefreshCookieName());
        return this.<LoginResponse>errorResponse(status, message, clearedAccessCookie, clearedRefreshCookie);
    }

    private ResponseEntity<ApiResponse<LoginResponse>> successResponse(HttpStatus status,
                                                                       String message,
                                                                       LoginResponse data,
                                                                       ResponseCookie... cookies) {
        ApiResponse<LoginResponse> response = new ApiResponse<>("success", message, data);
        HttpHeaders headers = new HttpHeaders();
        for (ResponseCookie cookie : cookies) {
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return ResponseEntity.status(status).headers(headers).body(response);
    }

    private <T> ResponseEntity<ApiResponse<T>> errorResponse(HttpStatus status,
                                                             String message,
                                                             ResponseCookie... cookies) {
        ApiResponse<T> response = new ApiResponse<>("error", message, null);
        HttpHeaders headers = new HttpHeaders();
        for (ResponseCookie cookie : cookies) {
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return ResponseEntity.status(status).headers(headers).body(response);
    }

    private <T> ResponseEntity<ApiResponse<T>> errorResponse(HttpStatus status, String message) {
        return errorResponse(status, message, new ResponseCookie[0]);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> successResponse(HttpStatus status,
                                                                             String message,
                                                                             Map<String, Object> data,
                                                                             ResponseCookie... cookies) {
        ApiResponse<Map<String, Object>> response = new ApiResponse<>("success", message, data);
        HttpHeaders headers = new HttpHeaders();
        for (ResponseCookie cookie : cookies) {
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return ResponseEntity.status(status).headers(headers).body(response);
    }

    private String extractCookieValue(Cookie[] cookies, String name) {
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Integer getTokenVersion(Claims claims) {
        Object value = claims.get("tokenVersion");
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private UUID getTokenId(Claims claims) {
        String id = claims.getId();
        return id != null ? UUID.fromString(id) : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Check if user exists
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return errorResponse(HttpStatus.NOT_FOUND, "User with this email does not exist");
        }

        // Check rate limiting
        if (otpService.isRateLimited(email)) {
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, 
                "Too many OTP requests. Please try again after 5 minutes.");
        }

        try {
            // Generate OTP
            String otp = otpService.generateOtp();
            
            // Save hashed OTP to database
            otpService.saveOtp(email, otp);
            
            // Send OTP via email (SMTP)
            emailService.sendOtpEmail(email, otp);
            
            log.info("OTP sent successfully to email: {}", email);
            
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            
            return successResponse(HttpStatus.OK, "OTP sent successfully", data);
        } catch (EmailService.EmailException e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to send OTP email. Please try again later.");
        } catch (Exception e) {
            log.error("Unexpected error sending OTP: {}", e.getMessage(), e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "An error occurred while sending OTP. Please try again later.");
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String otp = request.getOtp();

        // Find user by email
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return errorResponse(HttpStatus.NOT_FOUND, "User with this email does not exist");
        }

        try {
            // Validate OTP
            otpService.validateOtp(email, otp);
            
            // OTP is valid, generate JWT tokens using existing logic
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            LoginResponse loginResponse = buildLoginResponse(user);
            
            JwtUtil.JwtToken accessToken = jwtUtil.generateAccessToken(userDetails.getUsername(), user.getTokenVersion());
            JwtUtil.RefreshJwtToken refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername(), user.getTokenVersion());

            refreshTokenService.saveRefreshToken(user, refreshToken.id(), refreshToken.value(), refreshToken.expiresAt());

            ResponseCookie accessCookie = createTokenCookie(jwtProperties.getAccessCookieName(), accessToken.value(), accessToken.expiresAt());
            ResponseCookie refreshCookie = createTokenCookie(jwtProperties.getRefreshCookieName(), refreshToken.value(), refreshToken.expiresAt());

            log.info("OTP verified successfully for email: {}", email);
            
            return successResponse(HttpStatus.OK, "OTP verified successfully", loginResponse, accessCookie, refreshCookie);
        } catch (OtpService.OtpException e) {
            log.warn("OTP validation failed for email {}: {}", email, e.getMessage());
            return errorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error verifying OTP: {}", e.getMessage(), e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "An error occurred while verifying OTP. Please try again later.");
        }
    }
}


