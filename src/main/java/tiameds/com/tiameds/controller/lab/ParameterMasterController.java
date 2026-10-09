package tiameds.com.tiameds.controller.lab;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.entity.ParameterMasterEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.ParameterMasterService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/lab/parameter-master")
public class ParameterMasterController {

    private final ParameterMasterService parameterMasterService;
    private final UserService userService;

    public ParameterMasterController(ParameterMasterService parameterMasterService, UserService userService) {
        this.parameterMasterService = parameterMasterService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getAllParameters() {
        Optional<User> currentUser = getAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        List<ParameterMasterEntity> parameters = parameterMasterService.getAllActiveParameters();
        return ApiResponseHelper.successResponse("Parameters fetched successfully", parameters);
    }

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof MyUserDetails myUserDetails) {
            return Optional.of(myUserDetails.getUser());
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.findByUsername(userDetails.getUsername());
        }
        if (principal instanceof String username && !"anonymousUser".equalsIgnoreCase(username)) {
            return userService.findByUsername(username);
        }
        return Optional.empty();
    }
}
