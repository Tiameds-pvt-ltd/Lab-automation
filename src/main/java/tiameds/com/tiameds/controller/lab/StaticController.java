package tiameds.com.tiameds.controller.lab;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.StaticDto;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.StaticServices;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/lab/statistics")
public class StaticController {

    private final StaticServices staticServices;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;
    private static final Logger log = LoggerFactory.getLogger(StaticController.class);

    public StaticController(StaticServices staticServices, UserAuthService userAuthService, LabAccessableFilter labAccessableFilter) {
        this.staticServices = staticServices;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
    }

    @GetMapping("/{labId}")
    public ResponseEntity<?> getStaticData(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // Validate token format
        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // delegate to service
        StaticDto staticData = staticServices.getStaticData(labId, startDate, endDate);

        return ApiResponseHelper.successResponse("Static data fetched successfully", staticData);
    }





    @GetMapping("/{labId}/datewise-transactionsdetails")
    public ResponseEntity<?> getPatientVisitsByDateRange(
            @PathVariable Long labId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            List<LabStatisticsDTO> dtoList = staticServices.getTransactionDatewise(labId, startDate, endDate, currentUser.get());
            return ApiResponseHelper.successResponseWithDataAndMessage("Transaction details fetched successfully", HttpStatus.OK, dtoList);

        } catch (Exception e) {
            log.error("Error fetching datewise transactions for lab {}", labId, e);
            return ApiResponseHelper.successResponseWithDataAndMessage("No transactions found", HttpStatus.OK, List.of());
        }
    }


//    @GetMapping("/{labId}/testwise-transactionsdetails"



}
