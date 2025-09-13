package tiameds.com.tiameds.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tiameds.com.tiameds.config.RateLimitConfig;

import java.io.IOException;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimitConfig.RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String requestURI = request.getRequestURI();

        // Apply rate limiting only to login endpoints
        if (requestURI.contains("/login") || requestURI.contains("/auth")) {
            
            // Check IP-based rate limiting
            if (!rateLimitService.isIpAllowed(clientIp)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many login attempts from this IP. Please try again later.\"}");
                return;
            }

            // Check user-based rate limiting (if username is available in request)
            String username = getUsernameFromRequest(request);
            if (username != null && !rateLimitService.isUserAllowed(username)) {
                log.warn("Rate limit exceeded for user: {} from IP: {}", username, clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many login attempts for this user. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String getUsernameFromRequest(HttpServletRequest request) {
        // Try to extract username from request body for login attempts
        // This is a simplified approach - in production, you might want to parse the JSON body
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            // For now, we'll extract username in the controller
            // This method can be enhanced to parse JSON body if needed
            return null;
        }
        return null;
    }
}
