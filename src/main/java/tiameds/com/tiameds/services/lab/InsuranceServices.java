package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.InsuranceDTO;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.InsuranceEntity;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.repository.InsuranceRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.stream.Collectors;

@Slf4j
@Service
public class InsuranceServices {

    private InsuranceRepository insuranceRepository;
    private LabRepository labRepository;
    private SequenceGeneratorService sequenceGeneratorService;

    public InsuranceServices(InsuranceRepository insuranceRepository, LabRepository labRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.insuranceRepository = insuranceRepository;
        this.labRepository = labRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public void addInsurance(Long labId, InsuranceDTO insuranceDTO) {
        //retrieve the lab and authenticate the user
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        //check if insurance already exists in the particular lab
        if (insuranceRepository.existsByNameAndLabId(insuranceDTO.getName(), labId)) {
            ApiResponseHelper.errorResponse("Insurance already exists in this lab", HttpStatus.BAD_REQUEST);
        }
        //create a new insurance and add to lab
        InsuranceEntity insurance = new InsuranceEntity();
        
        // Generate unique insurance code using sequence generator
        String insuranceCode = sequenceGeneratorService.generateCode(labId, EntityType.INSURANCE);
        insurance.setInsuranceCode(insuranceCode);
        
        insurance.setName(insuranceDTO.getName());
        insurance.setDescription(insuranceDTO.getDescription());
        insurance.setPrice(insuranceDTO.getPrice());
        insurance.setDuration(insuranceDTO.getDuration());
        insurance.setCoverageLimit(insuranceDTO.getCoverageLimit());
        insurance.setCoverageType(insuranceDTO.getCoverageType());
        insurance.setStatus(insuranceDTO.getStatus());
        insurance.setProvider(insuranceDTO.getProvider());
        insurance.setLab(lab);
        insuranceRepository.save(insurance);

    }


    public Object getAllInsurance(Long labId) {

        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        return lab.getInsurance().stream()
                .map(insurance -> new InsuranceDTO(
                        insurance.getId(),
                        insurance.getName(),
                        insurance.getDescription(),
                        insurance.getPrice(),
                        insurance.getDuration(),
                        insurance.getCoverageLimit(),
                        insurance.getCoverageType(),
                        insurance.getStatus(),
                        insurance.getProvider()
                ))
                .collect(Collectors.toList());
    }

    // get those insurance which are matched with labid and insuranceid
    public Object getInsuranceById(Long labId, Long insuranceId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        // Check if the lab contains the specified insurance
        InsuranceEntity insurance = lab.getInsurance().stream()
                .filter(insuranceEntity -> insuranceEntity.getId() == insuranceId.intValue())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Insurance not found"));

        return new InsuranceDTO(
                insurance.getId(),
                insurance.getName(),
                insurance.getDescription(),
                insurance.getPrice(),
                insurance.getDuration(),
                insurance.getCoverageLimit(),
                insurance.getCoverageType(),
                insurance.getStatus(),
                insurance.getProvider()
        );
    }



    // update insurance where labid and insuranceid are matched means insurance is associated with that lab only
    public void updateInsurance(Long labId, Long insuranceId, InsuranceDTO insuranceDTO) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        // Check if the lab contains the specified insurance
        InsuranceEntity insurance = lab.getInsurance().stream()
                .filter(insuranceEntity -> insuranceEntity.getId() == insuranceId.intValue())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Insurance not found"));

        insurance.setName(insuranceDTO.getName());
        insurance.setDescription(insuranceDTO.getDescription());
        insurance.setPrice(insuranceDTO.getPrice());
        insurance.setDuration(insuranceDTO.getDuration());
        insurance.setCoverageLimit(insuranceDTO.getCoverageLimit());
        insurance.setCoverageType(insuranceDTO.getCoverageType());
        insurance.setStatus(insuranceDTO.getStatus());
        insurance.setProvider(insuranceDTO.getProvider());
        insuranceRepository.save(insurance);
    }


    // delete insurance where labid and insuranceid are matched means insurance is associated with that lab only
    public void deleteInsurance(Long labId, Long insuranceId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        // Check if the lab contains the specified insurance
        InsuranceEntity insurance = lab.getInsurance().stream()
                .filter(insuranceEntity -> insuranceEntity.getId() == insuranceId.intValue())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Insurance not found"));

        insuranceRepository.delete(insurance);
    }
}
