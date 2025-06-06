package tiameds.com.tiameds.controller.sampleassociation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.ReportDto;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;
import tiameds.com.tiameds.services.lab.ReportService;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/lab")
@Tag(name = "Report Generation", description = "manage the report generation in the lab")
public class ReportGeneration {

    private final UserAuthService userAuthService;
    private final ReportService reportService;
    private final LabAccessableFilter labAccessableFilter;
    private final LabRepository labRepository;

    public ReportGeneration(UserAuthService userAuthService, ReportService reportService, LabAccessableFilter labAccessableFilter, LabRepository labRepository) {
        this.userAuthService = userAuthService;
        this.reportService = reportService;
        this.labAccessableFilter = labAccessableFilter;
        this.labRepository = labRepository;
    }

    @Transactional
    @PostMapping("{labId}/report")
    public ResponseEntity<?> createReport(
            @PathVariable Long labId,
            @RequestBody List<ReportDto> reportDtoList,
            @RequestHeader("Authorization") String token) {

        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }
        // Check if the user is a member of the lab
        if (!currentUser.get().getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }
        // Ensure report list is valid
        if (reportDtoList == null || reportDtoList.isEmpty()) {
            return ApiResponseHelper.errorResponse("Report list cannot be empty", HttpStatus.BAD_REQUEST);
        }
        return reportService.createReports(reportDtoList, labId, currentUser.get());
    }

    @Transactional
    @GetMapping("{labId}/report/{visitId}")
    public ResponseEntity<?> getReport(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestHeader("Authorization") String token) {

        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        if (!currentUser.get().getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Ensure report list is valid
        if (visitId == null) {
            return ApiResponseHelper.errorResponse("Visit ID cannot be empty", HttpStatus.BAD_REQUEST);
        }
        return reportService.getReport(visitId, labId);
    }





}
