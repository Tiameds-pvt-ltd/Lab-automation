package tiameds.com.tiameds.services.lab;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
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

        // Ensure report list is valid
//        if (reportDtoList == null || reportDtoList.isEmpty()) {
//            return ApiResponseHelper.errorResponse("Report list cannot be empty", HttpStatus.BAD_REQUEST);
//        }

        // Ensure report list is valid
        if (reportDtoList == null || reportDtoList.isEmpty()) {
            // if reportDtoList is empty,which means no reports to create, that is hard copy given to patient by some other machine
            VisitEntity visitEntity = new VisitEntity();
            visitEntity.setVisitStatus("Completed");
            return ApiResponseHelper.successResponse(
                    "No digital reports submitted. Visit marked as completed—reports may have been provided externally in hard copy.",
                    visitEntity
            );

        }

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


    public ResponseEntity<?> updateReports(List<ReportDto> reportDtoList, User user) {
        List<ReportEntity> updatedReports = new ArrayList<>();

        for (ReportDto reportDto : reportDtoList) {
            if (reportDto.getReportId() == null) {
                return ApiResponseHelper.errorResponse("Report ID is required for update", HttpStatus.BAD_REQUEST);
            }

            Optional<ReportEntity> optionalReport = reportRepository.findById(reportDto.getReportId());
            if (optionalReport.isEmpty()) {
                return ApiResponseHelper.errorResponse("Report not found with ID: " + reportDto.getReportId(), HttpStatus.NOT_FOUND);
            }

            ReportEntity reportEntity = optionalReport.get();

            // Update fields
            reportEntity.setTestName(reportDto.getTestName());
            reportEntity.setTestCategory(reportDto.getTestCategory());
            reportEntity.setPatientName(reportDto.getPatientName());
            reportEntity.setReferenceDescription(reportDto.getReferenceDescription());
            reportEntity.setReferenceRange(reportDto.getReferenceRange());
            reportEntity.setReferenceAgeRange(reportDto.getReferenceAgeRange());
            reportEntity.setEnteredValue(reportDto.getEnteredValue());
            reportEntity.setUnit(reportDto.getUnit());
            reportEntity.setCreatedBy(user.getId());

            updatedReports.add(reportEntity);
        }

        List<ReportEntity> savedReports = reportRepository.saveAll(updatedReports);
        return ApiResponseHelper.successResponse("Reports updated successfully", savedReports);
    }


    @Transactional
    public ResponseEntity<?> completeVisit(Long visitId) {
        Optional<VisitEntity> optionalVisit = visitRepository.findById(visitId);
        if (optionalVisit.isEmpty()) {
            return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }

        VisitEntity visit = optionalVisit.get();
        if ("Completed".equalsIgnoreCase(visit.getVisitStatus())) {
            return ApiResponseHelper.errorResponse("Visit is already completed", HttpStatus.BAD_REQUEST);
        }

        // Perform direct DB update
        visitRepository.updateVisitStatus(visitId, "Completed");

        return ApiResponseHelper.successResponse("Visit completed successfully", HttpStatus.CREATED);
    }


    public ResponseEntity<?> canceledVisit(Long visitId) {
        Optional<VisitEntity> optionalVisit = visitRepository.findById(visitId);
        if (optionalVisit.isEmpty()) {
            return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }

        VisitEntity visit = optionalVisit.get();
        if ("Completed".equalsIgnoreCase(visit.getVisitStatus())) {
            return ApiResponseHelper.errorResponse("Visit is already canceled", HttpStatus.BAD_REQUEST);
        }

        // Perform direct DB update
        visitRepository.updateVisitStatus(visitId, "Canceled");

        return ApiResponseHelper.successResponse("Visit Canceled successfully", HttpStatus.CREATED);
    }
}

