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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
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
import tiameds.com.tiameds.services.auth.PasswordResetRateLimitService;
import tiameds.com.tiameds.services.auth.PasswordResetService;
import tiameds.com.tiameds.services.email.EmailService;
import tiameds.com.tiameds.dto.auth.ForgotPasswordRequest;
import tiameds.com.tiameds.dto.auth.ResetPasswordRequest;
import tiameds.com.tiameds.entity.PasswordResetToken;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.JwtUtil;
import tiameds.com.tiameds.utils.PasswordValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

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
    private final PasswordResetService passwordResetService;
    private final PasswordResetRateLimitService passwordResetRateLimitService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${password.reset.url:https://app.com/reset-password}")
    private String passwordResetUrl;
    
    @Value("${server.base-url:http://localhost:8080/api/v1}")
    private String backendBaseUrl;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                                    HttpServletRequest request) {
        String usernameOrEmail = loginRequest.getUsername();

        // Check user-based rate limiting (blocks user for 10 minutes if limit exceeded)
        if (!rateLimitService.isUserAllowed(usernameOrEmail)) {
            int windowMinutes = rateLimitService.getWindowMinutes();
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, 
                String.format("Too many login attempts for this user. Please try again after %d minutes.", windowMinutes));
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        String email = request.getEmail().toLowerCase().trim();
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Validate email format (already validated by @Email annotation, but double-check)
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.warn("Invalid email format in forgot password request: {}", email);
            // Return generic success to prevent enumeration
            return successResponse(HttpStatus.OK, 
                "If an account exists with this email, a password reset link has been sent.", 
                new HashMap<>());
        }

        // Rate limiting: check both email and IP
        if (!passwordResetRateLimitService.isAllowed(email, clientIp)) {
            log.warn("Rate limit exceeded for forgot password: email={}, ip={}", email, clientIp);
            auditPasswordResetAction(null, "FORGOT_PASSWORD_RATE_LIMITED", clientIp, userAgent, email);
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, 
                "Too many password reset requests. Please try again later.");
        }

        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        // Always return generic success message to prevent user enumeration
        // This is a security best practice
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "If an account exists with this email, a password reset link has been sent.");

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            try {
                // Generate and save password reset token
                String resetToken = passwordResetService.createPasswordResetToken(user);
                
                // Send password reset email
                emailService.sendPasswordResetEmail(user.getEmail(), resetToken, passwordResetUrl, backendBaseUrl);
                
                log.info("Password reset token generated and email sent for user: {}", email);
                
                // Audit log
                auditPasswordResetAction(user.getId(), "FORGOT_PASSWORD_REQUESTED", clientIp, userAgent, email);
                
            } catch (EmailService.EmailException e) {
                log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
                // Still return generic success to prevent enumeration
                auditPasswordResetAction(user.getId(), "FORGOT_PASSWORD_EMAIL_FAILED", clientIp, userAgent, email);
            } catch (Exception e) {
                log.error("Unexpected error processing forgot password for {}: {}", email, e.getMessage(), e);
                auditPasswordResetAction(user.getId(), "FORGOT_PASSWORD_ERROR", clientIp, userAgent, email);
            }
        } else {
            // User doesn't exist - still return generic success
            log.debug("Forgot password request for non-existent email: {}", email);
            auditPasswordResetAction(null, "FORGOT_PASSWORD_NON_EXISTENT", clientIp, userAgent, email);
        }

        return successResponse(HttpStatus.OK, 
            "If an account exists with this email, a password reset link has been sent.", 
            responseData);
    }

    /**
     * Validates password reset token from email link
     * This endpoint can be called by frontend to validate token before showing reset form
     * Always returns JSON response for frontend consumption
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateResetToken(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            // URL decode the token in case it was encoded (handle + signs and special chars)
            String decodedToken;
            try {
                decodedToken = java.net.URLDecoder.decode(token, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                // If decoding fails, use token as-is (might already be decoded)
                log.debug("Token URL decoding failed, using token as-is: {}", e.getMessage());
                decodedToken = token;
            }
            
            // Validate token
            PasswordResetToken resetToken = passwordResetService.validateToken(decodedToken);
            
            log.info("Valid password reset token validated for user: {}", resetToken.getUser().getEmail());
            auditPasswordResetAction(resetToken.getUser().getId(), "VALIDATE_RESET_TOKEN_SUCCESS", 
                clientIp, userAgent, resetToken.getUser().getEmail());
            
            // Return JSON response with token validity
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("valid", true);
            responseData.put("message", "Token is valid");
            responseData.put("email", resetToken.getUser().getEmail()); // Optional: return email for display
            return successResponse(HttpStatus.OK, "Token is valid", responseData);
            
        } catch (PasswordResetService.PasswordResetException e) {
            // Token is invalid/expired
            log.warn("Invalid password reset token validation attempt from IP: {} - {}", clientIp, e.getMessage());
            auditPasswordResetAction(null, "VALIDATE_RESET_TOKEN_INVALID", clientIp, userAgent, null);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("valid", false);
            responseData.put("message", e.getMessage());
            return successResponse(HttpStatus.OK, e.getMessage(), responseData);
            
        } catch (IllegalArgumentException e) {
            // Invalid token format
            log.warn("Invalid token format from IP: {} - {}", clientIp, e.getMessage());
            auditPasswordResetAction(null, "VALIDATE_RESET_TOKEN_INVALID_FORMAT", clientIp, userAgent, null);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("valid", false);
            responseData.put("message", "Invalid token format");
            return successResponse(HttpStatus.OK, "Invalid token format", responseData);
            
        } catch (Exception e) {
            log.error("Unexpected error validating reset token: {}", e.getMessage(), e);
            auditPasswordResetAction(null, "VALIDATE_RESET_TOKEN_ERROR", clientIp, userAgent, null);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("valid", false);
            responseData.put("message", "An error occurred while validating the token");
            return successResponse(HttpStatus.OK, "An error occurred while validating the token. Please try again later.", responseData);
        }
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        String token = request.getToken();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        // Validate password strength
        PasswordValidator.ValidationResult validationResult = PasswordValidator.validate(newPassword);
        if (!validationResult.isValid()) {
            return errorResponse(HttpStatus.BAD_REQUEST, validationResult.getErrorMessage());
        }

        // Validate and get token
        PasswordResetToken resetToken;
        try {
            resetToken = passwordResetService.validateToken(token);
        } catch (PasswordResetService.PasswordResetException e) {
            log.warn("Invalid password reset token attempt from IP: {}", clientIp);
            auditPasswordResetAction(null, "RESET_PASSWORD_INVALID_TOKEN", clientIp, userAgent, null);
            return errorResponse(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }

        User user = resetToken.getUser();

        // Check if password is the same as current password (optional: prevent reuse)
        // This is optional but recommended for security
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return errorResponse(HttpStatus.BAD_REQUEST, 
                "New password must be different from your current password");
        }

        try {
            // Update password
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(hashedPassword);
            
            // Increment token version to invalidate all existing sessions
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);

            // Mark token as used
            passwordResetService.markTokenAsUsed(resetToken);

            // Send confirmation email
            try {
                emailService.sendPasswordResetConfirmationEmail(user.getEmail());
            } catch (EmailService.EmailException e) {
                log.warn("Failed to send password reset confirmation email to {}: {}", 
                    user.getEmail(), e.getMessage());
                // Don't fail the request if email fails
            }

            log.info("Password reset successful for user: {}", user.getEmail());
            
            // Audit log
            auditPasswordResetAction(user.getId(), "RESET_PASSWORD_SUCCESS", clientIp, userAgent, user.getEmail());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Password has been reset successfully");
            
            return successResponse(HttpStatus.OK, "Password has been reset successfully", responseData);

        } catch (Exception e) {
            log.error("Unexpected error resetting password for user {}: {}", 
                user.getEmail(), e.getMessage(), e);
            auditPasswordResetAction(user.getId(), "RESET_PASSWORD_ERROR", clientIp, userAgent, user.getEmail());
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "An error occurred while resetting password. Please try again later.");
        }
    }

    /**
     * Helper method to create audit logs for password reset actions
     */
    private void auditPasswordResetAction(Long userId, String action, String ipAddress, 
                                         String userAgent, String email) {
        try {
            LabAuditLogs auditLog = new LabAuditLogs();
            auditLog.setModule("Authentication");
            auditLog.setEntityType("PasswordReset");
            auditLog.setLab_id("GLOBAL");
            auditLog.setActionType(action);
            auditLog.setUserId(userId);
            auditLog.setUsername(email != null ? email : "unknown");
            auditLog.setIpAddress(ipAddress != null ? ipAddress : "");
            auditLog.setDeviceInfo(userAgent != null ? userAgent : "");
            auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
            
            if (email != null) {
                // Create simple JSON string for audit log
                String jsonValue = String.format("{\"email\":\"%s\",\"action\":\"%s\"}", email, action);
                auditLog.setNewValue(jsonValue);
            }
            
            auditLogService.persistAsync(auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log for password reset action: {}", e.getMessage());
            // Don't fail the request if audit logging fails
        }
    }
}


