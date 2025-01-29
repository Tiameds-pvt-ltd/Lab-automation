package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.DoctorDTO;
import tiameds.com.tiameds.services.lab.DoctorService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

@Transactional
@RestController
@RequestMapping("/admin/lab")
@Tag(name = "Doctor Controller", description = "manage the doctors in the lab")
public class DoctorController {

    private final DoctorService doctorService;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;

    public DoctorController(DoctorService doctorService, UserAuthService userAuthService, LabAccessableFilter labAccessableFilter) {
        this.doctorService = doctorService;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
    }

    // create doctor or add doctor to lab
    @PostMapping("{labId}/doctors")
    public ResponseEntity<?> addDoctorToLab(
            @PathVariable("labId") Long labId,
            @RequestBody DoctorDTO doctorDTO,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate user
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            // Delegate to the service layer
            doctorService.addDoctorToLab(labId, doctorDTO);

            return ApiResponseHelper.successResponse("Doctor added successfully", doctorDTO);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // update doctor
    @PutMapping("{labId}/doctors/{doctorId}")
    public ResponseEntity<?> updateDoctor(
            @PathVariable("labId") Long labId,
            @PathVariable("doctorId") Long doctorId,
            @RequestBody DoctorDTO doctorDTO,
            @RequestHeader("Authorization") String token) {

        try {
            // Authenticate user
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            doctorService.updateDoctor(labId, doctorId, doctorDTO);

            return ApiResponseHelper.successResponse("Doctor updated successfully", doctorDTO);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // delete doctor
    @DeleteMapping("{labId}/doctors/{doctorId}")
    public ResponseEntity<?> deleteDoctor(
            @PathVariable("labId") Long labId,
            @PathVariable("doctorId") Long doctorId,
            @RequestHeader("Authorization") String token) {

        try {
            // Authenticate user
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            doctorService.deleteDoctor(labId, doctorId);

            return ApiResponseHelper.successResponse("Doctor deleted successfully", null);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // get all doctors
    @GetMapping("{labId}/doctors")
    public ResponseEntity<?> getAllDoctors(
            @PathVariable("labId") Long labId,
            @RequestHeader("Authorization") String token) {

        try {
            // Authenticate user
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            return ApiResponseHelper.successResponse("Doctors retrieved successfully", doctorService.getAllDoctors(labId));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // get doctor by id
    @GetMapping("{labId}/doctors/{doctorId}")
    public ResponseEntity<?> getDoctorById(
            @PathVariable("labId") Long labId,
            @PathVariable("doctorId") Long doctorId,
            @RequestHeader("Authorization") String token) {

        try {
            // Authenticate user
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            return ApiResponseHelper.successResponse("Doctor retrieved successfully", doctorService.getDoctorById(labId, doctorId));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
