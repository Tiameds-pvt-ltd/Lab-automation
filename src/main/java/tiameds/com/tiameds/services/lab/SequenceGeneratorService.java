package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.LabEntitySequence;
import tiameds.com.tiameds.repository.LabEntitySequenceRepository;

import java.text.DecimalFormat;

/**
 * Service for generating unique, sequential codes for entities per lab.
 * Uses pessimistic locking to ensure thread-safe sequence generation.
 * 
 * Example usage:
 *   String code = sequenceGeneratorService.generateCode(labId, EntityType.PATIENT.name(), EntityType.PATIENT.getPrefix());
 *   // Returns: PAT1-00001, PAT1-00002, etc. (includes lab ID for global uniqueness)
 */
@Slf4j
@Service
public class SequenceGeneratorService {

    private final LabEntitySequenceRepository sequenceRepository;

    @Autowired
    public SequenceGeneratorService(LabEntitySequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Generates a unique sequential code for the given lab and entity type.
     * Uses pessimistic locking to ensure thread-safe sequence generation.
     * 
     * @param labId the lab ID
     * @param entityName the entity name (e.g., "PATIENT", "VISIT")
     * @param prefix the prefix for the code (e.g., "PAT", "VIS")
     * @return the generated code (e.g., "PAT1-00001", "VIS1-00001") - includes lab ID for global uniqueness
     */
    @Transactional
    public String generateCode(Long labId, String entityName, String prefix) {
        if (labId == null) {
            throw new IllegalArgumentException("Lab ID cannot be null");
        }
        if (entityName == null || entityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity name cannot be null or empty");
        }
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }

        try {
            // Use pessimistic write lock to prevent concurrent modifications
            LabEntitySequence sequence = sequenceRepository
                    .findByLabIdAndEntityNameWithLock(labId, entityName)
                    .orElseGet(() -> {
                        // Create new sequence record if it doesn't exist
                        LabEntitySequence newSequence = new LabEntitySequence();
                        newSequence.setLabId(labId);
                        newSequence.setEntityName(entityName);
                        newSequence.setLastNumber(0L);
                        return newSequence;
                    });

            // Increment the sequence number
            sequence.setLastNumber(sequence.getLastNumber() + 1);
            
            // Save the updated sequence
            sequenceRepository.save(sequence);

            // Format the code with zero-padding and include lab ID for global uniqueness
            // Format: PREFIXLABID-NUMBER (e.g., PAT1-00001, VIS2-00001)
            DecimalFormat formatter = new DecimalFormat("00000");
            String formattedNumber = formatter.format(sequence.getLastNumber());
            
            return prefix + labId + "-" + formattedNumber;

        } catch (Exception e) {
            log.error("Error generating sequence code for labId: {}, entityName: {}, prefix: {}", 
                    labId, entityName, prefix, e);
            throw new RuntimeException("Failed to generate sequence code: " + e.getMessage(), e);
        }
    }

    /**
     * Ensures the stored sequence for the given lab/entity is at least the provided value.
     * If the existing lastNumber is lower, it is updated so future code generations
     * continue from the higher boundary (useful when importing historical data).
     *
     * @param labId lab identifier
     * @param entityType entity enum
     * @param minimumValue minimum value that sequence lastNumber must reach
     */
    @Transactional
    public void ensureMinimumSequence(Long labId, EntityType entityType, long minimumValue) {
        if (labId == null) {
            throw new IllegalArgumentException("Lab ID cannot be null");
        }
        LabEntitySequence sequence = sequenceRepository
                .findByLabIdAndEntityNameWithLock(labId, entityType.getEntityName())
                .orElseGet(() -> {
                    LabEntitySequence newSequence = new LabEntitySequence();
                    newSequence.setLabId(labId);
                    newSequence.setEntityName(entityType.getEntityName());
                    newSequence.setLastNumber(0L);
                    return newSequence;
                });

        if (sequence.getLastNumber() < minimumValue) {
            sequence.setLastNumber(minimumValue);
            sequenceRepository.save(sequence);
        }
    }

    /**
     * Generates a code using EntityType enum.
     * Convenience method that uses the enum's name and prefix.
     * 
     * @param labId the lab ID
     * @param entityType the entity type enum
     * @return the generated code
     */
    @Transactional
    public String generateCode(Long labId, EntityType entityType) {
        return generateCode(labId, entityType.getEntityName(), entityType.getPrefix());
    }

    /**
     * Gets the current sequence number for a lab and entity type without incrementing.
     * Useful for checking the last used number.
     * 
     * @param labId the lab ID
     * @param entityName the entity name
     * @return the current sequence number, or 0 if no sequence exists
     */
    @Transactional(readOnly = true)
    public Long getCurrentSequenceNumber(Long labId, String entityName) {
        return sequenceRepository
                .findByLabIdAndEntityName(labId, entityName)
                .map(LabEntitySequence::getLastNumber)
                .orElse(0L);
    }

    /**
     * Gets the current sequence number using EntityType enum.
     * 
     * @param labId the lab ID
     * @param entityType the entity type enum
     * @return the current sequence number, or 0 if no sequence exists
     */
    @Transactional(readOnly = true)
    public Long getCurrentSequenceNumber(Long labId, EntityType entityType) {
        return getCurrentSequenceNumber(labId, entityType.getEntityName());
    }
}

