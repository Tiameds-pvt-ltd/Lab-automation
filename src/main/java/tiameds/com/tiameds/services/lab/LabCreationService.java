package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.LabCreationResponseDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;

/**
 * Service for handling lab creation with user association.
 * Similar to onboarding flow but for existing authenticated users.
 */
@Slf4j
@Service
public class LabCreationService {

    private final LabRepository labRepository;
    private final UserLabService userLabService;

    public LabCreationService(LabRepository labRepository, UserLabService userLabService) {
        this.labRepository = labRepository;
        this.userLabService = userLabService;
    }

    /**
     * Creates a lab and associates the user with it.
     * 
     * Flow:
     * 1. Validates lab name doesn't already exist
     * 2. Creates Lab entity with all provided information
     * 3. Associates user with the lab
     * 4. Saves lab to database
     * 
     * This is all done in a single transaction to ensure atomicity.
     *
     * @param labRequestDTO The lab request containing lab information
     * @param currentUser The authenticated user creating the lab
     * @return LabCreationResponseDTO with created lab and user details
     */
    @Transactional
    public LabCreationResponseDTO createLabWithUserAssociation(LabRequestDTO labRequestDTO, User currentUser) {
        // Step 1: Check if the lab already exists
        if (userLabService.existsLabByName(labRequestDTO.getName())) {
            throw new IllegalArgumentException("Lab with this name already exists");
        }

        // Step 2: Create Lab entity
        Lab lab = new Lab();
        
        // Basic fields
        lab.setName(labRequestDTO.getName());
        lab.setAddress(labRequestDTO.getAddress());
        lab.setCity(labRequestDTO.getCity());
        lab.setState(labRequestDTO.getState());
        lab.setDescription(labRequestDTO.getDescription());
        lab.setIsActive(true);
        lab.setCreatedBy(currentUser);

        // Business fields
        lab.setLabLogo(labRequestDTO.getLabLogo());
        lab.setLicenseNumber(labRequestDTO.getLicenseNumber());
        lab.setLabType(labRequestDTO.getLabType());
        lab.setLabZip(labRequestDTO.getLabZip());
        lab.setLabCountry(labRequestDTO.getLabCountry());
        lab.setLabPhone(labRequestDTO.getLabPhone());
        lab.setLabEmail(labRequestDTO.getLabEmail());

        // Director fields
        lab.setDirectorName(labRequestDTO.getDirectorName());
        lab.setDirectorEmail(labRequestDTO.getDirectorEmail());
        lab.setDirectorPhone(labRequestDTO.getDirectorPhone());
        lab.setDirectorGovtId(labRequestDTO.getDirectorGovtId());

        // Compliance fields
        lab.setCertificationBody(labRequestDTO.getCertificationBody());
        lab.setLabCertificate(labRequestDTO.getLabCertificate());
        lab.setLabBusinessRegistration(labRequestDTO.getLabBusinessRegistration());
        lab.setLabLicense(labRequestDTO.getLabLicense());
        lab.setTaxId(labRequestDTO.getTaxId());
        lab.setLabAccreditation(labRequestDTO.getLabAccreditation());
        lab.setDataPrivacyAgreement(labRequestDTO.getDataPrivacyAgreement() != null && labRequestDTO.getDataPrivacyAgreement());

        // Step 3: Save lab to database
        Lab savedLab = labRepository.save(lab);
        log.info("Created lab: {} (ID: {})", savedLab.getName(), savedLab.getId());

        // Step 4: Add user as member of the lab
        savedLab.getMembers().add(currentUser);
        labRepository.save(savedLab);
        log.info("Added user {} as member of lab {}", currentUser.getUsername(), savedLab.getName());

        // Step 5: Build response
        return LabCreationResponseDTO.builder()
                .message("Lab created successfully and user added as a member")
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .labId(savedLab.getId())
                .labName(savedLab.getName())
                .labActive(savedLab.getIsActive())
                .build();
    }
}


