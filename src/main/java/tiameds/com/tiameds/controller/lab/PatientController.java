package tiameds.com.tiameds.controller.lab;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.Auditable;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.*;
import tiameds.com.tiameds.dto.visits.BillDTO;
import tiameds.com.tiameds.dto.visits.BillDtoDue;
import tiameds.com.tiameds.entity.BillingEntity;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.services.lab.PatientService;
import tiameds.com.tiameds.services.lab.UpdatePatientService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final UpdatePatientService updatePatientService;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;
    private final VisitRepository visitRepository;

    public PatientController(PatientService patientService, UserAuthService userAuthService, LabRepository labRepository, LabAccessableFilter labAccessableFilter, PatientRepository patientRepository, UpdatePatientService updatePatientService, AuditLogService auditLogService, FieldChangeTracker fieldChangeTracker, VisitRepository visitRepository) {
        this.patientService = patientService;
        this.userAuthService = userAuthService;
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.patientRepository = patientRepository;
        this.updatePatientService = updatePatientService;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
        this.visitRepository = visitRepository;
    }

    @Auditable(module = "Lab")
    @GetMapping("/{labId}/patients")
    public ResponseEntity<?> getAllPatients(@PathVariable Long labId, @RequestHeader("Authorization") String token) {
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
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }
            return ApiResponseHelper.successResponseWithDataAndMessage("Patients retrieved successfully", HttpStatus.OK, patientService.getAllPatientsByLabId(labId));
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Auditable(module = "Lab")
    @GetMapping("/{labId}/patient/{patientId}")
    public ResponseEntity<?> getPatientById(@PathVariable Long labId, @PathVariable Long patientId, @RequestHeader("Authorization") String token) {
        try {
            
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

    @Auditable(module = "Lab")
    @PutMapping("/{labId}/update-patient/{patientId}")
    public ResponseEntity<?> updatePatient(
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestHeader("Authorization")
            String token, @RequestBody
            PatientDTO patientDTO) {
        try {
            
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
            patientService.updatePatient(patientId, labId, patientDTO,currentUser.get().getUsername());
//
//            return ApiResponseHelper.successResponse("Patient updated successfully", HttpStatus.OK);
            return ApiResponseHelper.successResponseWithDataAndMessage("Patient updated successfully", HttpStatus.OK, patientDTO);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Auditable(module = "Lab")
    @DeleteMapping("/{labId}/delete-patient/{patientId}")
    public ResponseEntity<?> deletePatient(@PathVariable Long labId, @PathVariable Long patientId, @RequestHeader("Authorization") String token) {
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

//
//    @PutMapping("/{labId}/update-patient-details/{patientId}")
//    public ResponseEntity<?> updatePatientDetails(
//            @PathVariable Long labId,
//            @PathVariable Long patientId,
//            @RequestHeader("Authorization") String token,
//            @RequestBody PatientDTO patientDTO) {
//        try {
//            // Authentication and authorization checks
//            Optional<User> currentUser = userAuthService.authenticateUser(token);
//            if (currentUser.isEmpty()) {
//                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
//            }
//
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
//            if (!currentUser.get().getLabs().contains(labOptional.get())) {
//                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
//            }
//
//            // Check if patient exists and belongs to the lab
//            Optional<PatientEntity> patientOptional = patientRepository.findById(patientId);
//            if (patientOptional.isEmpty() || !patientOptional.get().getLabs().contains(labOptional.get())) {
//                return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
//            }
//
//            // Update patient
//            PatientDTO updatedPatient = patientService.updatePatientDetails(patientOptional.get(), labOptional.get(), patientDTO , currentUser.get().getUsername());
//
//            return ApiResponseHelper.successResponseWithDataAndMessage(
//                    "Patient updated successfully",
//                    HttpStatus.OK,
//                    updatedPatient
//            );
//        } catch (Exception e) {
//            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//    }

    @Auditable(module = "Lab")
    @Transactional
    @GetMapping("/{labId}/search-patient")
    public ResponseEntity<?> searchPatientByPhone(
            @PathVariable Long labId,
            @RequestParam String phone,
            @RequestHeader("Authorization") String token) {
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
                System.out.println("Lab not accessible for current user");
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            if (!currentUser.get().getLabs().contains(labOptional.get())) {
                System.out.println("User is not a member of this lab");
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }
            List<PatientList> result = patientService.getPatientbytheirPhoneAndLabId(phone, labId);
            return ApiResponseHelper.successResponseWithDataAndMessage("Patients retrieved successfully", HttpStatus.OK, result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @Auditable(module = "Lab")
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
            // check if patient exists by phone and first name on the particular lab
            Optional<PatientEntity> existingPatient = patientService.findByPhoneAndFirstNameAndLabsId(
                    patientDTO.getPhone(),
                    patientDTO.getFirstName(),
                    labOptional.get().getId()
            );
            if (existingPatient.isPresent()) {
                PatientDTO updatedPatient = patientService.addVisitAndBillingToExistingPatient(
                        labOptional.get(),
                        patientDTO,
                        existingPatient.get(),
                        currentUser.get().getUsername()
                );
                return ApiResponseHelper.successResponseWithDataAndMessage(
                        "Visit and billing added to existing patient",
                        HttpStatus.OK,
                        updatedPatient
                );
            }
            // New patient
            PatientDTO newPatient = patientService.savePatientWithDetails(labOptional.get(), patientDTO, currentUser.get().getUsername());
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "New patient added successfully",
                    HttpStatus.CREATED,
                    newPatient
            );
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

//    @PostMapping("/{labId}/billing/{billingId}/partial-payment")
//    public ResponseEntity<?> addPartialPayment(
//            @PathVariable Long billingId,
//            @RequestHeader("Authorization") String token,
//            @RequestBody BillDtoDue billDTO
//    ) {
//        try {
//            Optional<User> currentUser = userAuthService.authenticateUser(token);
//            if (currentUser.isEmpty()) {
//                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
//            }
//            Optional<BillingEntity> billingOptional = patientService.getBillingById(billingId);
//            if (billingOptional.isEmpty()) {
//                return ApiResponseHelper.errorResponse("Billing not found", HttpStatus.NOT_FOUND);
//            }
//
//            BillingEntity billing = billingOptional.get();
//            Optional<Lab> labOptional = labRepository.findById(billing.getLabs().iterator().next().getId());
//            if (labOptional.isEmpty()) {
//                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//            }
//
//            BillDTO updatedBill = patientService.addPartialPayment(billing, billDTO, currentUser.get().getUsername());
//            return ApiResponseHelper.successResponseWithDataAndMessage("Partial payment added successfully", HttpStatus.OK, updatedBill);
//        } catch (Exception e) {
//            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//    }

    @PutMapping("/{labId}/visit/{visitId}/cancel")
    public ResponseEntity<?> cancelVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestBody CancellationDataDTO cancellationData,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
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
            patientService.cancelVisit(visitId, labId, currentUser.get().getUsername(), cancellationData);
            
            // Fetch the visit after cancellation to get the patient data
            Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
            PatientDTO patientDTO = null;
            if (visitOptional.isPresent() && visitOptional.get().getPatient() != null) {
                PatientEntity patient = visitOptional.get().getPatient();
                patientDTO = new PatientDTO(patient);
            }
            
            // Create audit log entry with cancellation details
            LabAuditLogs auditLog = new LabAuditLogs();
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setUsername(currentUser.get().getUsername());
            auditLog.setUserId(currentUser.get().getId());
            auditLog.setLab_id(String.valueOf(labId));
            auditLog.setModule("Lab");
            auditLog.setEntityType("Visit");
            auditLog.setEntityId(String.valueOf(visitId));
            auditLog.setActionType("CANCEL");
            auditLog.setChangeReason(cancellationData.getVisitCancellationReason() != null ? cancellationData.getVisitCancellationReason() : "");
            
            // Store complete patient data as JSON in newValue
            if (patientDTO != null) {
                String patientDataJson = fieldChangeTracker.objectToJson(patientDTO);
                if (patientDataJson != null) {
                    auditLog.setNewValue(patientDataJson);
                } else {
                    // Fallback if JSON serialization fails
                    log.warn("Failed to serialize patient data to JSON for visit cancellation");
                    auditLog.setNewValue("{\"status\": \"CANCELLED\", \"reason\": \"" + 
                        (cancellationData.getVisitCancellationReason() != null ? cancellationData.getVisitCancellationReason() : "") + "\"}");
                }
            } else {
                // Fallback if patient data not found
                log.warn("Patient data not found for visit cancellation, visitId: {}", visitId);
                auditLog.setNewValue("{\"status\": \"CANCELLED\", \"reason\": \"" + 
                    (cancellationData.getVisitCancellationReason() != null ? cancellationData.getVisitCancellationReason() : "") + "\"}");
            }
            
            auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
            
            // Set request metadata if available
            if (request != null) {
                auditLog.setIpAddress(request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : request.getRemoteAddr());
                auditLog.setDeviceInfo(request.getHeader("User-Agent"));
                auditLog.setRequestId(request.getHeader("X-Request-ID"));
            }
            
            // Set user role if available
            if (currentUser.get().getRoles() != null && !currentUser.get().getRoles().isEmpty()) {
                auditLog.setRole(currentUser.get().getRoles().iterator().next().getName());
            }
            
            // Persist audit log
            auditLogService.persistAsync(auditLog);
            
            return ApiResponseHelper.successResponse("Visit cancelled successfully", HttpStatus.OK);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @Auditable(module = "Lab")
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


            PatientDTO updatedPatient = updatePatientService.updatePatientDetails(
                    patientOptional.get(),
                    labOptional.get(),
                    patientDTO,
                    currentUser.get().getUsername()
            );

            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Patient updated successfully",
                    HttpStatus.OK,
                    updatedPatient
            );
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }



    @PostMapping("/{labId}/billing/{billingId}/partial-payment")
    public ResponseEntity<?> addPartialPayment(
            @PathVariable Long labId,
            @PathVariable Long billingId,
            @RequestHeader("Authorization") String token,
            @RequestBody BillDtoDue billDTO,
            HttpServletRequest request
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            Optional<BillingEntity> billingOptional = patientService.getBillingById(billingId);
            if (billingOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Billing not found", HttpStatus.NOT_FOUND);
            }

            BillingEntity billing = billingOptional.get();
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }
            if (billing.getLabs() == null || billing.getLabs().stream().noneMatch(l -> Objects.equals(l.getId(), labId))) {
                return ApiResponseHelper.errorResponse("Billing does not belong to this lab", HttpStatus.UNAUTHORIZED);
            }

            // Snapshot before update
            BillDTO oldBillSnapshot = new BillDTO(billing);

            BillDTO updatedBill = patientService.addPartialPayment(billing, billDTO, currentUser.get().getUsername());

            // Create audit log entry for the billing update
            LabAuditLogs auditLog = new LabAuditLogs();
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setUsername(currentUser.get().getUsername());
            auditLog.setUserId(currentUser.get().getId());
            auditLog.setLab_id(String.valueOf(labId));
            auditLog.setModule("Billing");
            auditLog.setEntityType("Billing");
            auditLog.setEntityId(String.valueOf(billingId));
            auditLog.setActionType("PARTIAL_PAYMENT");
            auditLog.setChangeReason(billDTO.getTransaction() != null ?
                    (billDTO.getTransaction().getRemarks() != null ? billDTO.getTransaction().getRemarks() : "") : "");

            // Set request metadata if available
            if (request != null) {
                String ipAddress = request.getHeader("X-Forwarded-For");
                auditLog.setIpAddress(ipAddress != null ? ipAddress : request.getRemoteAddr());
                auditLog.setDeviceInfo(request.getHeader("User-Agent"));
                auditLog.setRequestId(request.getHeader("X-Request-ID"));
            }

            // Set user role if available
            if (currentUser.get().getRoles() != null && !currentUser.get().getRoles().isEmpty()) {
                auditLog.setRole(currentUser.get().getRoles().iterator().next().getName());
            }

            // Store old and new billing data
            auditLog.setOldValue(fieldChangeTracker.objectToJson(oldBillSnapshot));
            auditLog.setNewValue(fieldChangeTracker.objectToJson(updatedBill));

            Map<String, Object> billingFieldChanges = fieldChangeTracker.compareBillFields(oldBillSnapshot, updatedBill);
            String fieldChangedJson = fieldChangeTracker.fieldChangesToJson(billingFieldChanges);
            if (fieldChangedJson != null) {
                auditLog.setFieldChanged(fieldChangedJson);
            }

            auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
            auditLogService.persistAsync(auditLog);

            return ApiResponseHelper.successResponseWithDataAndMessage("Partial payment added successfully", HttpStatus.OK, updatedBill);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    
}
