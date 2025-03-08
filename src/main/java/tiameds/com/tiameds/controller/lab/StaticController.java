package tiameds.com.tiameds.controller.lab;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.StaticDto;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.StaticServices;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;
import java.util.Optional;

@RestController
@RequestMapping("/lab/static")
public class StaticController {

    private final StaticServices staticServices;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;

    public StaticController(StaticServices staticServices, UserAuthService userAuthService, LabAccessableFilter labAccessableFilter) {
        this.staticServices = staticServices;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
    }

    @GetMapping("/{labId}")
    public ResponseEntity<?> getStaticData(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // Validate token format
        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // delegate to service
        StaticDto staticData = staticServices.getStaticData(labId, startDate, endDate);

        return ApiResponseHelper.successResponse("Static data fetched successfully", staticData);

    }
}
