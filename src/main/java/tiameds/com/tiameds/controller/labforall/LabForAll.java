package tiameds.com.tiameds.controller.labforall;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.dto.lab.LabListDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.UserLabService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/lab-for-all")
public class LabForAll {

    private final UserRepository userRepository;
    private UserLabService userLabService;
    private UserAuthService userAuthService;
    private LabRepository labRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private LabAccessableFilter labAccessableFilter;

    public LabForAll(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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
}
