package tiameds.com.tiameds.services.lab;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.ReportDto;
import tiameds.com.tiameds.dto.lab.TestResultDto;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.ReportEntity;
import tiameds.com.tiameds.entity.TestRow;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.entity.VisitTestResult;
import tiameds.com.tiameds.repository.*;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final VisitRepository visitRepository;
    private final TestRepository testRepository;
    private final LabRepository labRepository;
    private final VisitTestResultRepository visitTestResultRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public ReportService(ReportRepository reportRepository, VisitRepository visitRepository, TestRepository testRepository, LabRepository labRepository, VisitTestResultRepository visitTestResultRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.reportRepository = reportRepository;
        this.visitRepository = visitRepository;
        this.testRepository = testRepository;
        this.labRepository = labRepository;
        this.visitTestResultRepository = visitTestResultRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

//    public ResponseEntity<?> createReports(List<ReportDto> reportDtoList, Long labId, User user) {
//        List<ReportEntity> reportEntities = new ArrayList<>();
//
//        // Ensure report list is valid
//        if (reportDtoList == null || reportDtoList.isEmpty()) {
//            // if reportDtoList is empty,which means no reports to create, that is hard copy given to patient by some other machine
//            VisitEntity visitEntity = new VisitEntity();
//            visitEntity.setVisitStatus("Completed");
//            return ApiResponseHelper.successResponse(
//                    "No digital reports submitted. Visit marked as completed—reports may have been provided externally in hard copy.",
//                    visitEntity
//            );
//
//        }
//
//        for (ReportDto reportDto : reportDtoList) {
//            // Validate if visit exists
//            Optional<VisitEntity> optionalVisit = visitRepository.findById(reportDto.getVisitId());
//            if (optionalVisit.isEmpty()) {
//                return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
//            }
//            VisitEntity visit = optionalVisit.get();
//            visit.setVisitStatus("Completed");
//
//            // Convert DTO to Entity
//            ReportEntity reportEntity = new ReportEntity();
//            reportEntity.setVisitId(reportDto.getVisitId());
//            reportEntity.setTestName(reportDto.getTestName());
//            reportEntity.setTestCategory(reportDto.getTestCategory());
//            reportEntity.setPatientName(reportDto.getPatientName()); // Set patient name if required
//            reportEntity.setLabId(labId); // Use the provided labId
//            reportEntity.setReferenceDescription(reportDto.getReferenceDescription());
//            reportEntity.setReferenceRange(reportDto.getReferenceRange());
//            reportEntity.setReferenceAgeRange(reportDto.getReferenceAgeRange());
//            reportEntity.setEnteredValue(reportDto.getEnteredValue());
//            reportEntity.setUnit(reportDto.getUnit());
//            reportEntity.setCreatedBy(user.getId());
//            reportEntities.add(reportEntity);
//
//        }
//        List<ReportEntity> savedEntities = reportRepository.saveAll(reportEntities);
//        savedEntities.forEach(report -> System.out.println("Saved Report ID: " + report.getReportId()));
//        return ApiResponseHelper.successResponse("Reports created successfully", savedEntities);
//    }

    public ResponseEntity<?> getReport(Long visitId, Long labId) {
        List<ReportEntity> reportEntities = reportRepository.findByVisitIdAndLabId(visitId, labId);
        if (reportEntities.isEmpty()) {
            return ApiResponseHelper.errorResponse("Report not found", HttpStatus.NOT_FOUND);
        }

        String patientCode = null;
        String visitCode = null;
        Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isPresent()) {
            VisitEntity visit = visitOptional.get();
            visitCode = visit.getVisitCode();
            if (visit.getPatient() != null) {
                patientCode = visit.getPatient().getPatientCode();
            }
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        final String finalPatientCode = patientCode;
        final String finalVisitCode = visitCode;
        reportEntities.forEach(report -> {
            report.setTestRows(buildTestRows(report));
            report.setPatientCode(finalPatientCode);
            report.setVisitCode(finalVisitCode);
            if (report.getCreatedAt() != null) {
                report.setCreatedDateTime(report.getCreatedAt().format(dateTimeFormatter));
            } else {
                report.setCreatedDateTime(null);
            }
        });

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
            // JSON fields
            reportEntity.setReportJson(reportDto.getReportJson());
            reportEntity.setReferenceRanges(reportDto.getReferenceRanges());
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



    public ResponseEntity<?> createReports(List<ReportDto> reportDtoList, Long labId, User user, TestResultDto testResultDto) {

        // Validate testResultDto
        if (testResultDto == null || testResultDto.getTestId() == null || testResultDto.getIsFilled() == null) {
            return ApiResponseHelper.errorResponse("Test result cannot be null or missing required fields", HttpStatus.BAD_REQUEST);
        }

        // Handle empty report list case
        if (reportDtoList == null || reportDtoList.isEmpty()) {
            Optional<VisitTestResult> optionalVisitTestResult = visitTestResultRepository.findByVisitIdAndTestId(null, testResultDto.getTestId()); // you might need to pass visitId separately in this case

            if (optionalVisitTestResult.isPresent()) {
                VisitTestResult existingVisitTestResult = optionalVisitTestResult.get();
                existingVisitTestResult.setIsFilled(testResultDto.getIsFilled());
                existingVisitTestResult.setUpdatedBy(String.valueOf(user.getUsername()));
                visitTestResultRepository.save(existingVisitTestResult);
            } else {
                return ApiResponseHelper.errorResponse("Visit Test Result not found for the given visit and test ID", HttpStatus.NOT_FOUND);
            }

            return ApiResponseHelper.successResponse("No digital reports submitted. Visit marked as completed", testResultDto);
        }

        // Update VisitTestResult
        Optional<VisitTestResult> optionalVisitTestResult = visitTestResultRepository.findByVisitIdAndTestId(reportDtoList.get(0).getVisitId(), testResultDto.getTestId());

        if (optionalVisitTestResult.isPresent()) {
            VisitTestResult existingVisitTestResult = optionalVisitTestResult.get();
            existingVisitTestResult.setIsFilled(testResultDto.getIsFilled());
            existingVisitTestResult.setUpdatedBy(String.valueOf(user.getId()));
            existingVisitTestResult.setReportStatus("Completed");
            visitTestResultRepository.save(existingVisitTestResult);
        } else {
            return ApiResponseHelper.errorResponse("Visit Test Result not found for the given visit and test ID", HttpStatus.NOT_FOUND);
        }

        ReportDto firstReport = reportDtoList.get(0);

        Optional<VisitEntity> optionalVisit = visitRepository.findById(firstReport.getVisitId());
        if (optionalVisit.isEmpty()) {
            return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }

        VisitEntity visit = optionalVisit.get();
        visit.setVisitStatus("Completed");
        visitRepository.save(visit);

        ReportEntity reportEntity = new ReportEntity();

        // Generate unique report code using sequence generator
        String reportCode = sequenceGeneratorService.generateCode(labId, EntityType.REPORT);
        reportEntity.setReportCode(reportCode);

        reportEntity.setVisitId(firstReport.getVisitId());
        reportEntity.setTestName(firstReport.getTestName());
        reportEntity.setTestCategory(firstReport.getTestCategory());
        reportEntity.setPatientName(firstReport.getPatientName());
        reportEntity.setLabId(labId);
        reportEntity.setReferenceDescription(firstReport.getReferenceDescription());
        reportEntity.setReferenceRange(firstReport.getReferenceRange());
        reportEntity.setReferenceAgeRange(firstReport.getReferenceAgeRange());
        reportEntity.setEnteredValue(firstReport.getEnteredValue());
        reportEntity.setDescription(firstReport.getDescription());
        reportEntity.setRemarks(firstReport.getRemarks());
        reportEntity.setComments(firstReport.getComments());
        reportEntity.setUnit(firstReport.getUnit());
        reportEntity.setReportJson(firstReport.getReportJson());
        reportEntity.setReferenceRanges(firstReport.getReferenceRanges());
        reportEntity.setCreatedBy(user.getId());

        List<TestRow> testRows = new ArrayList<>();
        for (ReportDto reportDto : reportDtoList) {
            TestRow testRow = new TestRow(
                    resolveTestParameter(reportDto),
                    reportDto.getReferenceRange(),
                    reportDto.getEnteredValue(),
                    reportDto.getUnit(),
                    reportDto.getReferenceAgeRange()
            );
            testRows.add(testRow);
        }
        reportEntity.setTestRows(testRows);

        ReportEntity savedEntity = reportRepository.save(reportEntity);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("report", savedEntity);
        responsePayload.put("testResult", testResultDto);

        return ApiResponseHelper.successResponse("Reports created successfully", responsePayload);
    }


//    public List<TestRow> buildTestRows(ReportEntity report) {
//        List<?> storedRows = report.getTestRows();
//        if (storedRows != null && !storedRows.isEmpty()) {
//            List<TestRow> normalizedRows = new ArrayList<>();
//            for (Object rowObj : storedRows) {
//                if (rowObj instanceof TestRow row) {
//                    normalizedRows.add(fillMissingRowValues(row, report));
//                } else if (rowObj instanceof Map<?, ?> rowMap) {
//                    normalizedRows.add(convertLegacyRow(rowMap, report));
//                }
//            }
//            if (!normalizedRows.isEmpty()) {
//                return normalizedRows;
//            }
//        }
//
//        return List.of(buildFallbackRow(report));
//    }

    public List<TestRow> buildTestRows(ReportEntity report) {

        List<?> storedRows = report.getTestRows();

        // If no testRows present (older data), return null
        if (storedRows == null || storedRows.isEmpty()) {
            return null;   // ← THIS FIXES YOUR ISSUE
        }

        // If testRows exist, normalize them
        List<TestRow> normalizedRows = new ArrayList<>();

        for (Object rowObj : storedRows) {
            if (rowObj instanceof TestRow row) {
                normalizedRows.add(fillMissingRowValues(row, report));
            } else if (rowObj instanceof Map<?, ?> rowMap) {
                normalizedRows.add(convertLegacyRow(rowMap, report));
            }
        }

        return normalizedRows.isEmpty() ? null : normalizedRows;
    }


    private TestRow convertLegacyRow(Map<?, ?> rowMap, ReportEntity report) {
        String parameter = stringValue(rowMap, "testParameter",
                stringValue(rowMap, "referenceDescription", defaultTestParameter(report)));
        String normalRange = stringValue(rowMap, "normalRange",
                stringValue(rowMap, "referenceRange", report.getReferenceRange()));
        String enteredValue = stringValue(rowMap, "enteredValue", report.getEnteredValue());
        String unit = stringValue(rowMap, "unit", report.getUnit());
        String ageRange = stringValue(rowMap, "referenceAgeRange", report.getReferenceAgeRange());

        return new TestRow(parameter, normalRange, enteredValue, unit, ageRange);
    }

    private TestRow fillMissingRowValues(TestRow row, ReportEntity report) {
        if (row == null) {
            return buildFallbackRow(report);
        }
        if (isBlank(row.getTestParameter())) {
            row.setTestParameter(defaultTestParameter(report));
        }
        if (isBlank(row.getNormalRange())) {
            row.setNormalRange(report.getReferenceRange());
        }
        if (isBlank(row.getEnteredValue())) {
            row.setEnteredValue(report.getEnteredValue());
        }
        if (isBlank(row.getUnit())) {
            row.setUnit(report.getUnit());
        }
        if (isBlank(row.getReferenceAgeRange())) {
            row.setReferenceAgeRange(report.getReferenceAgeRange());
        }
        return row;
    }

    private TestRow buildFallbackRow(ReportEntity report) {
        return new TestRow(
                defaultTestParameter(report),
                report.getReferenceRange(),
                report.getEnteredValue(),
                report.getUnit(),
                report.getReferenceAgeRange()
        );
    }

    private String resolveTestParameter(ReportDto reportDto) {
        if (reportDto == null) {
            return null;
        }
        if (!isBlank(reportDto.getReportParameter())) {
            return reportDto.getReportParameter().trim();
        }
        if (!isBlank(reportDto.getReferenceDescription())) {
            return reportDto.getReferenceDescription().trim();
        }
        return reportDto.getTestName();
    }

    private String defaultTestParameter(ReportEntity report) {
        if (report == null) {
            return null;
        }
        if (!isBlank(report.getReferenceDescription())) {
            return report.getReferenceDescription();
        }
        return report.getTestName();
    }

    private String stringValue(Map<?, ?> map, String primaryKey, String defaultValue) {
        Object value = map.get(primaryKey);
        if (value == null && primaryKey != null) {
            // attempt secondary lookup already handled by caller
        }
        return value instanceof String str && !str.isBlank() ? str : defaultValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}

