package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.TestReferenceRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.utils.CustomMockMultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class LabDefaultDataService {

    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final TestRepository testRepository;
    private final TestReferenceRepository testReferenceRepository;
    private final TestServices testServices;
    private final TestReferenceServices testReferenceServices;

    public LabDefaultDataService(LabRepository labRepository,
                                 UserRepository userRepository,
                                 TestRepository testRepository,
                                 TestReferenceRepository testReferenceRepository,
                                 TestServices testServices,
                                 TestReferenceServices testReferenceServices) {
        this.labRepository = labRepository;
        this.userRepository = userRepository;
        this.testRepository = testRepository;
        this.testReferenceRepository = testReferenceRepository;
        this.testServices = testServices;
        this.testReferenceServices = testReferenceServices;
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
    }

    private MultipartFile loadCsvAsMultipart(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            byte[] contentBytes = contentBuilder.toString().getBytes(StandardCharsets.UTF_8);
            return new CustomMockMultipartFile(
                    resourcePath,
                    resourcePath,
                    "text/csv",
                    contentBytes
            );
        }
    }
}

