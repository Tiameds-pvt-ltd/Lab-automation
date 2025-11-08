package tiameds.com.tiameds.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import tiameds.com.tiameds.audit.helpers.AuditDataExtractor;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.entity.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final PatientRepository patientRepository;
    private final AuditDataExtractor auditDataExtractor;
    private final FieldChangeTracker fieldChangeTracker;

    public AuditAspect(AuditLogService auditLogService, 
                      PatientRepository patientRepository,
                      AuditDataExtractor auditDataExtractor,
                      FieldChangeTracker fieldChangeTracker) {
        this.auditLogService = auditLogService;
        this.patientRepository = patientRepository;
        this.auditDataExtractor = auditDataExtractor;
        this.fieldChangeTracker = fieldChangeTracker;
    }

    @Pointcut(value = "@annotation(auditable)", argNames = "auditable")
    public void auditableMethod(Auditable auditable) {}

    @Around(value = "auditableMethod(auditable)", argNames = "pjp,auditable")
    public Object aroundAuditable(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object result;
        HttpServletRequest request = currentHttpRequest();
        String httpMethod = request != null ? request.getMethod() : null;
        
        // Extract PatientDTO from method arguments for CREATE/UPDATE operations
        PatientDTO requestPatientDTO = auditDataExtractor.extractPatientDTOFromArgs(pjp.getArgs());
        PatientEntity oldPatientEntity = null;
        PatientDTO oldPatientSnapshot = null;
        Long patientIdFromPath = null;
        
        // Extract patientId from path variables for UPDATE operations
        if (request != null) {
            Object varsObj = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (varsObj instanceof Map<?, ?> vars) {
                String patientIdStr = asString(vars.get("patientId"));
                if (!patientIdStr.isEmpty()) {
                    try {
                        patientIdFromPath = Long.parseLong(patientIdStr);
                    } catch (NumberFormatException e) {
                        log.debug("Could not parse patientId from path: {}", patientIdStr);
                    }
                }
            }
        }
        
        // For UPDATE operations, get old patient data before the update
        Long patientIdToFetch = null;
        if (requestPatientDTO != null && requestPatientDTO.getId() != null) {
            patientIdToFetch = requestPatientDTO.getId();
        } else if (patientIdFromPath != null) {
            patientIdToFetch = patientIdFromPath;
        }
        
        if (("PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) && patientIdToFetch != null) {
            Optional<PatientEntity> oldPatientOpt = patientRepository.findById(patientIdToFetch);
            if (oldPatientOpt.isPresent()) {
                oldPatientEntity = oldPatientOpt.get();
                oldPatientSnapshot = new PatientDTO(oldPatientEntity);
            }
        }

        // Let the business logic run first
        result = pjp.proceed();

        LabAuditLogs auditLog = new LabAuditLogs();
        // Set timestamp
        auditLog.setTimestamp(LocalDateTime.now());
        
        // User and role
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MyUserDetails principal) {
            User user = principal.getUser();
            if (user != null) {
                auditLog.setUsername(user.getUsername());
                auditLog.setUserId(user.getId());
            } else {
                auditLog.setUsername(authentication.getName());
                auditLog.setUserId(null);
            }
            String role = authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()
                    ? authentication.getAuthorities().iterator().next().getAuthority()
                    : null;
            auditLog.setRole(role);
        } else if (authentication != null) {
            auditLog.setUsername(authentication.getName());
            auditLog.setUserId(null);
        } else {
            auditLog.setUsername("anonymous");
            auditLog.setUserId(null);
        }

        // Request metadata
        if (request != null) {
            auditLog.setIpAddress(Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElseGet(request::getRemoteAddr));
            auditLog.setDeviceInfo(request.getHeader("User-Agent"));
            auditLog.setRequestId(Optional.ofNullable(request.getHeader("X-Request-ID")).orElse(null));
        }

        // Module & action
        auditLog.setModule(auditable.module());
        String action = auditable.action();
        if (action == null || action.isBlank()) {
            action = deriveActionFromHttpMethod(httpMethod);
        }
        auditLog.setActionType(action);

        // Entity identification from common path variables
        if (request != null) {
            Object varsObj = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (varsObj instanceof Map<?, ?> vars) {
                String labId = asString(vars.get("labId"));
                String patientId = asString(vars.get("patientId"));
                String visitId = asString(vars.get("visitId"));
                String billingId = asString(vars.get("billingId"));
                String testId = asString(vars.get("testId"));

                // Set lab_id (required field)
                if (!labId.isEmpty()) {
                    auditLog.setLab_id(labId);
                }

                if (!patientId.isEmpty()) {
                    auditLog.setEntityType("Patient");
                    auditLog.setEntityId(patientId);
                } else if (!visitId.isEmpty()) {
                    auditLog.setEntityType("Visit");
                    auditLog.setEntityId(visitId);
                } else if (!billingId.isEmpty()) {
                    auditLog.setEntityType("Billing");
                    auditLog.setEntityId(billingId);
                } else if (!testId.isEmpty()) {
                    auditLog.setEntityType("Test");
                    auditLog.setEntityId(testId);
                }
            }

            // Fallback: infer type from URL if still empty
            if ((auditLog.getEntityType() == null || auditLog.getEntityType().isEmpty())) {
                String uri = request.getRequestURI();
                if (uri != null) {
                    if (uri.contains("/patient")) {
                        auditLog.setEntityType("Patient");
                    } else if (uri.contains("/visit")) {
                        auditLog.setEntityType("Visit");
                    } else if (uri.contains("/billing")) {
                        auditLog.setEntityType("Billing");
                    }
                }
            }
        }

        // Handle patient data for CREATE and UPDATE operations
        // Check if this is a patient operation (either from entityType or from request having PatientDTO)
        boolean isPatientOperation = "Patient".equals(auditLog.getEntityType()) || requestPatientDTO != null;
        
        if (isPatientOperation) {
            try {
                PatientDTO responsePatientDTO = auditDataExtractor.extractPatientDTOFromResponse(result);
                
                // Ensure entityType is set if we have patient data
                if ((requestPatientDTO != null || responsePatientDTO != null) && 
                    (auditLog.getEntityType() == null || auditLog.getEntityType().isEmpty())) {
                    auditLog.setEntityType("Patient");
                }
                
                if ("CREATE".equals(auditLog.getActionType())) {
                    // For CREATE: Store new patient data in newValue
                    PatientDTO patientDataToStore = responsePatientDTO != null ? responsePatientDTO : requestPatientDTO;
                    if (patientDataToStore != null) {
                        String newValueJson = fieldChangeTracker.objectToJson(patientDataToStore);
                        auditLog.setNewValue(newValueJson);
                        // Set entityId from the created patient's ID
                        if (patientDataToStore.getId() != null) {
                            auditLog.setEntityId(String.valueOf(patientDataToStore.getId()));
                        }
                        log.debug("Patient CREATE audit: stored patient data with ID {}", patientDataToStore.getId());
                    } else {
                        log.warn("Patient CREATE audit: no patient data found in request or response");
                    }
                } else if (("UPDATE".equals(auditLog.getActionType()) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) 
                           && (requestPatientDTO != null || responsePatientDTO != null || oldPatientSnapshot != null)) {
                    // For UPDATE: Store old data, new data, and field changes
                    PatientDTO newPatientDTO = responsePatientDTO != null ? responsePatientDTO : requestPatientDTO;

                    // If response didn't include updated data, load it from the repository
                    if (newPatientDTO == null && patientIdToFetch != null) {
                        Optional<PatientEntity> updatedPatientOpt = patientRepository.findById(patientIdToFetch);
                        if (updatedPatientOpt.isPresent()) {
                            newPatientDTO = new PatientDTO(updatedPatientOpt.get());
                        } else {
                            log.warn("Patient UPDATE audit: could not load updated patient with id {}", patientIdToFetch);
                        }
                    }

                    if (oldPatientSnapshot != null) {
                        String oldValueJson = fieldChangeTracker.objectToJson(oldPatientSnapshot);
                        auditLog.setOldValue(oldValueJson);
                    }

                    if (newPatientDTO != null) {
                        String newValueJson = fieldChangeTracker.objectToJson(newPatientDTO);
                        auditLog.setNewValue(newValueJson);
                    }

                    // Track field changes using helper
                    if (oldPatientSnapshot != null && newPatientDTO != null) {
                        Map<String, Object> fieldChanges = fieldChangeTracker.comparePatientFields(oldPatientSnapshot, newPatientDTO);
                        String fieldChangedJson = fieldChangeTracker.fieldChangesToJson(fieldChanges);
                        if (fieldChangedJson != null) {
                            auditLog.setFieldChanged(fieldChangedJson);
                        }
                    } else {
                        log.debug("Patient UPDATE audit: insufficient data to compute field changes (old={}, new={})",
                                oldPatientSnapshot != null, newPatientDTO != null);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to process patient audit data: {}", e.getMessage(), e);
            }
        }

        // Enrichment placeholders (entity info can be set by targeted manual calls if needed)
        auditLog.setSeverity(LabAuditLogs.Severity.LOW);

        auditLogService.persistAsync(auditLog);
        log.debug("Audit event queued: module={}, action={}, user={}", auditLog.getModule(), auditLog.getActionType(), auditLog.getUsername());
        return result;
    }

    private String deriveActionFromHttpMethod(String httpMethod) {
        if (httpMethod == null) return "VIEW";
        return switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "VIEW";
        };
    }

    private HttpServletRequest currentHttpRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}


