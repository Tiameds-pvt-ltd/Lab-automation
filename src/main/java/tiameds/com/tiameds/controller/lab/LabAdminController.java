package tiameds.com.tiameds.controller.lab;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.auth.MemberDetailsUpdate;
import tiameds.com.tiameds.dto.auth.MemberRegisterDto;
import tiameds.com.tiameds.dto.lab.UserInLabDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.ModuleEntity;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.ModuleRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.auth.MemberUserServices;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.UserLabService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;
import java.util.*;
import java.util.stream.Collectors;

///create-user/


@RestController
@RequestMapping("/user-management")
public class LabAdminController {
    private final UserRepository userRepository;
    private UserLabService userLabService;
    private UserAuthService userAuthService;
    private LabRepository labRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private ModuleRepository moduleRepository;
    private LabAccessableFilter labAccessableFilter;
    private MemberUserServices memberUserServices;
    private AuditLogService auditLogService;
    private FieldChangeTracker fieldChangeTracker;

    @Autowired
    public LabAdminController(
            UserLabService userLabService,
            UserAuthService userAuthService,
            LabRepository labRepository, UserService userService,
            PasswordEncoder passwordEncoder,
            ModuleRepository moduleRepository,
            LabAccessableFilter labAccessableFilter,
            UserRepository userRepository, MemberUserServices memberUserServices,
            AuditLogService auditLogService,
            FieldChangeTracker fieldChangeTracker) {
        this.userLabService = userLabService;
        this.userAuthService = userAuthService;
        this.labRepository = labRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.moduleRepository = moduleRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.userRepository = userRepository;
        this.memberUserServices = memberUserServices;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
    }

    public LabAdminController(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder, MemberUserServices memberUserServices) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.memberUserServices = memberUserServices;
    }

    @Transactional
    @GetMapping("/get-members/{labId}")
    public ResponseEntity<?> getLabMembers(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token) {
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
        }
        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }
//        if (!lab.getCreatedBy().equals(currentUser)) {
//            return ApiResponseHelper.errorResponse("You are not authorized to view members of this lab", HttpStatus.UNAUTHORIZED);
//        }

        // check if the user is correct role to view members SUPERADMIN AND ADMIN

        List<UserInLabDTO> memberDTOs = MemberUserServices.getMembersInLab(lab);
        return ApiResponseHelper.successResponse("Lab members retrieved successfully", memberDTOs);
    }


    @Transactional
    @PostMapping("/create-user/{labId}")
    public ResponseEntity<?> createUserInLab(
            @RequestBody MemberRegisterDto registerRequest,
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null)
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);

        // Check creator of the lab
//        if (!lab.getCreatedBy().equals(currentUser)) {
//            return ApiResponseHelper.errorResponse("You are not authorized to create members in this lab", HttpStatus.UNAUTHORIZED);
//        }
        //check the user is already a member of the lab using username and email
        if (lab.getMembers().stream().anyMatch(user -> user.getUsername().equals(registerRequest.getUsername()) || user.getEmail().equals(registerRequest.getEmail()))) {
            return ApiResponseHelper.errorResponse("User is already a member of this lab", HttpStatus.CONFLICT);
        }

        // deligate to memberUserServices to create user and add to lab send currentuser and registerRequest
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ApiResponseHelper.errorResponse("Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ApiResponseHelper.errorResponse("Email already exists", HttpStatus.CONFLICT);
        }

        // role-based creation rules (hierarchical)
        // Determine creator's roles
        java.util.Set<String> creatorRoles = currentUser.getRoles().stream()
                .map(role -> role.getName())
                .filter(java.util.Objects::nonNull)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());

        boolean isSuperAdminCreator = creatorRoles.contains("SUPERADMIN");
        boolean isAdminCreator = creatorRoles.contains("ADMIN");
        boolean isTechOrDeskCreator = creatorRoles.contains("TECHNICIAN") || creatorRoles.contains("DESKROLE");

        // TECHNICIAN and DESKROLE cannot create any user
        if (isTechOrDeskCreator && !isSuperAdminCreator && !isAdminCreator) {
            return ApiResponseHelper.errorResponse("You are not permitted to create users", HttpStatus.FORBIDDEN);
        }

        // Determine allowed roles based on creator's highest privilege
        java.util.Set<String> allowedRoles = isSuperAdminCreator
                ? java.util.Set.of("SUPERADMIN", "ADMIN", "TECHNICIAN", "DESKROLE")
                : (isAdminCreator ? java.util.Set.of("ADMIN", "TECHNICIAN", "DESKROLE") : java.util.Set.of());

        if (allowedRoles.isEmpty()) {
            return ApiResponseHelper.errorResponse("Only ADMIN or SUPERADMIN can create users", HttpStatus.FORBIDDEN);
        }

        boolean invalidRequestedRole = registerRequest.getRoles() != null && registerRequest.getRoles().stream()
                .filter(java.util.Objects::nonNull)
                .map(String::toUpperCase)
                .anyMatch(r -> !allowedRoles.contains(r));
        if (invalidRequestedRole) {
            return ApiResponseHelper.errorResponse(
                    isSuperAdminCreator
                            ? "Invalid role selection. Allowed: SUPERADMIN, ADMIN, TECHNICIAN, DESKROLE"
                            : "Invalid role selection. Allowed: ADMIN, TECHNICIAN, DESKROLE",
                    HttpStatus.FORBIDDEN);
        }

        // Create a new user and add to the lab
        memberUserServices.createUserAndAddToLab(registerRequest, lab, currentUser);

        // Fetch the created user for audit logging
        User createdUser = userRepository.findByUsername(registerRequest.getUsername()).orElse(null);
        if (createdUser != null) {
            Map<String, Object> newUserData = toUserAuditMap(createdUser);
            String changeReason = String.format("User created in lab %d by %s", labId, currentUser.getUsername());
            logLabAdminAudit(
                    request,
                    currentUser,
                    labId,
                    createdUser.getId(),
                    "CREATE",
                    null,
                    newUserData,
                    changeReason
            );
        }

        return ApiResponseHelper.successResponse("User created and added to lab successfully", HttpStatus.OK);
    }


     // update user in lab
    @Transactional
    @PutMapping("/update-user/{labId}/{userId}")
    public ResponseEntity<?> updateUserInLab(
            @PathVariable Long labId,
            @PathVariable Long userId,
            @RequestBody MemberDetailsUpdate registerRequest,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Check creator of the lab
//        if (!lab.getCreatedBy().equals(currentUser)) {
//            return ApiResponseHelper.errorResponse("You are not authorized to update members in this lab", HttpStatus.UNAUTHORIZED);
//        }

        User userToUpdate = userRepository.findById(userId).orElse(null);
        if (userToUpdate == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);
        }

        // Capture old state before update
        Map<String, Object> oldUserData = toUserAuditMap(userToUpdate);

        // delegate to memberUserServices to update user and add to lab
        memberUserServices.updateUserInLab(registerRequest, userToUpdate, lab, currentUser);

        // Fetch updated user from database to get updated state
        User updatedUser = userRepository.findById(userId).orElse(null);
        if (updatedUser != null) {
            Map<String, Object> newUserData = toUserAuditMap(updatedUser);
            String changeReason = String.format("User updated in lab %d by %s", labId, currentUser.getUsername());
            logLabAdminAudit(
                    request,
                    currentUser,
                    labId,
                    userId,
                    "UPDATE",
                    oldUserData,
                    newUserData,
                    changeReason
            );
        }

        return ApiResponseHelper.successResponse("User updated successfully", HttpStatus.OK);

    }

    @Transactional
    @PutMapping("/reset-password/{labId}/{userId}")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long labId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordRequest, // Change to accept JSON object
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null) {
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Check creator of the lab
        if (!lab.getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to reset password for members in this lab", HttpStatus.UNAUTHORIZED);
        }

        User userToUpdate = userRepository.findById(userId).orElse(null);
        if (userToUpdate == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);
        }

        String newPassword = passwordRequest.get("newPassword");
        String confirmPassword = passwordRequest.get("confirmPassword");

        // Validate passwords match
        if (newPassword == null || confirmPassword == null || !newPassword.equals(confirmPassword)) {
            return ApiResponseHelper.errorResponse("Passwords don't match", HttpStatus.BAD_REQUEST);
        }

        // Validate password length
        if (newPassword.length() < 8) {
            return ApiResponseHelper.errorResponse("Password must be at least 8 characters", HttpStatus.BAD_REQUEST);
        }

        // Capture old state before password reset
        Map<String, Object> oldUserData = toUserAuditMap(userToUpdate);

        // Reset password
        userToUpdate.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userToUpdate);

        // Fetch updated user from database to get updated state
        User updatedUser = userRepository.findById(userId).orElse(null);
        if (updatedUser != null) {
            Map<String, Object> newUserData = toUserAuditMap(updatedUser);
            String changeReason = String.format("Password reset for user in lab %d by %s", labId, currentUser.getUsername());
            logLabAdminAudit(
                    request,
                    currentUser,
                    labId,
                    userId,
                    "UPDATE",
                    oldUserData,
                    newUserData,
                    changeReason
            );
        }

        return ApiResponseHelper.successResponse("Password reset successfully", HttpStatus.OK);
    }

    private void logLabAdminAudit(
            HttpServletRequest request,
            User currentUser,
            Long labId,
            Long entityId,
            String action,
            Map<String, Object> oldData,
            Map<String, Object> newData,
            String changeReason) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setModule("LabAdmin");
        auditLog.setEntityType("User");
        auditLog.setLab_id(labId != null ? String.valueOf(labId) : "GLOBAL");
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

    private Map<String, Object> toUserAuditMap(User user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("phone", user.getPhone());
        data.put("address", user.getAddress());
        data.put("city", user.getCity());
        data.put("state", user.getState());
        data.put("zip", user.getZip());
        data.put("country", user.getCountry());
        data.put("enabled", user.isEnabled());
        data.put("isVerified", user.isVerified());
        data.put("tokenVersion", user.getTokenVersion());
        data.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        data.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        
        // Include roles
        if (user.getRoles() != null) {
            List<String> roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());
            data.put("roles", roleNames);
        } else {
            data.put("roles", new ArrayList<>());
        }
        
        // Include modules
        if (user.getModules() != null) {
            List<String> moduleNames = user.getModules().stream()
                    .map(ModuleEntity::getName)
                    .collect(Collectors.toList());
            data.put("modules", moduleNames);
        } else {
            data.put("modules", new ArrayList<>());
        }
        
        // Include lab IDs
        if (user.getLabs() != null) {
            List<Long> labIds = user.getLabs().stream()
                    .map(Lab::getId)
                    .collect(Collectors.toList());
            data.put("labIds", labIds);
        } else {
            data.put("labIds", new ArrayList<>());
        }
        
        // Include created by
        if (user.getCreatedBy() != null) {
            data.put("createdById", user.getCreatedBy().getId());
            data.put("createdByUsername", user.getCreatedBy().getUsername());
        }
        
        return data;
    }


}

