package tiameds.com.tiameds.controller.sampleassociation;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.SampleDto;
import tiameds.com.tiameds.services.lab.SampleAssociationService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.List;

@RestController
@RequestMapping("/lab")
@Tag(name = "Sample Association", description = "manage the sample associations in the lab")
public class SampleAssociation {

    private final UserAuthService userAuthService;
    private final SampleAssociationService sampleAssociationService;


    public SampleAssociation(UserAuthService userAuthService, SampleAssociationService sampleAssociationService) {
        this.userAuthService = userAuthService;
        this.sampleAssociationService = sampleAssociationService;
    }

    // Get all sample associations of a respective lab
    @GetMapping("/sample-list")
    public ResponseEntity<?> getSampleAssociationList(
            @RequestHeader("Authorization") String token){

        //check if the token is valid
        if (userAuthService.authenticateUser(token).isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Delegate to the service layer
        List<SampleDto> sampleAssociationList = sampleAssociationService.getSampleAssociationList();

        return ApiResponseHelper.successResponse("Sample associations retrieved successfully", sampleAssociationList);
    }


    // create sample
    @PostMapping("/sample")
    public ResponseEntity<?> createSample(
            @RequestBody SampleDto sampleDto,
            @RequestHeader("Authorization") String token) {

        // Check if the token is valid
        if (userAuthService.authenticateUser(token).isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Delegate to the service layer
        return sampleAssociationService.createSample(sampleDto);
    }


    // update sample
    @PutMapping("/sample/{sampleId}")
    public ResponseEntity<?> updateSample(
            @PathVariable("sampleId") Long sampleId,
            @RequestBody SampleDto sampleDto,
            @RequestHeader("Authorization") String token) {

        try {
            // Authenticate user
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            sampleAssociationService.updateSample(sampleId, sampleDto);

            return ApiResponseHelper.successResponse("Sample updated successfully", sampleDto);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // delete sample
    @DeleteMapping("/sample/{sampleId}")
    public ResponseEntity<?> deleteSample(
            @PathVariable("sampleId") Long sampleId,
            @RequestHeader("Authorization") String token) {

        try {
            // Authenticate user
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            sampleAssociationService.deleteSample(sampleId);

            return ApiResponseHelper.successResponse("Sample deleted successfully", null);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }



}
