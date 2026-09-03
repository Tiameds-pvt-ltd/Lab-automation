package tiameds.com.tiameds.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;


import java.util.Optional;

@Service
public class UserAuthService {

    private final JwtUtil jwtUtils;
    private final UserService userService;

    @Autowired
    public UserAuthService(JwtUtil jwtUtils, UserService userService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    public Optional<User> authenticateUser(String token) {
        // Validate token format
        if (!token.startsWith("Bearer ")) {
            return Optional.empty();
        }

        // JwtFilter has already validated this exact token for this request and
        // populated the SecurityContext with the authenticated user — reuse it
        // instead of re-parsing the token and re-fetching the user from the DB
        // a second time. Falls back to the original lookup if, for any reason,
        // the SecurityContext wasn't populated (e.g. this method is ever called
        // outside a normal filtered request), so behavior is unchanged in that case.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof MyUserDetails userDetails) {
            return Optional.of(userDetails.getUser());
        }

        // Extract the username from the token
        String currentUsername = jwtUtils.extractUsername(token.substring(7));

        // Fetch user details using the username
        return userService.findByUsername(currentUsername);
    }

}
