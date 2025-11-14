package tiameds.com.tiameds.controller.lab;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.BillingService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.util.List;
import java.util.Optional;

@Transactional
@RestController
@RequestMapping("/lab")
@Tag(name = "Billing Controller", description = "Manage billing in the lab")
public class BillingController {
    private final BillingService billingService;
    private final UserService userService;
    private final LabAccessableFilter labAccessableFilter;
    public BillingController(BillingService billingService,
                             UserService userService,
                             LabAccessableFilter labAccessableFilter) {
        this.billingService = billingService;
        this.userService = userService;
        this.labAccessableFilter = labAccessableFilter;
    }
    // Get all billings of a respective lab
    @GetMapping("/{labId}/billing")
    public ResponseEntity<?> getBillingList(
            BillingDTO billingDTO,
            @PathVariable("labId") Long labId) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            List<BillingDTO> billingList = billingService.getBillingList(labId, currentUser, billingDTO);
            return ApiResponseHelper.successResponse("Billing list fetched successfully", billingList);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
//    Additional billing endpoints can be reintroduced here as needed.

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof MyUserDetails myUserDetails) {
            return userService.findByUsername(myUserDetails.getUsername());
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
