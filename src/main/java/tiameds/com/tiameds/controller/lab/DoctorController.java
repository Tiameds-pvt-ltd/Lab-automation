package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.DoctorDTO;
import tiameds.com.tiameds.entity.Doctors;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.DoctorRepository;
import tiameds.com.tiameds.services.lab.DoctorService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Transactional
@RestController
@RequestMapping("/admin/lab")
@Tag(name = "Doctor Controller", description = "manage the doctors in the lab")
public class DoctorController {

    private final DoctorService doctorService;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;
    private final DoctorRepository doctorRepository;

    public DoctorController(DoctorService doctorService,
                            UserAuthService userAuthService,
                            LabAccessableFilter labAccessableFilter,
                            AuditLogService auditLogService,
                            FieldChangeTracker fieldChangeTracker,
                            DoctorRepository doctorRepository) {
        this.doctorService = doctorService;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
        this.doctorRepository = doctorRepository;
    }

    // create doctor or add doctor to lab
    @PostMapping("{labId}/doctors")
    public ResponseEntity<?> addDoctorToLab(
            @PathVariable("labId") Long labId,
            @RequestBody DoctorDTO doctorDTO,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            // Delegate to the service layer
            DoctorDTO createdDoctor = doctorService.addDoctorToLab(labId, doctorDTO,currentUser.get().getUsername());

            logDoctorAudit(labId,
                    "CREATE",
                    null,
                    createdDoctor,
                    resolveChangeReason(doctorDTO),
                    currentUser.get(),
                    request);

            return ApiResponseHelper.successResponse("Doctor added successfully", createdDoctor);
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
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            DoctorDTO oldDoctorSnapshot = getDoctorSnapshot(labId, doctorId);
            // Delegate to the service layer
            DoctorDTO updatedDoctor = doctorService.updateDoctor(labId, doctorId, doctorDTO,currentUser.get().getUsername());

            logDoctorAudit(labId,
                    "UPDATE",
                    oldDoctorSnapshot,
                    updatedDoctor,
                    resolveChangeReason(doctorDTO),
                    currentUser.get(),
                    request);

            return ApiResponseHelper.successResponse("Doctor updated successfully", updatedDoctor);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // delete doctor
    @DeleteMapping("{labId}/doctors/{doctorId}")
    public ResponseEntity<?> deleteDoctor(
            @PathVariable("labId") Long labId,
            @PathVariable("doctorId") Long doctorId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            DoctorDTO oldDoctorSnapshot = getDoctorSnapshot(labId, doctorId);
            DoctorDTO deletedDoctor = doctorService.deleteDoctor(labId, doctorId);

            logDoctorAudit(labId,
                    "DELETE",
                    oldDoctorSnapshot != null ? oldDoctorSnapshot : deletedDoctor,
                    null,
                    "",
                    currentUser.get(),
                    request);

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
            return ApiResponseHelper.successResponse("Doctors retrieved successfully", doctorService.getAllDoctors(labId));
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("{labId}/doctors/{doctorId}")
    public ResponseEntity<?> getDoctorById(
            @PathVariable("labId") Long labId,
            @PathVariable("doctorId") Long doctorId,
            @RequestHeader("Authorization") String token) {
        try {
            if (userAuthService.authenticateUser(token).isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            return ApiResponseHelper.successResponse("Doctor retrieved successfully", doctorService.getDoctorById(labId, doctorId));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void logDoctorAudit(Long labId,
                                String action,
                                DoctorDTO oldDoctor,
                                DoctorDTO newDoctor,
                                String changeReason,
                                User currentUser,
                                HttpServletRequest request) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setModule("Doctor");
        auditLog.setEntityType("Doctor");
        if (newDoctor != null && newDoctor.getId() != 0) {
            auditLog.setEntityId(String.valueOf(newDoctor.getId()));
        } else if (oldDoctor != null && oldDoctor.getId() != 0) {
            auditLog.setEntityId(String.valueOf(oldDoctor.getId()));
        }
        auditLog.setLab_id(String.valueOf(labId));
        auditLog.setActionType(action);
        auditLog.setChangeReason(changeReason != null ? changeReason : "");

        if (currentUser != null) {
            auditLog.setUsername(currentUser.getUsername());
            auditLog.setUserId(currentUser.getId());
            if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
                auditLog.setRole(currentUser.getRoles().iterator().next().getName());
            }
        } else {
            auditLog.setUsername("system");
        }

        if (request != null) {
            String ipAddress = request.getHeader("X-Forwarded-For");
            auditLog.setIpAddress(ipAddress != null ? ipAddress : request.getRemoteAddr());
            auditLog.setDeviceInfo(request.getHeader("User-Agent"));
            auditLog.setRequestId(request.getHeader("X-Request-ID"));
        }

        auditLog.setOldValue(fieldChangeTracker.objectToJson(oldDoctor));
        auditLog.setNewValue(fieldChangeTracker.objectToJson(newDoctor));

        Map<String, Object> fieldChanges = fieldChangeTracker.compareDoctorFields(oldDoctor, newDoctor);
        String fieldChangedJson = fieldChangeTracker.fieldChangesToJson(fieldChanges);
        if (fieldChangedJson != null) {
            auditLog.setFieldChanged(fieldChangedJson);
        }

        auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
        auditLogService.persistAsync(auditLog);
    }

    private DoctorDTO getDoctorSnapshot(Long labId, Long doctorId) {
        return doctorRepository.findById(doctorId)
                .filter(doctor -> doctor.getLabs() != null && doctor.getLabs().stream()
                        .anyMatch(lab -> Objects.equals(lab.getId(), labId)))
                .map(this::toDoctorDTO)
                .orElse(null);
    }

    private DoctorDTO toDoctorDTO(Doctors doctor) {
        DoctorDTO dto = new DoctorDTO();
        dto.setId(doctor.getId());
        dto.setName(doctor.getName());
        dto.setEmail(doctor.getEmail());
        dto.setSpeciality(doctor.getSpeciality());
        dto.setQualification(doctor.getQualification());
        dto.setHospitalAffiliation(doctor.getHospitalAffiliation());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setPhone(doctor.getPhone());
        dto.setAddress(doctor.getAddress());
        dto.setCity(doctor.getCity());
        dto.setState(doctor.getState());
        dto.setCountry(doctor.getCountry());
        dto.setCreatedBy(doctor.getCreatedBy());
        dto.setUpdatedBy(doctor.getUpdatedBy());
        dto.setCreatedAt(doctor.getCreatedAt());
        dto.setUpdatedAt(doctor.getUpdatedAt());
        return dto;
    }

    private String resolveChangeReason(DoctorDTO doctorDTO) {
        if (doctorDTO == null) {
            return "";
        }
        if (doctorDTO.getUpdatedBy() != null && !doctorDTO.getUpdatedBy().isBlank()) {
            return doctorDTO.getUpdatedBy();
        }
        if (doctorDTO.getCreatedBy() != null && !doctorDTO.getCreatedBy().isBlank()) {
            return doctorDTO.getCreatedBy();
        }
        return "";
    }
}
