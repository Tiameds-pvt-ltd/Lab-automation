package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.AiClinicalObservationRequest;
import tiameds.com.tiameds.entity.AiClinicalObservation;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.AiClinicalObservationRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/lab")
@Tag(name = "AI Clinical Observation", description = "Manage AI clinical observations for patient visits")
public class AiClinicalObservationController {

    private final AiClinicalObservationRepository observationRepository;
    private final VisitRepository visitRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final UserService userService;

    public AiClinicalObservationController(AiClinicalObservationRepository observationRepository,
                                           VisitRepository visitRepository,
                                           LabAccessableFilter labAccessableFilter,
                                           UserService userService) {
        this.observationRepository = observationRepository;
        this.visitRepository = visitRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.userService = userService;
    }

    @Transactional
    @PostMapping("/{labId}/visit/{visitId}/ai-clinical-observation")
    public ResponseEntity<?> saveObservation(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestBody AiClinicalObservationRequest request) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
            if (visitOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
            }

            VisitEntity visit = visitOptional.get();
            boolean visitInLab = visit.getLabs().stream().anyMatch(l -> l.getId() == labId);
            if (!visitInLab) {
                return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
            }

            AiClinicalObservation observation = observationRepository
                    .findTopByVisit_VisitIdOrderByCreatedAtDesc(visitId)
                    .orElseGet(AiClinicalObservation::new);

            observation.setVisit(visit);
            observation.setProvisionalDiagnosis(request.getProvisionalDiagnosis());
            observation.setClinicalInterpretation(request.getClinicalInterpretation());
            observation.setDoctorToVisit(request.getDoctorToVisit());
            observation.setPatientInterpretation(request.getPatientInterpretation());
            observation.setTips(request.getTips());
            observation.setContentHash(request.getContentHash());

            if (observation.getId() == null) {
                observation.setCreatedBy(currentUser.get().getUsername());
            } else {
                observation.setUpdatedBy(currentUser.get().getUsername());
            }

            observationRepository.save(observation);

            return ApiResponseHelper.successResponse("AI clinical observation saved successfully", buildResponse(observation));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Transactional for the same reason the save is: spring.jpa.open-in-view is false, so
    // the Hibernate session closes as soon as the repository call returns. Both the lab
    // check below (VisitEntity.labs is a LAZY @ManyToMany) and buildResponse's
    // obs.getVisit().getVisitId() touch lazy state, and without a transaction around them
    // they throw LazyInitializationException -- which the blanket catch here turns into a
    // 500. The client reads that failure as "never generated" and re-runs the model on
    // every single open, so the whole point of storing observations is lost silently.
    @Transactional
    @GetMapping("/{labId}/visit/{visitId}/ai-clinical-observation")
    public ResponseEntity<?> getObservations(
            @PathVariable Long labId,
            @PathVariable Long visitId) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
            if (visitOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
            }

            boolean visitInLab = visitOptional.get().getLabs().stream().anyMatch(l -> l.getId() == labId);
            if (!visitInLab) {
                return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
            }

            return observationRepository.findTopByVisit_VisitIdOrderByCreatedAtDesc(visitId)
                    .map(obs -> ApiResponseHelper.successResponse(
                            "AI clinical observation retrieved successfully", buildResponse(obs)))
                    .orElseGet(() -> ApiResponseHelper.successResponse(
                            "No AI clinical observation found", null));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> buildResponse(AiClinicalObservation obs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", obs.getId());
        map.put("visitId", obs.getVisit().getVisitId());
        map.put("provisionalDiagnosis", obs.getProvisionalDiagnosis());
        map.put("clinicalInterpretation", obs.getClinicalInterpretation());
        map.put("doctorToVisit", obs.getDoctorToVisit());
        map.put("patientInterpretation", obs.getPatientInterpretation());
        map.put("tips", obs.getTips());
        map.put("contentHash", obs.getContentHash());
        map.put("createdBy", obs.getCreatedBy());
        map.put("createdAt", obs.getCreatedAt());
        map.put("updatedAt", obs.getUpdatedAt());
        return map;
    }

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
