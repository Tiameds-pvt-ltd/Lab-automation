package tiameds.com.tiameds.services.lab;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.ReportDto;
import tiameds.com.tiameds.entity.ReportEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.ReportRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final VisitRepository visitRepository;
    private final TestRepository testRepository;
    private final LabRepository labRepository;

    public ReportService(ReportRepository reportRepository, VisitRepository visitRepository, TestRepository testRepository, LabRepository labRepository) {
        this.reportRepository = reportRepository;
        this.visitRepository = visitRepository;
        this.testRepository = testRepository;
        this.labRepository = labRepository;
    }

    public ResponseEntity<?> createReports(List<ReportDto> reportDtoList, Long labId, User user) {
        List<ReportEntity> reportEntities = new ArrayList<>();

        for (ReportDto reportDto : reportDtoList) {
            // Validate if visit exists
            Optional<VisitEntity> optionalVisit = visitRepository.findById(reportDto.getVisitId());
            if (optionalVisit.isEmpty()) {
                return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
            }
            VisitEntity visit = optionalVisit.get();
            visit.setVisitStatus("Completed");

            // Convert DTO to Entity
            ReportEntity reportEntity = new ReportEntity();
            reportEntity.setVisitId(reportDto.getVisitId());
            reportEntity.setTestName(reportDto.getTestName());
            reportEntity.setTestCategory(reportDto.getTestCategory());
            reportEntity.setPatientName(reportDto.getPatientName()); // Set patient name if required
            reportEntity.setLabId(labId); // Use the provided labId
            reportEntity.setReferenceDescription(reportDto.getReferenceDescription());
            reportEntity.setReferenceRange(reportDto.getReferenceRange());
            reportEntity.setReferenceAgeRange(reportDto.getReferenceAgeRange());
            reportEntity.setEnteredValue(reportDto.getEnteredValue());
            reportEntity.setUnit(reportDto.getUnit());
            reportEntity.setCreatedBy(user.getId());
            reportEntities.add(reportEntity);

        }
        List<ReportEntity> savedEntities = reportRepository.saveAll(reportEntities);
        savedEntities.forEach(report -> System.out.println("Saved Report ID: " + report.getReportId()));
        return ApiResponseHelper.successResponse("Reports created successfully", savedEntities);
    }

    public ResponseEntity<?> getReport(Long visitId, Long labId) {
        List<ReportEntity> reportEntities = reportRepository.findByVisitIdAndLabId(visitId, labId);
        if (reportEntities.isEmpty()) {
            return ApiResponseHelper.errorResponse("Report not found", HttpStatus.NOT_FOUND);
        }
        return ApiResponseHelper.successResponse("Report fetched successfully", reportEntities);
    }
}

