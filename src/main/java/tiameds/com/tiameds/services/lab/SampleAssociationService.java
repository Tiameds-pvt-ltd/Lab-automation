package tiameds.com.tiameds.services.lab;


import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.SampleDto;
import tiameds.com.tiameds.entity.SampleEntity;
import tiameds.com.tiameds.repository.SampleAssocationRepository;

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


    public SampleDto createSample(SampleDto sampleDto) {
        //check if the sample exists on  db
        if (sampleAssociationRepository.findByName(sampleDto.getName()).isPresent()) {
            throw new RuntimeException("Sample already exists");
        }
        //create the sample
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setName(sampleDto.getName());
        SampleEntity saved = sampleAssociationRepository.save(sampleEntity);
        return toSampleDto(saved);
    }


    public SampleDto updateSample(Long sampleId, SampleDto sampleDto) {
        //check if the sample exists on  db
        SampleEntity sampleEntity = sampleAssociationRepository.findById(sampleId)
                .orElseThrow(() -> new RuntimeException("Sample not found"));
        sampleEntity.setName(sampleDto.getName());
        SampleEntity saved = sampleAssociationRepository.save(sampleEntity);
        return toSampleDto(saved);
    }

    public SampleDto deleteSample(Long sampleId) {
        //check if the sample exists on  db
        SampleEntity sampleEntity = sampleAssociationRepository.findById(sampleId)
                .orElseThrow(() -> new RuntimeException("Sample not found"));
        SampleDto snapshot = toSampleDto(sampleEntity);
        sampleAssociationRepository.delete(sampleEntity);
        return snapshot;
    }

    public SampleDto getSampleSnapshot(Long sampleId) {
        return sampleAssociationRepository.findById(sampleId)
                .map(this::toSampleDto)
                .orElse(null);
    }

    private SampleDto toSampleDto(SampleEntity sampleEntity) {
        SampleDto dto = new SampleDto();
        dto.setId(sampleEntity.getId());
        dto.setName(sampleEntity.getName());
        dto.setCreatedAt(sampleEntity.getCreatedAt());
        dto.setUpdatedAt(sampleEntity.getUpdatedAt());
        return dto;
    }


}
