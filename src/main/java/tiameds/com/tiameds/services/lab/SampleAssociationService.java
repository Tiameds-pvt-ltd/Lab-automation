package tiameds.com.tiameds.services.lab;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.SampleDto;
import tiameds.com.tiameds.entity.SampleEntity;
import tiameds.com.tiameds.repository.SampleAssocationRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class SampleAssociationService {

    private final SampleAssocationRepository sampleAssociationRepository;

    public SampleAssociationService(SampleAssocationRepository sampleAssociationRepository) {
        this.sampleAssociationRepository = sampleAssociationRepository;
    }

    //get all sample which is constant for all labs
    public List<SampleDto> getSampleAssociationList() {
        return sampleAssociationRepository.findAll().stream().map(sample -> {
            SampleDto sampleDto = new SampleDto();
            sampleDto.setId(sample.getId());
            sampleDto.setName(sample.getName());
            sampleDto.setCreatedAt(sample.getCreatedAt());
            sampleDto.setUpdatedAt(sample.getUpdatedAt());
            return sampleDto;
        }).collect(Collectors.toList());
    }


    public ResponseEntity<?> createSample(SampleDto sampleDto) {
        //check if the sample exists on  db
        if (sampleAssociationRepository.findByName(sampleDto.getName()).isPresent()) {
              return ApiResponseHelper.errorResponse("Sample already exists", HttpStatus.BAD_REQUEST);
        }
        //create the sample
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setName(sampleDto.getName());
        sampleAssociationRepository.save(sampleEntity);
        return ApiResponseHelper.successResponse("Sample created successfully", sampleDto);
    }


    public void updateSample(Long sampleId, SampleDto sampleDto) {
        //check if the sample exists on  db
        if (sampleAssociationRepository.findById(sampleId).isEmpty()) {
            throw new RuntimeException("Sample not found");
        }
        //update the sample using the dto
        SampleEntity sampleEntity = sampleAssociationRepository.findById(sampleId).get();
        sampleEntity.setName(sampleDto.getName());
        sampleAssociationRepository.save(sampleEntity);
    }

    public void deleteSample(Long sampleId) {
        //check if the sample exists on  db
        if (sampleAssociationRepository.findById(sampleId).isEmpty()) {
            throw new RuntimeException("Sample not found");
        }
        //delete the sample
        sampleAssociationRepository.deleteById(sampleId);
    }


}
