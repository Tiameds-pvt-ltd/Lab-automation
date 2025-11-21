package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.SampleEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.SampleAssocationRepository;
import tiameds.com.tiameds.repository.TestReferenceRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.repository.UserRepository;
import org.springframework.util.StreamUtils;
import tiameds.com.tiameds.utils.CustomMockMultipartFile;
import tiameds.com.tiameds.utils.EncodingUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class LabDefaultDataService {

    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final TestRepository testRepository;
    private final TestReferenceRepository testReferenceRepository;
    private final TestServices testServices;
    private final TestReferenceServices testReferenceServices;
    private final SampleAssocationRepository sampleAssocationRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    private static final List<String> DEFAULT_SAMPLE_NAMES = List.of(
            "Blood",
            "Serum",
            "Plasma",
            "Urine",
            "Stool",
            "Sputum",
            "Throat Swab",
            "Nasal Swab",
            "Oral Swab",
            "Vaginal Swab",
            "Seminal Fluid (Semen)",
            "CSF (Cerebrospinal Fluid)",
            "Pleural Fluid",
            "Ascitic Fluid",
            "Synovial Fluid (Joint Fluid)",
            "Biopsy Tissue",
            "Wound Swab",
            "Blood Culture Sample",
            "Skin Scraping",
            "Hair Sample"
    );

    public LabDefaultDataService(LabRepository labRepository,
                                 UserRepository userRepository,
                                 TestRepository testRepository,
                                 TestReferenceRepository testReferenceRepository,
                                 TestServices testServices,
                                 TestReferenceServices testReferenceServices,
                                 SampleAssocationRepository sampleAssocationRepository,
                                 SequenceGeneratorService sequenceGeneratorService) {
        this.labRepository = labRepository;
        this.userRepository = userRepository;
        this.testRepository = testRepository;
        this.testReferenceRepository = testReferenceRepository;
        this.testServices = testServices;
        this.testReferenceServices = testReferenceServices;
        this.sampleAssocationRepository = sampleAssocationRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void uploadDefaultData(Long labId, Long userId) {
        if (labId == null || userId == null) {
            log.warn("Skipping default CSV upload due to missing labId or userId (labId={}, userId={})", labId, userId);
            return;
        }

        Lab lab = labRepository.findById(labId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (lab == null || user == null) {
            log.warn("Skipping default CSV upload because lab or user could not be found (labId={}, userId={})", labId, userId);
            return;
        }

        if (testRepository.existsByLabs_Id(labId)) {
            log.info("Lab {} already has tests. Skipping default price list upload.", labId);
        } else {
            try {
                MultipartFile priceListFile = loadCsvAsMultipart("tiamed_price_list.csv");
                testServices.uploadCSV(priceListFile, lab);
                log.info("Uploaded default price list for lab {}", labId);
            } catch (Exception e) {
                log.error("Failed to upload default price list for lab {}: {}", labId, e.getMessage(), e);
            }
        }

        if (testReferenceRepository.existsByLabs_Id(labId)) {
            log.info("Lab {} already has test references. Skipping default reference upload.", labId);
        } else {
            try {
                MultipartFile referenceFile = loadCsvAsMultipart("Lab_Modified.xlsx-Sheet1 - Copy.csv");
                testReferenceServices.uploadCsv(lab, referenceFile, user);
                log.info("Uploaded default reference data for lab {}", labId);
            } catch (Exception e) {
                log.error("Failed to upload default reference data for lab {}: {}", labId, e.getMessage(), e);
            }
        }

        if (sampleAssocationRepository.findByLabId(labId).isEmpty()) {
            try {
                createDefaultSamples(labId);
                log.info("Uploaded default samples for lab {}", labId);
            } catch (Exception e) {
                log.error("Failed to upload default samples for lab {}: {}", labId, e.getMessage(), e);
            }
        } else {
            log.info("Lab {} already has samples. Skipping default sample upload.", labId);
        }
    }

    private MultipartFile loadCsvAsMultipart(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        byte[] rawBytes;
        try (InputStream inputStream = resource.getInputStream()) {
            rawBytes = StreamUtils.copyToByteArray(inputStream);
        }

        String normalizedContent = EncodingUtils.decodeWithUtf8Fallback(rawBytes);
        byte[] contentBytes = normalizedContent.getBytes(StandardCharsets.UTF_8);

        return new CustomMockMultipartFile(
                resourcePath,
                resourcePath,
                "text/csv",
                contentBytes
        );
    }

    private void createDefaultSamples(Long labId) {
        for (String sampleName : DEFAULT_SAMPLE_NAMES) {
            String normalizedName = sampleName.trim();
            if (normalizedName.isEmpty()) {
                continue;
            }
            boolean exists = sampleAssocationRepository.findByNameAndLabId(normalizedName, labId).isPresent();
            if (exists) {
                continue;
            }
            SampleEntity sample = new SampleEntity();
            sample.setLabId(labId);
            sample.setName(normalizedName);
            sample.setSampleCode(sequenceGeneratorService.generateCode(labId, EntityType.SAMPLE));
            sampleAssocationRepository.save(sample);
        }
    }
}

