package tiameds.com.tiameds.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;

@RestController
@RequestMapping("/auth")
public class LogoutController {

    private final UserRepository userRepository;
    public LogoutController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return ApiResponseHelper.errorResponse("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        return ApiResponseHelper.successResponse("Logged out successfully", null);
    }
}
