package tiameds.com.tiameds.services.lab;


import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.SampleDto;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.SampleEntity;
import tiameds.com.tiameds.repository.SampleAssocationRepository;
import tiameds.com.tiameds.services.lab.SequenceGeneratorService;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class SampleAssociationService {

    private final SampleAssocationRepository sampleAssociationRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public SampleAssociationService(SampleAssocationRepository sampleAssociationRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.sampleAssociationRepository = sampleAssociationRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    //get all samples for a specific lab
    public List<SampleDto> getSampleAssociationList(Long labId) {
        return sampleAssociationRepository.findByLabId(labId).stream().map(sample -> {
            SampleDto sampleDto = new SampleDto();
            sampleDto.setId(sample.getId());
            sampleDto.setName(sample.getName());
            sampleDto.setCreatedAt(sample.getCreatedAt());
            sampleDto.setUpdatedAt(sample.getUpdatedAt());
            return sampleDto;
        }).collect(Collectors.toList());
    }


    public SampleDto createSample(SampleDto sampleDto, Long labId) {
        //check if the sample exists for this lab
        if (sampleAssociationRepository.findByNameAndLabId(sampleDto.getName(), labId).isPresent()) {
            throw new RuntimeException("Sample already exists for this lab");
        }
        //create the sample
        SampleEntity sampleEntity = new SampleEntity();
        
        // Generate unique sample code using sequence generator for this lab
        String sampleCode = sequenceGeneratorService.generateCode(labId, EntityType.SAMPLE);
        sampleEntity.setSampleCode(sampleCode);
        
        sampleEntity.setName(sampleDto.getName());
        sampleEntity.setLabId(labId);
        SampleEntity saved = sampleAssociationRepository.save(sampleEntity);
        return toSampleDto(saved);
    }


    public SampleDto updateSample(Long sampleId, SampleDto sampleDto, Long labId) {
        //check if the sample exists for this lab
        SampleEntity sampleEntity = sampleAssociationRepository.findById(sampleId)
                .orElseThrow(() -> new RuntimeException("Sample not found"));
        
        // Verify the sample belongs to the lab
        if (!sampleEntity.getLabId().equals(labId)) {
            throw new RuntimeException("Sample does not belong to this lab");
        }
        
        // Check if another sample with the same name exists for this lab (excluding current sample)
        sampleAssociationRepository.findByNameAndLabId(sampleDto.getName(), labId)
                .ifPresent(existing -> {
                    if (existing.getId() != sampleId) {
                        throw new RuntimeException("A sample with this name already exists for this lab");
                    }
                });
        
        sampleEntity.setName(sampleDto.getName());
        SampleEntity saved = sampleAssociationRepository.save(sampleEntity);
        return toSampleDto(saved);
    }

    public SampleDto deleteSample(Long sampleId, Long labId) {
        //check if the sample exists for this lab
        SampleEntity sampleEntity = sampleAssociationRepository.findById(sampleId)
                .orElseThrow(() -> new RuntimeException("Sample not found"));
        
        // Verify the sample belongs to the lab
        if (!sampleEntity.getLabId().equals(labId)) {
            throw new RuntimeException("Sample does not belong to this lab");
        }
        
        SampleDto snapshot = toSampleDto(sampleEntity);
        sampleAssociationRepository.delete(sampleEntity);
        return snapshot;
    }

    public SampleDto getSampleSnapshot(Long sampleId, Long labId) {
        return sampleAssociationRepository.findById(sampleId)
                .map(sample -> {
                    // Verify the sample belongs to the lab
                    if (!sample.getLabId().equals(labId)) {
                        return null;
                    }
                    return toSampleDto(sample);
                })
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
