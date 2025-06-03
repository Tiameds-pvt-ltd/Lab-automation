package tiameds.com.tiameds.controller.lab;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.services.lab.PatientService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.Optional;

@Transactional
@RestController
@RequestMapping("/lab")
@Tag(name = "Patient Controller", description = "Endpoints for managing patients in a lab")
@Slf4j
public class PatientController {

    private final PatientService patientService;
    private final UserAuthService userAuthService;
    private final LabRepository labRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final PatientRepository patientRepository;

    public PatientController(PatientService patientService, UserAuthService userAuthService, LabRepository labRepository, LabAccessableFilter labAccessableFilter, PatientRepository patientRepository) {
        this.patientService = patientService;
        this.userAuthService = userAuthService;
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.patientRepository = patientRepository;
    }

    @PostMapping("/{labId}/add-patient")
    public ResponseEntity<?> addPatient(@PathVariable Long labId,
                                        @RequestHeader("Authorization") String token,
                                        @RequestBody PatientDTO patientDTO) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }
            // Check if patient exists (phone AND first name match)
            Optional<PatientEntity> existingPatient = patientService.findByPhoneAndFirstName(
                    patientDTO.getPhone(),
                    patientDTO.getFirstName()
            );
            if (existingPatient.isPresent()) {
                PatientDTO updatedPatient = patientService.addVisitAndBillingToExistingPatient(
                        labOptional.get(),
                        patientDTO,
                        existingPatient.get()
                );
                return ApiResponseHelper.successResponseWithDataAndMessage(
                        "Visit and billing added to existing patient",
                        HttpStatus.OK,
                        updatedPatient
                );
            }
            // New patient
            PatientDTO newPatient = patientService.savePatientWithDetails(labOptional.get(), patientDTO);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "New patient added successfully",
                    HttpStatus.CREATED,
                    newPatient
            );
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{labId}/patients")
    public ResponseEntity<?> getAllPatients(@PathVariable Long labId, @RequestHeader("Authorization") String token) {
        try {
            // Validate token format
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Check if the user is a member of the lab
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

//            return ResponseEntity.ok(patientService.getAllPatientsByLabId(labId));
            return ApiResponseHelper.successResponseWithDataAndMessage("Patients retrieved successfully", HttpStatus.OK, patientService.getAllPatientsByLabId(labId));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{labId}/patient/{patientId}")
    public ResponseEntity<?> getPatientById(@PathVariable Long labId, @PathVariable Long patientId, @RequestHeader("Authorization") String token) {
        try {
            // Validate token format
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Check if the user is a member of the lab
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }
            //check if the patient exists on the lab

//            return ResponseEntity.ok(patientService.getPatientById(patientId, labId));
            return ApiResponseHelper.successResponseWithDataAndMessage("Patient retrieved successfully", HttpStatus.OK, patientService.getPatientById(patientId, labId));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{labId}/update-patient/{patientId}")
    public ResponseEntity<?> updatePatient(
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestHeader("Authorization")
            String token, @RequestBody
            PatientDTO patientDTO) {
        try {
            // Validate token format
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Check if the user is a member of the lab
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            //service to update the patient
            patientService.updatePatient(patientId, labId, patientDTO);
//
//            return ApiResponseHelper.successResponse("Patient updated successfully", HttpStatus.OK);
            return ApiResponseHelper.successResponseWithDataAndMessage("Patient updated successfully", HttpStatus.OK, patientDTO);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{labId}/delete-patient/{patientId}")
    public ResponseEntity<?> deletePatient(@PathVariable Long labId, @PathVariable Long patientId, @RequestHeader("Authorization") String token) {
        try {
            // Validate token format
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }
            patientService.deletePatient(patientId, labId);
            return ApiResponseHelper.successResponse("Patient deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @PutMapping("/{labId}/update-patient-details/{patientId}")
    public ResponseEntity<?> updatePatientDetails(
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestHeader("Authorization") String token,
            @RequestBody PatientDTO patientDTO) {
        try {
            // Authentication and authorization checks
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            // Check if patient exists and belongs to the lab
            Optional<PatientEntity> patientOptional = patientRepository.findById(patientId);
            if (patientOptional.isEmpty() || !patientOptional.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
            }

            // Update patient
            PatientDTO updatedPatient = patientService.updatePatientDetails(patientOptional.get(), labOptional.get(), patientDTO);

            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Patient updated successfully",
                    HttpStatus.OK,
                    updatedPatient
            );
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


}
