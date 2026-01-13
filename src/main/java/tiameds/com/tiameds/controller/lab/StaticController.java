package tiameds.com.tiameds.controller.lab;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.StaticDto;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.StaticServices;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/lab/statistics")
public class StaticController {

    private final StaticServices staticServices;
    private final LabAccessableFilter labAccessableFilter;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(StaticController.class);

    public StaticController(StaticServices staticServices, LabAccessableFilter labAccessableFilter, UserService userService) {
        this.staticServices = staticServices;
        this.labAccessableFilter = labAccessableFilter;
        this.userService = userService;
    }

    @GetMapping("/{labId}")
    public ResponseEntity<?> getStaticData(
            @PathVariable Long labId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // Validate token format
        Optional<User> currentUser = getAuthenticatedUser();
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            int sanitizedPage = Math.max(page, 0);
            int sanitizedSize = Math.min(Math.max(size, 10), 200);

            var dtoPage = staticServices.getTransactionDatewise(labId, startDate, endDate, currentUser.get(), sanitizedPage, sanitizedSize);

            var responseBody = new java.util.HashMap<String, Object>();
            responseBody.put("status", "success");
            responseBody.put("message", "Transaction details fetched successfully");
            responseBody.put("data", dtoPage.getContent());

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("X-Total-Elements", String.valueOf(dtoPage.getTotalElements()));
            headers.add("X-Total-Pages", String.valueOf(dtoPage.getTotalPages()));
            headers.add("X-Page-Number", String.valueOf(dtoPage.getNumber()));
            headers.add("X-Page-Size", String.valueOf(dtoPage.getSize()));

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error fetching datewise transactions for lab {}", labId, e);
            return ApiResponseHelper.successResponseWithDataAndMessage("No transactions found", HttpStatus.OK, List.of());
        }
    }

    /**
     * Get payment details by date range (past bills paid on filter date).
     * This endpoint shows ONLY past bills that were paid on the filter date.
     * 
     * Includes:
     * - Billings created BEFORE the filter date
     * - AND have transactions where transaction date falls within the filter date range
     * 
     * Excludes:
     * - Bills created on the filter date
     * - Bills without transactions
     * 
     * Results are sorted by most recent transaction date (descending).
     * 
     * Example: Filter by 30th December will show bills created before 30th December
     * that were paid on 30th December, but NOT bills created on 30th December.
     */
    @GetMapping("/{labId}/datewise-paymentdetails")
    public ResponseEntity<?> getPaymentDetailsByDateRange(
            @PathVariable Long labId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            int sanitizedPage = Math.max(page, 0);
            int sanitizedSize = Math.min(Math.max(size, 10), 200);

            var dtoPage = staticServices.getPaymentDatewise(labId, startDate, endDate, currentUser.get(), sanitizedPage, sanitizedSize);

            var responseBody = new java.util.HashMap<String, Object>();
            responseBody.put("status", "success");
            responseBody.put("message", "Payment details fetched successfully");
            responseBody.put("data", dtoPage.getContent());

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("X-Total-Elements", String.valueOf(dtoPage.getTotalElements()));
            headers.add("X-Total-Pages", String.valueOf(dtoPage.getTotalPages()));
            headers.add("X-Page-Number", String.valueOf(dtoPage.getNumber()));
            headers.add("X-Page-Size", String.valueOf(dtoPage.getSize()));

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error fetching datewise payment details for lab {}", labId, e);
            return ApiResponseHelper.successResponseWithDataAndMessage("No payment details found", HttpStatus.OK, List.of());
        }
    }


    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof MyUserDetails myUserDetails) {
            return userService.findByUsername(myUserDetails.getUsername());
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.findByUsername(userDetails.getUsername());
        }
        if (principal instanceof String username && !"anonymousUser".equalsIgnoreCase(username)) {
            return userService.findByUsername(username);
        }
        return Optional.empty();
    }
}
