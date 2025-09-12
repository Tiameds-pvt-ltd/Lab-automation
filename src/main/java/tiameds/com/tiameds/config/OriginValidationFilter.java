package tiameds.com.tiameds.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final String TRUSTED_ORIGIN = "https://lab-test-env.tiameds.ai";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        // Allow preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isTrusted = TRUSTED_ORIGIN.equals(origin) ||
                (referer != null && referer.startsWith(TRUSTED_ORIGIN));

        if (!isTrusted) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized origin");
            return;
        }

        filterChain.doFilter(request, response);
    }
}