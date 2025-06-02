package tiameds.com.tiameds.controller.superAdmin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.LabListDTO;
import tiameds.com.tiameds.services.superAdmin.LabSuperAdminService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.List;

@RestController
@RequestMapping("api/v1/lab-super-admin")
@Tag(name = "Lab Super Admin Controller", description = "Operations pertaining to lab super admin management")
public class LabSuperAdminController {

    private final LabSuperAdminService labSuperAdminService;
    private final UserAuthService userAuthService;

    public LabSuperAdminController(LabSuperAdminService labSuperAdminService, UserAuthService userAuthService) {
        this.labSuperAdminService = labSuperAdminService;
        this.userAuthService = userAuthService;
    }
    @GetMapping("/labs")
    public ResponseEntity<?> getLabs(@RequestHeader("Authorization") String token) {
        try {
            // Retrieve the list of labs
            List<LabListDTO> labs = labSuperAdminService.getLabs();
            return ApiResponseHelper.successResponse("Labs retrieved successfully", labs);
        }catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    //allow lab to isActive or not
    @PutMapping("lab/{labId}/isActive")
    public ResponseEntity<?> updateLabStatus(@PathVariable long labId, @RequestBody LabListDTO labListDTO) {
        try {
            if (labListDTO.getIsActive() == null) {
                return ApiResponseHelper.errorResponse("isActive field is required", HttpStatus.BAD_REQUEST);
            }
            labSuperAdminService.updateLabStatus(labId, labListDTO);
            return ApiResponseHelper.successResponse("Lab status updated successfully", null);
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("Internal Server Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
