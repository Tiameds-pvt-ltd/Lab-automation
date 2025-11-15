package tiameds.com.tiameds.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tiameds.com.tiameds.config.JwtProperties;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.utils.JwtUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/send-otp",
            "/auth/verify-otp",
            "/auth/refresh",
            "/public/login",
            "/public/register"
    );

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && !"/".equals(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return PUBLIC_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveAccessToken(request);
        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.parseAccessToken(token);
            String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                writeUnauthorized(response, "Invalid access token subject");
                return;
            }

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                writeUnauthorized(response, "User associated with token not found");
                return;
            }

            Integer tokenVersionClaim = getTokenVersion(claims);
            if (!Objects.equals(tokenVersionClaim, user.getTokenVersion())) {
                writeUnauthorized(response, "Token version mismatch");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            chain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            log.debug("Access token expired: {}", ex.getMessage());
            writeUnauthorized(response, "Access token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Access token validation failed: {}", ex.getMessage());
            writeUnauthorized(response, "Invalid access token");
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String cookieToken = extractTokenFromCookies(request.getCookies());
        if (StringUtils.hasText(cookieToken)) {
            return cookieToken;
        }

        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String extractTokenFromCookies(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        String cookieName = jwtProperties.getAccessCookieName();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
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

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = String.format("{\"status\":\"error\",\"message\":\"%s\"}", message);
        response.getWriter().write(body);
    }
}

