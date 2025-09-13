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
import tiameds.com.tiameds.config.IpWhitelistConfig;

import java.io.IOException;

@Slf4j
@Component
public class IpWhitelistFilter extends OncePerRequestFilter {

    @Autowired
    private IpWhitelistConfig ipWhitelistConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        
        log.debug("Checking IP whitelist for IP: {}", clientIp);

        if (!ipWhitelistConfig.isIpAllowed(clientIp)) {
            log.warn("Access denied for IP: {} - not in whitelist", clientIp);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Your IP address is not authorized to access this service\"}");
            return;
        }

        log.debug("IP {} is allowed, proceeding with request", clientIp);
        filterChain.doFilter(request, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply IP whitelist to login endpoints
        String requestURI = request.getRequestURI();
        return !requestURI.contains("/login") && !requestURI.contains("/auth");
    }
}
