package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.HealthPackageRequest;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.HealthPackage;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.HealthPackageRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.SequenceGeneratorService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/admin/lab")
@Tag(name = "Package", description = "Package API which is used to manage packages")
public class HealthPackageController {

    private final LabRepository labRepository;
    private final TestRepository testRepository;
    private final UserService userService;
    private final HealthPackageRepository healthPackageRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;
    private final SequenceGeneratorService sequenceGeneratorService;

    //default constructor
    public HealthPackageController(LabRepository labRepository,
                                   TestRepository testRepository,
                                   UserService userService,
                                   HealthPackageRepository healthPackageRepository,
                                   LabAccessableFilter labAccessableFilter,
                                   AuditLogService auditLogService,
                                   FieldChangeTracker fieldChangeTracker,
                                   SequenceGeneratorService sequenceGeneratorService) {
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.userService = userService;
        this.healthPackageRepository = healthPackageRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }


    //get all packages of a respective lab by their lab id  and only members of the lab can access this
    @Transactional
    @GetMapping("{labId}/packages")
    public ResponseEntity<?> getHealthPackages(
            @PathVariable("labId") Long labId) {

        // Authenticate the user
        User currentUser = getAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }


        // Fetch the lab and check if it exists
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user member of the lab or not
        if (!currentUser.getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health packages of the lab
        List<HealthPackage> healthPackages = healthPackageRepository.findAllByLabs(lab.get());

        // Return the success response with the fetched health packages
        return ApiResponseHelper.successResponse(
                "Health packages fetched successfully",
                healthPackages
        );
    }


    @Transactional
    @PostMapping("{labId}/package")
    public ResponseEntity<?> createHealthPackage(
            @PathVariable("labId") Long labId,
            @RequestBody HealthPackageRequest packageRequest, // Assuming a DTO is used to accept the data
            HttpServletRequest request) {

        // Authenticate the user
        User currentUser = getAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the lab and check if it exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        Lab lab = labOptional.get();

        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Check if the test IDs belong to the lab or not
        List<Test> tests = testRepository.findAllById(packageRequest.getTestIds());
        if (tests.size() != packageRequest.getTestIds().size()) {
            return ApiResponseHelper.errorResponse("One or more test IDs do not exist", HttpStatus.BAD_REQUEST);
        }

        // Create a new health package
        HealthPackage healthPackage = new HealthPackage();
        
        // Generate unique package code using sequence generator
        String packageCode = sequenceGeneratorService.generateCode(labId, EntityType.HEALTH_PACKAGE);
        healthPackage.setPackageCode(packageCode);
        
        healthPackage.setPackageName(packageRequest.getPackageName());
        healthPackage.setPrice(packageRequest.getPrice());
        healthPackage.setDiscount(packageRequest.getDiscount());
        healthPackage.setTests(new HashSet<>(tests)); // Convert List to Set before adding tests

        // Establish bidirectional relationship
        healthPackage.getLabs().add(lab); // Add lab to healthPackage
        lab.getHealthPackages().add(healthPackage); // Add healthPackage to lab

        // Save the health package to the database
        HealthPackage savedPackage = healthPackageRepository.save(healthPackage);
        labRepository.save(lab); // Ensure the lab entity updates the relationship

        Map<String, Object> newData = toAuditMap(savedPackage);
        logHealthPackageAudit(
                labId,
                "PACKAGE_CREATE",
                null,
                newData,
                resolveChangeReason("created", savedPackage.getPackageName()),
                currentUser,
                request,
                savedPackage.getId()
        );

        // Return the success response with the created health package
        return ApiResponseHelper.successResponse(
                "Health package created successfully",
                savedPackage
        );
    }

    //get package by id
    @Transactional
    @GetMapping("{labId}/package/{packageId}")
    public ResponseEntity<?> getHealthPackage(
            @PathVariable("labId") Long labId,
            @PathVariable("packageId") Long packageId
    ) {
        // Authenticate the user
        User currentUser = getAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the lab and check if it exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));


        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health package based on the provided package ID
        var healthPackageOptional = healthPackageRepository.findById(packageId);

        // Check if the health package exists
        if (healthPackageOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        HealthPackage healthPackage = healthPackageOptional.get();

        // Check if the health package is associated with the lab
        if (!healthPackage.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        // Return the success response with the fetched health package
        return ApiResponseHelper.successResponse(
                "Health package fetched successfully",
                healthPackage
        );
    }


    //update package
    @Transactional
    @PutMapping("{labId}/package/{packageId}")
    public ResponseEntity<?> updateHealthPackage(
            @PathVariable("labId") Long labId,
            @PathVariable("packageId") Long packageId,
            @RequestBody HealthPackageRequest packageRequest, // Assuming a DTO is used to accept the data
            HttpServletRequest request
    ) {
        // Authenticate the user
        User currentUser = getAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the lab and check if it exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));


        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health package based on the provided package ID
        var healthPackageOptional = healthPackageRepository.findById(packageId);

        // Check if the health package exists
        if (healthPackageOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        HealthPackage healthPackage = healthPackageOptional.get();

        // Check if the health package is associated with the lab
        if (!healthPackage.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        Map<String, Object> oldData = toAuditMap(healthPackage);

        // Fetch the health tests based on the provided test IDs
        List<Test> tests = testRepository.findAllById(packageRequest.getTestIds());  // Fetching tests by their IDs

        // Check if all test IDs exist in the database
        if (tests.size() != packageRequest.getTestIds().size()) {
            return ApiResponseHelper.errorResponse("One or more test IDs do not exist", HttpStatus.BAD_REQUEST);
        }

        // Clear existing associations
        for (Test existingTest : new HashSet<>(healthPackage.getTests())) {
            existingTest.getHealthPackages().remove(healthPackage);
        }

        // Update the health package
        healthPackage.setPackageName(packageRequest.getPackageName());
        healthPackage.setPrice(packageRequest.getPrice());
        healthPackage.setDiscount(packageRequest.getDiscount());
        HashSet<Test> updatedTests = new HashSet<>(tests);
        healthPackage.setTests(updatedTests);  // Convert List to Set before adding tests
        for (Test updatedTest : updatedTests) {
            updatedTest.getHealthPackages().add(healthPackage);
        }

        // Save the updated health package to the database
        HealthPackage updatedPackage = healthPackageRepository.save(healthPackage);
        labRepository.save(lab);

        Map<String, Object> newData = toAuditMap(updatedPackage);

        logHealthPackageAudit(
                labId,
                "PACKAGE_UPDATE",
                oldData,
                newData,
                resolveChangeReason("updated", updatedPackage.getPackageName()),
                currentUser,
                request,
                updatedPackage.getId()
        );

        // Return the success response with the updated health package
        return ApiResponseHelper.successResponse(
                "Health package updated successfully",
                updatedPackage
        );
    }


    //delete package by their respective id
    @Transactional
    @DeleteMapping("{labId}/package/{packageId}")
    public ResponseEntity<?> deleteHealthPackage(
            @PathVariable("labId") Long labId,
            @PathVariable("packageId") Long packageId,
            HttpServletRequest request
    ) {
        // Authenticate the user
        User currentUser = getAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the lab and check if it exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health package based on the provided package ID
        var healthPackageOptional = healthPackageRepository.findById(packageId);

        // Check if the health package exists
        if (healthPackageOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        HealthPackage healthPackage = healthPackageOptional.get();

        // Check if the health package is associated with the lab
        if (!healthPackage.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("Health package not associated with this lab", HttpStatus.NOT_FOUND);
        }

        Map<String, Object> oldData = toAuditMap(healthPackage);
        long packageIdValue = healthPackage.getId();

        // Remove the association of the health package with the lab
        lab.getHealthPackages().remove(healthPackage);

        // Remove package from tests association
        for (Test test : new HashSet<>(healthPackage.getTests())) {
            test.getHealthPackages().remove(healthPackage);
        }
        healthPackage.getTests().clear();

        // Save the updated lab to ensure the association is removed
        labRepository.save(lab);

        // Delete the health package from the database
        healthPackageRepository.delete(healthPackage);

        logHealthPackageAudit(
                labId,
                "PACKAGE_DELETE",
                oldData,
                null,
                resolveChangeReason("deleted", (String) oldData.get("packageName")),
                currentUser,
                request,
                packageIdValue
        );

        // Return the success response
        return ApiResponseHelper.successResponse(
                "Health package deleted successfully",
                null
        );
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

    private void logHealthPackageAudit(Long labId,
                                       String action,
                                       Map<String, Object> oldData,
                                       Map<String, Object> newData,
                                       String changeReason,
                                       User currentUser,
                                       HttpServletRequest request,
                                       Long entityId) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setModule("HealthPackage");
        auditLog.setEntityType("HealthPackage");
        auditLog.setLab_id(String.valueOf(labId));
        auditLog.setActionType(action);
        auditLog.setChangeReason(changeReason != null ? changeReason : "");

        if (entityId != null) {
            auditLog.setEntityId(String.valueOf(entityId));
        }

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

        auditLog.setOldValue(fieldChangeTracker.objectToJson(oldData));
        auditLog.setNewValue(fieldChangeTracker.objectToJson(newData));

        if (oldData != null || newData != null) {
            Map<String, Object> fieldChanges = fieldChangeTracker.compareMaps(oldData, newData);
            String fieldChangedJson = fieldChangeTracker.fieldChangesToJson(fieldChanges);
            if (fieldChangedJson != null) {
                auditLog.setFieldChanged(fieldChangedJson);
            }
        }

        auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
        auditLogService.persistAsync(auditLog);
    }

    private Map<String, Object> toAuditMap(HealthPackage healthPackage) {
        if (healthPackage == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", healthPackage.getId());
        data.put("packageName", healthPackage.getPackageName());
        data.put("price", healthPackage.getPrice());
        data.put("discount", healthPackage.getDiscount());
        Set<Long> testIds = healthPackage.getTests().stream()
                .map(Test::getId)
                .collect(Collectors.toCollection(TreeSet::new));
        data.put("testIds", testIds);
        return data;
    }

    private String resolveChangeReason(String action, String packageName) {
        String name = packageName != null ? packageName : "package";
        return switch (action) {
            case "created" -> "Created package " + name;
            case "updated" -> "Updated package " + name;
            case "deleted" -> "Deleted package " + name;
            default -> name;
        };
    }
}
