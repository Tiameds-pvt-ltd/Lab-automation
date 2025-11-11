package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.auth.RegisterRequest;
import tiameds.com.tiameds.dto.lab.LabDetailsDetails;
import tiameds.com.tiameds.dto.lab.LabListDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.ModuleEntity;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.ModuleRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.UserLabService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lab/admin")
@Tag(name = "Lab Member Controller", description = "create, get, update and delete lab members")
public class LabMemberController {

    private final UserRepository userRepository;
    private UserLabService userLabService;
    private UserAuthService userAuthService;
    private LabRepository labRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private ModuleRepository moduleRepository;
    private LabAccessableFilter labAccessableFilter;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;

    @Autowired
    public LabMemberController(
            UserLabService userLabService,
            UserAuthService userAuthService,
            LabRepository labRepository, UserService userService,
            PasswordEncoder passwordEncoder,
            ModuleRepository moduleRepository,
            LabAccessableFilter labAccessableFilter,
            UserRepository userRepository,
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
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
    }

    @PostMapping("/add-member/{labId}/member/{userId}")
    public ResponseEntity<?> addMemberToLab(
            @PathVariable Long labId,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Check if the user is authenticated
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        // Check if the lab exists
        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null)
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);


        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Check if the user exists (assuming you have a UserRepository or similar)
        User userToAdd = userLabService.getUserById(userId);
        if (userToAdd == null)
            return ApiResponseHelper.errorResponse("User to be added not found", HttpStatus.NOT_FOUND);

        //check createor of the lab
        if (!lab.getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to get members of this lab", HttpStatus.UNAUTHORIZED);
        }
        
        // Capture old state before modification
        Map<String, Object> oldData = toLabAuditMap(lab);

        // Add the user to the lab's members
        if (lab.getMembers().contains(userToAdd)) {
            return ApiResponseHelper.errorResponse("User is already a member of this lab", HttpStatus.CONFLICT);
        }
        lab.getMembers().add(userToAdd);
        labRepository.save(lab);

        // Capture new state after modification
        Map<String, Object> newData = toLabAuditMap(lab);

        logLabMemberAudit(
                labId,
                "MEMBER_ADD",
                oldData,
                newData,
                "Added user " + userToAdd.getUsername() + " (ID: " + userId + ") to lab " + labId,
                currentUser,
                request,
                String.valueOf(userId)
        );

        return ApiResponseHelper.successResponse("User added to lab successfully", HttpStatus.OK);
    }

//    @Transactional
//    @GetMapping("/get-members/{labId}")
//    public ResponseEntity<?> getLabMembers(
//            @PathVariable Long labId,
//            @RequestHeader("Authorization") String token) {
//        User currentUser = userAuthService.authenticateUser(token).orElse(null);
//        if (currentUser == null) {
//            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
//        }
//        Lab lab = labRepository.findById(labId).orElse(null);
//        if (lab == null) {
//            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//        }
//        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
//        if (isAccessible == false) {
//            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
//        }
//        if (!lab.getCreatedBy().equals(currentUser)) {
//            return ApiResponseHelper.errorResponse("You are not authorized to view members of this lab", HttpStatus.UNAUTHORIZED);
//        }
//        List<UserInLabDTO> memberDTOs = lab.getMembers().stream()
//                .map(user -> new UserInLabDTO(
//                        user.getId(),
//                        user.getUsername(),
//                        user.getEmail(),
//                        user.getFirstName(),
//                        user.getLastName(),
//                        user.isEnabled(),
//                        user.getPhone(),
//                        user.getCity(),
//                        user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()
//                        ))).collect(Collectors.toList());
//        return ApiResponseHelper.successResponse("Lab members retrieved successfully", memberDTOs);
//    }

    //---------------remove a member from a lab----------------------------\\
    @DeleteMapping("/remove-member/{labId}/member/{userId}")
    public ResponseEntity<?> removeMemberFromLab(
            @PathVariable Long labId,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null)
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);


        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        User userToRemove = userLabService.getUserById(userId);
        if (userToRemove == null)
            return ApiResponseHelper.errorResponse("User to be removed not found", HttpStatus.NOT_FOUND);


        //check createor of the lab
        if (!lab.getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to remove members from this lab", HttpStatus.UNAUTHORIZED);
        }

        if (!lab.getMembers().contains(userToRemove)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.NOT_FOUND);
        }

        // Capture old state before modification
        Map<String, Object> oldData = toLabAuditMap(lab);

        lab.getMembers().remove(userToRemove);
        labRepository.save(lab);

        // Capture new state after modification
        Map<String, Object> newData = toLabAuditMap(lab);

        logLabMemberAudit(
                labId,
                "MEMBER_REMOVE",
                oldData,
                newData,
                "Removed user " + userToRemove.getUsername() + " (ID: " + userId + ") from lab " + labId,
                currentUser,
                request,
                String.valueOf(userId)
        );

        return ApiResponseHelper.successResponse("User removed from lab successfully", HttpStatus.OK);
    }


    //create a user in lab
//    @Transactional
//    @PostMapping("/create-user/{labId}")
//    public ResponseEntity<?> createUserInLab(
//            @RequestBody RegisterRequest registerRequest,
//            @PathVariable Long labId,
//            @RequestHeader("Authorization") String token) {
//
//        User currentUser = userAuthService.authenticateUser(token).orElse(null);
//        if (currentUser == null)
//            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
//
//        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
//        if (isAccessible == false) {
//            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
//        }
//
//        List<Long> moduleIds = registerRequest.getModules();
//        Set<ModuleEntity> modules = new HashSet<>();
//
//        for (Long moduleId : moduleIds) {
//            Optional<ModuleEntity> moduleOptional = moduleRepository.findById(moduleId);
//            if (!moduleOptional.isPresent()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Module with ID " + moduleId + " not found");
//            }
//            modules.add(moduleOptional.get());
//        }
//        Lab lab = labRepository.findById(labId).orElse(null);
//        if (lab == null)
//            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//
//        // Check creator of the lab
//        if (!lab.getCreatedBy().equals(currentUser)) {
//            return ApiResponseHelper.errorResponse("You are not authorized to create members in this lab", HttpStatus.UNAUTHORIZED);
//        }
//
//        //check the user is already a member of the lab using username and email
//        if (lab.getMembers().stream().anyMatch(user -> user.getUsername().equals(registerRequest.getUsername()) || user.getEmail().equals(registerRequest.getEmail()))) {
//            return ApiResponseHelper.errorResponse("User is already a member of this lab", HttpStatus.CONFLICT);
//        }
//
//        if (registerRequest.getUsername().isEmpty() || registerRequest.getPassword().isEmpty() || registerRequest.getEmail().isEmpty() || registerRequest.getFirstName().isEmpty() || registerRequest.getLastName().isEmpty()) {
//            return ApiResponseHelper.errorResponse("Please fill all the fields", HttpStatus.BAD_REQUEST);
//        }
//        if (!registerRequest.getEmail().contains("@") || !registerRequest.getEmail().contains(".")) {
//            return ApiResponseHelper.errorResponse("Please enter a valid email", HttpStatus.BAD_REQUEST);
//        }
//        if (registerRequest.getPassword().length() < 8) {
//            return ApiResponseHelper.errorResponse("Password must be at least 8 characters long", HttpStatus.BAD_REQUEST);
//        }
//        if (registerRequest.getUsername().length() < 4) {
//            return ApiResponseHelper.errorResponse("Username must be at least 4 characters long", HttpStatus.BAD_REQUEST);
//        }
//
//        if (userService.existsByEmail(registerRequest.getEmail())) {
//            return ApiResponseHelper.errorResponse("Email already exists", HttpStatus.BAD_REQUEST);
//        }
//        if (userService.existsByUsername(registerRequest.getUsername())) {
//            return ApiResponseHelper.errorResponse("Username already exists", HttpStatus.BAD_REQUEST);
//        }
//
//        User user = new User();
//        user.setUsername(registerRequest.getUsername());
//        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
//        user.setEmail(registerRequest.getEmail());
//        user.setFirstName(registerRequest.getFirstName());
//        user.setLastName(registerRequest.getLastName());
//        user.setPhone(registerRequest.getPhone());
//        user.setAddress(registerRequest.getAddress());
//        user.setCity(registerRequest.getCity());
//        user.setState(registerRequest.getState());
//        user.setZip(registerRequest.getZip());
//        user.setCountry(registerRequest.getCountry());
//        user.setVerified(registerRequest.isVerified());
//        user.setEnabled(true);
//        user.setCreatedBy(currentUser);
//        user.setModules(modules);
//        userService.saveUser(user);
//        if (lab.getMembers().contains(user)) {
//            return ApiResponseHelper.errorResponse("User is already a member of this lab", HttpStatus.CONFLICT);
//        }
//        lab.getMembers().add(user);
//        labRepository.save(lab);
//        return ApiResponseHelper.successResponse("User created and added to lab successfully", HttpStatus.OK);
//    }

    //update member details in lab
    @PutMapping("/update-user/{userId}")
    public ResponseEntity<?> updateUserInLab(
            @RequestBody RegisterRequest registerRequest,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Check if the user is authenticated
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        // Check if the user exists
        User userToUpdate = userLabService.getUserById(userId);
        if (userToUpdate == null)
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);

        //check createor of the lab
        if (!userToUpdate.getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to update this user", HttpStatus.UNAUTHORIZED);
        }

        // check feild is empty or not
        if (registerRequest.getUsername().isEmpty() || registerRequest.getPassword().isEmpty() || registerRequest.getEmail().isEmpty() || registerRequest.getFirstName().isEmpty() || registerRequest.getLastName().isEmpty()) {
            return ApiResponseHelper.errorResponse("Please fill all the fields", HttpStatus.BAD_REQUEST);
        }

        // check email is valid or not
        if (!registerRequest.getEmail().contains("@") || !registerRequest.getEmail().contains(".")) {
            return ApiResponseHelper.errorResponse("Please enter a valid email", HttpStatus.BAD_REQUEST);
        }

        // check password length
        if (registerRequest.getPassword().length() < 8) {
            return ApiResponseHelper.errorResponse("Password must be at least 8 characters long", HttpStatus.BAD_REQUEST);
        }

        // check username length
        if (registerRequest.getUsername().length() < 4) {
            return ApiResponseHelper.errorResponse("Username must be at least 4 characters long", HttpStatus.BAD_REQUEST);
        }

        // Capture old state before modification
        Map<String, Object> oldData = toUserAuditMap(userToUpdate);

        // Update the user
        userToUpdate.setUsername(registerRequest.getUsername());
        userToUpdate.setPassword(registerRequest.getPassword());
        userToUpdate.setEmail(registerRequest.getEmail());
        userToUpdate.setFirstName(registerRequest.getFirstName());
        userToUpdate.setLastName(registerRequest.getLastName());
        userToUpdate.setPhone(registerRequest.getPhone());
        userToUpdate.setAddress(registerRequest.getAddress());
        userToUpdate.setCity(registerRequest.getCity());
        userToUpdate.setState(registerRequest.getState());
        userToUpdate.setZip(registerRequest.getZip());
        userToUpdate.setCountry(registerRequest.getCountry());
        userToUpdate.setVerified(registerRequest.isVerified());
//        userToUpdate.setModules(registerRequest.getModules());
        userToUpdate.setEnabled(true);
        userToUpdate.setCreatedBy(currentUser);
        // Save the user
        userService.saveUser(userToUpdate);

        // Capture new state after modification
        User updatedUser = userLabService.getUserById(userId);
        Map<String, Object> newData = toUserAuditMap(updatedUser);

        // Get lab ID from user's labs
        Long labId = null;
        if (updatedUser.getLabs() != null && !updatedUser.getLabs().isEmpty()) {
            labId = updatedUser.getLabs().iterator().next().getId();
        }

        logLabMemberAudit(
                labId,
                "USER_UPDATE",
                oldData,
                newData,
                "Updated user " + updatedUser.getUsername() + " (ID: " + userId + ")",
                currentUser,
                request,
                String.valueOf(userId)
        );

        return ApiResponseHelper.successResponse("User updated successfully", HttpStatus.OK);
    }


    //delete user in lab if you are the creator and in user table there is field that contain a creator id
    @Transactional
    @DeleteMapping("/delete-user/{userId}")
    public ResponseEntity<?> deleteUserInLab(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Check if the user is authenticated
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        // Check if the user exists
        User userToDelete = userLabService.getUserById(userId);
        if (userToDelete == null)
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);

        //check createor of the lab
        if (!userToDelete.getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to delete this user", HttpStatus.UNAUTHORIZED);
        }

        // Capture old state before deletion
        Map<String, Object> oldData = toUserAuditMap(userToDelete);
        
        // Get lab IDs before deletion
        Set<Long> labIds = new HashSet<>();
        if (userToDelete.getLabs() != null) {
            for (Lab lab : userToDelete.getLabs()) {
                labIds.add(lab.getId());
            }
        }

        // Remove the user from all labs
        List<Lab> labs = labRepository.findAll();
        for (Lab lab : labs) {
            if (lab.getMembers().contains(userToDelete)) {
                lab.getMembers().remove(userToDelete);
                labRepository.save(lab);
            }
        }
        // Delete the user
        userService.deleteUser(userToDelete.getId());

        // Log audit for each lab the user was removed from
        for (Long labId : labIds) {
            logLabMemberAudit(
                    labId,
                    "USER_DELETE",
                    oldData,
                    null,
                    "Deleted user " + userToDelete.getUsername() + " (ID: " + userId + ")",
                    currentUser,
                    request,
                    String.valueOf(userId)
            );
        }

        return ApiResponseHelper.successResponse("User deleted successfully", HttpStatus.OK);

    }


    //========================= role assign and remove ========================

    //assign role to member
    @PutMapping("/assign-role/{userId}/role/{roleId}")
    public ResponseEntity<?> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {

        // Check if the user is authenticated
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        // Check if the user exists
        User user = userLabService.getUserById(userId);
        if (user == null)
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);


        //check user is member of the lab or not
        Optional<Lab> lab = labRepository.findByMembers(user);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("User is not a member of any lab", HttpStatus.NOT_FOUND);
        }

        //check createor of the lab
        if (!lab.get().getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to assign role to this user", HttpStatus.UNAUTHORIZED);
        }

        // Capture old state before modification
        Map<String, Object> oldData = toUserAuditMap(user);

        //assign role to user
        try {
            User updatedUser = userService.assignRole(userId, Math.toIntExact(roleId));
            
            // Capture new state after modification
            Map<String, Object> newData = toUserAuditMap(updatedUser);

            logLabMemberAudit(
                    lab.get().getId(),
                    "ROLE_ASSIGN",
                    oldData,
                    newData,
                    "Assigned role " + roleId + " to user " + updatedUser.getUsername() + " (ID: " + userId + ")",
                    currentUser,
                    request,
                    String.valueOf(userId)
            );

            return ApiResponseHelper.successResponse("Role assigned successfully", updatedUser);
        } catch (EntityNotFoundException e) {
            return ApiResponseHelper.errorResponse("User or Role not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An error occurred while assigning the role", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    //remove role from member
    @DeleteMapping("/remove-role/{userId}/role/{roleId}")
    public ResponseEntity<?> removeRole(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {

        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
        User user = userLabService.getUserById(userId);
        if (user == null)
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);
        Optional<Lab> lab = labRepository.findByMembers(user);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("User is not a member of any lab", HttpStatus.NOT_FOUND);
        }
        //check createor of the lab
        if (!lab.get().getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to remove role from this user", HttpStatus.UNAUTHORIZED);
        }

        // Capture old state before modification
        Map<String, Object> oldData = toUserAuditMap(user);

        try {
            User updatedUser = userService.removeRole(userId, Math.toIntExact(roleId));
            
            // Capture new state after modification
            Map<String, Object> newData = toUserAuditMap(updatedUser);

            logLabMemberAudit(
                    lab.get().getId(),
                    "ROLE_REMOVE",
                    oldData,
                    newData,
                    "Removed role " + roleId + " from user " + updatedUser.getUsername() + " (ID: " + userId + ")",
                    currentUser,
                    request,
                    String.valueOf(userId)
            );

            return ApiResponseHelper.successResponse("Role removed successfully", updatedUser);
        } catch (EntityNotFoundException e) {
            return ApiResponseHelper.errorResponse("User or Role not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An error occurred while removing the role", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    //get user by id of
    @Transactional
    @GetMapping("/get-user/{userId}")
    public ResponseEntity<?> getUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token
    ) {
        User currentUser = userAuthService.authenticateUser(token).orElse(null);

        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);
        return ApiResponseHelper.successResponse("User fetched successfully", user);
    }


    @Transactional
    @GetMapping("/get-user-labs")
    public ResponseEntity<?> getUserLabs(
            @RequestHeader("Authorization") String token
    ) {

        // Check if the user is authenticated
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);
        Set<Lab> labs = labRepository.findLabsByUserId(currentUser.getId());
        if (labs.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("No labs found", HttpStatus.OK, null);
        }
        List<LabListDTO> labListDTOs = labs.stream()
                .map(lab -> new LabListDTO(
                        lab.getId(),
                        lab.getName(),
                        lab.getAddress(),
                        lab.getCity(),
                        lab.getState(),
                        lab.getIsActive(),
                        lab.getDescription(),
                        lab.getCreatedBy().getUsername()
                ))
                .toList();
        return ApiResponseHelper.successResponseWithDataAndMessage("Labs fetched successfully", HttpStatus.OK, labListDTOs);
    }

    //--------------------get lab details by lab id---------------------\\
    @Transactional
    @GetMapping("/get-lab/{labId}")
    public ResponseEntity<?> getLabById(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token) {

        // Check if the user is authenticated
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        // Check if the lab exists
        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null)
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);

        // Check if the lab is accessible
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }
        LabDetailsDetails labDetails = new LabDetailsDetails(
                lab.getId(),
                lab.getName(),
                lab.getLabLogo(),
                lab.getAddress(),
                lab.getCity(),
                lab.getState(),
                lab.getIsActive(),
                lab.getDescription(),
                lab.getLabZip(),
                lab.getLabCountry(),
                lab.getLabPhone(),
                lab.getLabEmail(),
                lab.getLicenseNumber(),
                lab.getLabType(),
                lab.getCreatedBy().getUsername(),
                lab.getCreatedAt().toString(),
                lab.getUpdatedAt().toString(),
                lab.getDirectorName(),
                lab.getDirectorEmail(),
                lab.getDirectorPhone(),
                lab.getCertificationBody(),
                lab.getLabCertificate(),
                lab.getDirectorGovtId(),
                lab.getLabBusinessRegistration(),
                lab.getLabLicense(),
                lab.getTaxId(),
                lab.getLabAccreditation(),
                lab.getDataPrivacyAgreement()
        );
        return ApiResponseHelper.successResponseWithDataAndMessage("Lab details fetched successfully", HttpStatus.OK, labDetails);
    }

    private void logLabMemberAudit(Long labId,
                                   String action,
                                   Map<String, Object> oldData,
                                   Map<String, Object> newData,
                                   String changeReason,
                                   User currentUser,
                                   HttpServletRequest request,
                                   String entityId) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setModule("LabMember");
        auditLog.setEntityType("LabMember");
        auditLog.setLab_id(labId != null ? String.valueOf(labId) : "GLOBAL");
        auditLog.setActionType(action);
        auditLog.setChangeReason(changeReason != null ? changeReason : "");

        if (entityId != null) {
            auditLog.setEntityId(entityId);
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

    private Map<String, Object> toLabAuditMap(Lab lab) {
        if (lab == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", lab.getId());
        data.put("name", lab.getName());
        data.put("address", lab.getAddress());
        data.put("city", lab.getCity());
        data.put("state", lab.getState());
        data.put("description", lab.getDescription());
        data.put("isActive", lab.getIsActive());
        data.put("labLogo", lab.getLabLogo());
        data.put("licenseNumber", lab.getLicenseNumber());
        data.put("labType", lab.getLabType());
        data.put("labZip", lab.getLabZip());
        data.put("labCountry", lab.getLabCountry());
        data.put("labPhone", lab.getLabPhone());
        data.put("labEmail", lab.getLabEmail());
        data.put("directorName", lab.getDirectorName());
        data.put("directorEmail", lab.getDirectorEmail());
        data.put("directorPhone", lab.getDirectorPhone());
        data.put("certificationBody", lab.getCertificationBody());
        data.put("labCertificate", lab.getLabCertificate());
        data.put("directorGovtId", lab.getDirectorGovtId());
        data.put("labBusinessRegistration", lab.getLabBusinessRegistration());
        data.put("labLicense", lab.getLabLicense());
        data.put("taxId", lab.getTaxId());
        data.put("labAccreditation", lab.getLabAccreditation());
        data.put("dataPrivacyAgreement", lab.getDataPrivacyAgreement());
        data.put("createdAt", lab.getCreatedAt() != null ? lab.getCreatedAt().toString() : null);
        data.put("updatedAt", lab.getUpdatedAt() != null ? lab.getUpdatedAt().toString() : null);
        
        // Include member IDs
        if (lab.getMembers() != null) {
            List<Long> memberIds = lab.getMembers().stream()
                    .map(User::getId)
                    .sorted()
                    .collect(Collectors.toList());
            data.put("memberIds", memberIds);
            
            // Include member details
            List<Map<String, Object>> memberDetails = lab.getMembers().stream()
                    .map(member -> {
                        Map<String, Object> memberMap = new LinkedHashMap<>();
                        memberMap.put("id", member.getId());
                        memberMap.put("username", member.getUsername());
                        memberMap.put("email", member.getEmail());
                        memberMap.put("firstName", member.getFirstName());
                        memberMap.put("lastName", member.getLastName());
                        return memberMap;
                    })
                    .collect(Collectors.toList());
            data.put("members", memberDetails);
        } else {
            data.put("memberIds", new ArrayList<>());
            data.put("members", new ArrayList<>());
        }
        
        // Include created by
        if (lab.getCreatedBy() != null) {
            data.put("createdById", lab.getCreatedBy().getId());
            data.put("createdByUsername", lab.getCreatedBy().getUsername());
        }
        
        return data;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo(
            @RequestHeader("Authorization") String token
    ) {
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
        if (currentUser == null)
            return ApiResponseHelper.errorResponse("User not found or unauthorized", HttpStatus.UNAUTHORIZED);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", currentUser.getId());
        userInfo.put("username", currentUser.getUsername());
        userInfo.put("email", currentUser.getEmail());
        userInfo.put("firstName", currentUser.getFirstName());
        userInfo.put("lastName", currentUser.getLastName());
        userInfo.put("phone", currentUser.getPhone());
        userInfo.put("address", currentUser.getAddress());
        userInfo.put("city", currentUser.getCity());
        userInfo.put("state", currentUser.getState());
        userInfo.put("zip", currentUser.getZip());
        userInfo.put("country", currentUser.getCountry());
        userInfo.put("enabled", currentUser.isEnabled());
        userInfo.put("isVerified", currentUser.isVerified());
        userInfo.put("createdAt", currentUser.getCreatedAt() != null ? currentUser.getCreatedAt().toString() : null);
        userInfo.put("updatedAt", currentUser.getUpdatedAt() != null ? currentUser.getUpdatedAt().toString() : null);

        // Include roles
        if (currentUser.getRoles() != null) {
            List<String> roleNames = currentUser.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());
            userInfo.put("roles", roleNames);
        } else {
            userInfo.put("roles", new ArrayList<>());
        }

        return ApiResponseHelper.successResponse("Current user info fetched successfully", userInfo);
    }
}
