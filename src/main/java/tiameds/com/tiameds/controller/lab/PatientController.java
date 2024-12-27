package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.lab.PatientService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.Optional;


@RestController
@RequestMapping("/lab")
@Tag(name = "Patient Controller", description = "Endpoints for managing patients in a lab")
public class PatientController {

    private final PatientService patientService;
    private final UserAuthService userAuthService;
    private final LabRepository labRepository;
    private final LabAccessableFilter labAccessableFilter;


    public PatientController(PatientService patientService, UserAuthService userAuthService, LabRepository labRepository, LabAccessableFilter labAccessableFilter) {
        this.patientService = patientService;
        this.userAuthService = userAuthService;
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
    }

    // Add your code here

    //add patient
//    @PostMapping("/{labId}/add-patient")
//    public ResponseEntity<?> addPatient(
//            @PathVariable Long labId,
//            @RequestHeader("Authorization") String token,
//            @RequestBody PatientDTO patientDTO
//    ) {
//        try {
//            // Validate token format
//            Optional<User> currentUser = userAuthService.authenticateUser(token);
//            if (currentUser.isEmpty()) {
//                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
//            }
//
//            // Check if the lab exists
//            Optional<Lab> labOptional = labRepository.findById(labId);
//            if (labOptional.isEmpty()) {
//                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//            }
//
//            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
//            if (isAccessible == false) {
//                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
//            }
//
//            // Check if the user is a member of the lab
//            if (!currentUser.get().getLabs().contains(labOptional.get())) {
//                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
//            }
//
//            // Check if the patient already exists by phone
//            if (patientService.existsByPhone(patientDTO.getPhone())) {
//                return ApiResponseHelper.errorResponse("Patient with this phone number already exists", HttpStatus.BAD_REQUEST);
//            }
//
//            // Add patient
//            patientService.savePatientWithDetails(labOptional.get(), patientDTO);
//
//            return ApiResponseHelper.successResponseWithDataAndMessage("Patient added successfully", HttpStatus.CREATED, patientDTO);
//
//
//        } catch (Exception e) {
//            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//    }

//    ===================================


//    @PostMapping("/{labId}/add-patient")
//    public ResponseEntity<?> addPatient(@PathVariable Long labId, @RequestHeader("Authorization") String token, @RequestBody PatientDTO patientDTO) {
//        try {
//            // Validate token format
//            Optional<User> currentUser = userAuthService.authenticateUser(token);
//            if (currentUser.isEmpty()) {
//                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
//            }
//
//            // Check if the lab exists
//            Optional<Lab> labOptional = labRepository.findById(labId);
//            if (labOptional.isEmpty()) {
//                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//            }
//
//            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
//            if (!isAccessible) {
//                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
//            }
//
//            // Check if the user is a member of the lab
//            if (!currentUser.get().getLabs().contains(labOptional.get())) {
//                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
//            }
//
//            // Check if the patient already exists by phone or email
//            Optional<PatientEntity> existingPatient = patientService.findByPhoneOrEmail(patientDTO.getPhone(), patientDTO.getEmail());
//            if (existingPatient.isPresent()) {
//                // If the patient exists, add visit and billing details to existing patient
//                PatientDTO updatedPatientDTO = patientService.addVisitAndBillingToExistingPatient(labOptional.get(), patientDTO, existingPatient.get());
//                return ApiResponseHelper.successResponseWithDataAndMessage("Visit and billing added to existing patient", HttpStatus.OK, updatedPatientDTO);
//            }
//
//            // If patient doesn't exist, create a new patient and add visit and billing details
//            PatientDTO newPatientDTO = patientService.savePatientWithDetails(labOptional.get(), patientDTO);
//            return ApiResponseHelper.successResponseWithDataAndMessage("New patient added successfully", HttpStatus.CREATED, newPatientDTO);
//
//        } catch (Exception e) {
//            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//    }

    @PostMapping("/{labId}/add-patient")
    public ResponseEntity<?> addPatient(@PathVariable Long labId, @RequestHeader("Authorization") String token, @RequestBody PatientDTO patientDTO) {
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
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Check if the user is a member of the lab
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            // Check if the patient already exists by phone or email
            Optional<PatientEntity> existingPatient = patientService.findByPhoneOrEmail(patientDTO.getPhone(), patientDTO.getEmail());
            if (existingPatient.isPresent()) {
                // If the patient exists, add visit and billing details to existing patient
                PatientDTO updatedPatientDTO = patientService.addVisitAndBillingToExistingPatient(labOptional.get(), patientDTO, existingPatient.get());
                return ApiResponseHelper.successResponseWithDataAndMessage("Visit and billing added to existing patient", HttpStatus.OK, updatedPatientDTO);
            }

            // If patient doesn't exist, create a new patient and add visit and billing details
            PatientDTO newPatientDTO = patientService.savePatientWithDetails(labOptional.get(), patientDTO);
            return ApiResponseHelper.successResponseWithDataAndMessage("New patient added successfully", HttpStatus.CREATED, newPatientDTO);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    //get all patients
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


    //get patient by id of the respective lab only
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


    //update the patient details of the respective lab only
    @PutMapping("/{labId}/update-patient/{patientId}")
    public ResponseEntity<?> updatePatient(@PathVariable Long labId, @PathVariable Long patientId, @RequestHeader("Authorization") String token, @RequestBody PatientDTO patientDTO) {
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


    //delete the patient of the respective lab only
    @DeleteMapping("/{labId}/delete-patient/{patientId}")
    public ResponseEntity<?> deletePatient(@PathVariable Long labId, @PathVariable Long patientId, @RequestHeader("Authorization") String token) {
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

            //service to delete the patient
            patientService.deletePatient(patientId, labId);

            return ApiResponseHelper.successResponse("Patient deleted successfully", HttpStatus.OK);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


}
