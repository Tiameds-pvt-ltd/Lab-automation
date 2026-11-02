package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.auth.UserProfileDTO;
import tiameds.com.tiameds.dto.auth.UserProfileUpdateDTO;
import tiameds.com.tiameds.dto.lab.LabProfileDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Profile screen: one read of everything about the logged-in user and their lab(s), plus the user's own
 * personal-details edit. The lab and certificate data comes from the same services as their own endpoints, so the
 * rules (membership check, what is editable) stay in one place.
 */
@Slf4j
@Service
public class LabProfileService {

    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final LabOnboardingDetailsService labOnboardingDetailsService;
    private final LabDocumentService labDocumentService;
    private final LabCertificateRequestService labCertificateRequestService;

    public LabProfileService(UserRepository userRepository,
                             LabRepository labRepository,
                             LabOnboardingDetailsService labOnboardingDetailsService,
                             LabDocumentService labDocumentService,
                             LabCertificateRequestService labCertificateRequestService) {
        this.userRepository = userRepository;
        this.labRepository = labRepository;
        this.labOnboardingDetailsService = labOnboardingDetailsService;
        this.labDocumentService = labDocumentService;
        this.labCertificateRequestService = labCertificateRequestService;
    }

    /**
     * @param labId only this lab (must be one the user belongs to); null returns every lab the user belongs to
     * @throws NoSuchElementException if labId does not exist (maps to 404)
     * @throws IllegalStateException  if the user is not a member of labId
     */
    @Transactional(readOnly = true)
    public LabProfileDTO getProfile(User currentUser, Long labId) {
        List<Lab> labs;
        if (labId != null) {
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new NoSuchElementException("Lab not found"));
            labs = List.of(lab);
        } else {
            labs = labRepository.findLabsByUserId(currentUser.getId()).stream()
                    .sorted(Comparator.comparingLong(Lab::getId))
                    .toList();
        }

        List<LabProfileDTO.LabSection> sections = labs.stream()
                .map(lab -> toSection(currentUser, lab))
                .toList();

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return LabProfileDTO.builder().user(toUserDto(user)).labs(sections).build();
    }

    /**
     * Edits the user's own personal details. Only the fields that are sent change; all-or-nothing.
     *
     * @throws IllegalArgumentException if nothing was sent or a sent field is blank (maps to 400)
     */
    @Transactional
    public UserProfileDTO updateMyDetails(User currentUser, UserProfileUpdateDTO request) {
        if (request == null || (request.getFirstName() == null && request.getLastName() == null
                && request.getPhone() == null && request.getAddress() == null && request.getCity() == null
                && request.getState() == null && request.getZip() == null && request.getCountry() == null)) {
            throw new IllegalArgumentException("At least one field is required");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        set("First name", request.getFirstName(), user::setFirstName);
        set("Last name", request.getLastName(), user::setLastName);
        set("Phone", request.getPhone(), user::setPhone);
        set("Address", request.getAddress(), user::setAddress);
        set("City", request.getCity(), user::setCity);
        set("State", request.getState(), user::setState);
        set("Zip", request.getZip(), user::setZip);
        set("Country", request.getCountry(), user::setCountry);

        User saved = userRepository.save(user);
        log.info("Personal details updated by user {}", saved.getUsername());
        return toUserDto(saved);
    }

    private LabProfileDTO.LabSection toSection(User currentUser, Lab lab) {
        Long id = lab.getId();
        return LabProfileDTO.LabSection.builder()
                .labId(id)
                .labName(lab.getName())
                .isActive(lab.getIsActive())
                .approvalStatus(lab.getApprovalStatus() == null ? null : lab.getApprovalStatus().name())
                .details(labOnboardingDetailsService.getDetails(currentUser, id))
                .documents(labDocumentService.getDocuments(currentUser, id))
                .certificateRequests(labCertificateRequestService.getLabRequests(currentUser, id))
                .build();
    }

    private void set(String label, String value, Consumer<String> setter) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        setter.accept(value.trim());
    }

    private UserProfileDTO toUserDto(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .zip(user.getZip())
                .country(user.getCountry())
                .roles(user.getRoles() == null ? List.of() : user.getRoles().stream().map(Role::getName).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
