package tiameds.com.tiameds.controller.superAdmin;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.SuperAdminReferanceEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.AdminTestReferanceandTestServices;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.UserAuthService;


import java.util.Optional;


@RestController
@RequestMapping("/super-admin/referance-and-test")
public class ReferanceAndTestController {
    private final UserAuthService userAuthService;
    private final AdminTestReferanceandTestServices adminTestReferanceandTestServices;

    public ReferanceAndTestController(UserAuthService userAuthService, AdminTestReferanceandTestServices adminTestReferanceandTestServices) {
        this.userAuthService = userAuthService;
        this.adminTestReferanceandTestServices = adminTestReferanceandTestServices;
    }

    @PostMapping(value = "/upload-testcsv",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadReferance(
            SuperAdminReferanceEntity superAdminReferanceEntity,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token
    ) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }
        User currentUser = userOptional.get();
        if (file.isEmpty() || !"text/csv".equals(file.getContentType())) {
            return ApiResponseHelper.errorResponse("Please upload a valid CSV file", HttpStatus.BAD_REQUEST);
        }
        // Call the service to handle the file upload and save the reference
        try {
            adminTestReferanceandTestServices.uploadTestDataPriceList(superAdminReferanceEntity, file, currentUser);
            return ApiResponseHelper.successResponseWithData("Referance and Test data uploaded successfully");
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("Error uploading file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @GetMapping("/testpricelist/download")
    public ResponseEntity<?> downloadPriceListCSV(
            @RequestHeader("Authorization") String token) {
        try {
            Optional<User> userOptional = userAuthService.authenticateUser(token);
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }
            return  adminTestReferanceandTestServices.downloadPriceListCSV();
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // upload test-referance
    @PostMapping(value = "/upload-test-referance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadTestReferance(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token
    ) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }
        User currentUser = userOptional.get();
        if (file.isEmpty() || !"text/csv".equals(file.getContentType())) {
            return ApiResponseHelper.errorResponse("Please upload a valid CSV file", HttpStatus.BAD_REQUEST);
        }
        // Call the service to handle the file upload and save the reference
        try {
            adminTestReferanceandTestServices.uploadTestReferance(file, currentUser);
            return ApiResponseHelper.successResponseWithData("Test referance uploaded successfully");
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("Error uploading file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/test-referance/download")
    public ResponseEntity<?> downloadTestReferanceCSV(
            @RequestHeader("Authorization") String token) {
        try {
            Optional<User> userOptional = userAuthService.authenticateUser(token);
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }
            return adminTestReferanceandTestServices.downloadTestReferanceCSV();
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // get all test price list
    @GetMapping("/test-price-list")
    public ResponseEntity<?> getAllTestPriceList(
            @RequestHeader("Authorization") String token) {
        try {
            Optional<User> userOptional = userAuthService.authenticateUser(token);
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }
            return adminTestReferanceandTestServices.getAllTestPriceList();
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // get all test referance
    @GetMapping("/test-referance")
    public ResponseEntity<?> getAllTestReferance(
            @RequestHeader("Authorization") String token) {
        try {
            Optional<User> userOptional = userAuthService.authenticateUser(token);
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }
            return adminTestReferanceandTestServices.getAllTestReferance();
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
