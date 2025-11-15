package tiameds.com.tiameds.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting filter.
 * 
 * Note: User-based rate limiting is handled in the controllers (AuthController, UserController)
 * where the username is available from the request body. This filter is kept for potential
 * future enhancements but currently passes through all requests.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // User-based rate limiting is handled in controllers where username is available
        // This filter passes through - no IP-based blocking
        filterChain.doFilter(request, response);
    }
}
