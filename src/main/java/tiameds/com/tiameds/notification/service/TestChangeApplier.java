package tiameds.com.tiameds.notification.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.TestDTO;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.services.lab.SequenceGeneratorService;

/**
 * Applies an approved test change. Mirrors what TestController does for add / update / remove (same checks, same
 * soft delete) so an approved change ends up exactly as if the admin had made it directly.
 * Runs inside the caller's transaction (NotificationService.approve).
 */
@Component
public class TestChangeApplier {

    /** The test as it was before and after the change (before is null for ADD, after is null for DELETE). */
    public record Result(TestDTO before, TestDTO after) {
    }

    private final TestRepository testRepository;
    private final LabRepository labRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public TestChangeApplier(TestRepository testRepository,
                             LabRepository labRepository,
                             SequenceGeneratorService sequenceGeneratorService) {
        this.testRepository = testRepository;
        this.labRepository = labRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public Test requireTestOfLab(Lab lab, Long testId) {
        if (testId == null) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "Test id is required");
        }
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "Test not found"));
        if (!lab.getTests().contains(test)) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "Test does not belong to this lab");
        }
        return test;
    }

    public Result add(Lab lab, TestDTO dto) {
        for (Test existing : lab.getTests()) {
            if (existing.isActive() && existing.getName().equals(dto.getName())) {
                throw new NotificationException(HttpStatus.CONFLICT,
                        "Test already exists in the lab; reject this request instead");
            }
        }
        Test test = new Test();
        test.setTestCode(sequenceGeneratorService.generateCode(lab.getId(), EntityType.TEST));
        test.setCategory(dto.getCategory());
        test.setName(dto.getName());
        test.setPrice(dto.getPrice());
        lab.addTest(test);
        labRepository.saveAndFlush(lab);
        return new Result(null, toDto(test));
    }

    public Result update(Lab lab, Long testId, TestDTO dto) {
        Test test = requireTestOfLab(lab, testId);
        TestDTO before = toDto(test);
        test.setCategory(dto.getCategory());
        test.setName(dto.getName());
        test.setPrice(dto.getPrice());
        testRepository.save(test);
        return new Result(before, toDto(test));
    }

    public Result delete(Lab lab, Long testId) {
        Test test = requireTestOfLab(lab, testId);
        TestDTO before = toDto(test);
        if (!test.isActive()) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "Test is already removed");
        }
        // Soft delete, same as TestController.removeTest
        test.setActive(false);
        testRepository.save(test);
        return new Result(before, null);
    }

    public TestDTO toDto(Test test) {
        return new TestDTO(test.getId(), test.getTestCode(), test.getCategory(), test.getName(),
                test.getPrice(), test.getCreatedAt(), test.getUpdatedAt());
    }
}
