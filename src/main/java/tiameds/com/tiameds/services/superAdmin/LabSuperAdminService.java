package tiameds.com.tiameds.services.superAdmin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.LabListDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LabSuperAdminService {

    private final LabRepository labRepository;

    public LabSuperAdminService(LabRepository labRepository) {
        this.labRepository = labRepository;
    }

    public List<LabListDTO> getLabs() {
        return labRepository.findAll().stream()
                .map(lab -> new LabListDTO(
                        lab.getId(),
                        lab.getName(),
                        lab.getAddress(),
                        lab.getCity(),
                        lab.getState(),
                        lab.getIsActive(),
                        lab.getDescription(),
                        lab.getCreatedByName(),
                        lab.getLabPhone()
                ))
                .collect(Collectors.toList());
    }



    public void updateLabStatus(long labId, LabListDTO labListDTO) {
        Optional<Lab> labOptional = labRepository.findById(labId);

        if (labOptional.isEmpty()) {
            throw new RuntimeException("Lab not found"); // Throw an exception for better handling
        }

        Lab labToUpdate = labOptional.get();
        labToUpdate.setIsActive(labListDTO.getIsActive());
        labRepository.save(labToUpdate);
    }


}
