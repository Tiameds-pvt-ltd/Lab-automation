package tiameds.com.tiameds.controller.lab;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.auth.MemberDetailsUpdate;
import tiameds.com.tiameds.dto.auth.MemberRegisterDto;
import tiameds.com.tiameds.dto.lab.UserInLabDTO;
import tiameds.com.tiameds.entity.Lab;
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

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/lab/admin")
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

    @Autowired
    public LabAdminController(
            UserLabService userLabService,
            UserAuthService userAuthService,
            LabRepository labRepository, UserService userService,
            PasswordEncoder passwordEncoder,
            ModuleRepository moduleRepository,
            LabAccessableFilter labAccessableFilter,
            UserRepository userRepository, MemberUserServices memberUserServices) {
        this.userLabService = userLabService;
        this.userAuthService = userAuthService;
        this.labRepository = labRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.moduleRepository = moduleRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.userRepository = userRepository;
        this.memberUserServices = memberUserServices;
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
            @RequestHeader("Authorization") String token) {

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

        // Create a new user and add to the lab
        memberUserServices.createUserAndAddToLab(registerRequest, lab, currentUser);

        return ApiResponseHelper.successResponse("User created and added to lab successfully", HttpStatus.OK);
    }


     // update user in lab
    @Transactional
    @PutMapping("/update-user/{labId}/{userId}")
    public ResponseEntity<?> updateUserInLab(
            @PathVariable Long labId,
            @PathVariable Long userId,
            @RequestBody MemberDetailsUpdate registerRequest,
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
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Check creator of the lab
        if (!lab.getCreatedBy().equals(currentUser)) {
            return ApiResponseHelper.errorResponse("You are not authorized to update members in this lab", HttpStatus.UNAUTHORIZED);
        }

        User userToUpdate = userRepository.findById(userId).orElse(null);
        if (userToUpdate == null) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.NOT_FOUND);
        }

        // delegate to memberUserServices to update user and add to lab
        memberUserServices.updateUserInLab(registerRequest, userToUpdate, lab, currentUser);

        return ApiResponseHelper.successResponse("User updated successfully", HttpStatus.OK);

    }

    //reset user password
    @Transactional
    @PutMapping("/reset-password/{labId}/{userId}")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long labId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordRequest, // Change to accept JSON object
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

        // Reset password
        userToUpdate.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userToUpdate);
        return ApiResponseHelper.successResponse("Password reset successfully", HttpStatus.OK);
    }


}

