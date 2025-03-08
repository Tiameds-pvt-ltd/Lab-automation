package tiameds.com.tiameds.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.User;
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

        // Extract the username from the token
        String currentUsername = jwtUtils.extractUsername(token.substring(7));

        // Fetch user details using the username
        return userService.findByUsername(currentUsername);
    }

}
