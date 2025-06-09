package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.HealthPackageRequest;
import tiameds.com.tiameds.entity.HealthPackage;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.HealthPackageRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/admin/lab")
@Tag(name = "Package", description = "Package API which is used to manage packages")
public class HealthPackageController {

    private final LabRepository labRepository;
    private final TestRepository testRepository;
    private final UserAuthService userAuthService;
    private final HealthPackageRepository healthPackageRepository;
    private final LabAccessableFilter labAccessableFilter;

    //default constructor
    public HealthPackageController(LabRepository labRepository, TestRepository testRepository, UserAuthService userAuthService, HealthPackageRepository healthPackageRepository, LabAccessableFilter labAccessableFilter) {
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.userAuthService = userAuthService;
        this.healthPackageRepository = healthPackageRepository;
        this.labAccessableFilter = labAccessableFilter;
    }


    //get all packages of a respective lab by their lab id  and only members of the lab can access this
    @Transactional
    @GetMapping("{labId}/packages")
    public ResponseEntity<?> getHealthPackages(
            @PathVariable("labId") Long labId,
            @RequestHeader("Authorization") String token) {

        // Authenticate the user
        User currentUser = userAuthService.authenticateUser(token)
                .orElseThrow(() -> new RuntimeException("User not found"));


        // Fetch the lab and check if it exists
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user member of the lab or not
        if (!currentUser.getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health packages of the lab
        List<HealthPackage> healthPackages = healthPackageRepository.findAllByLabs(lab.get());

        // Return the success response with the fetched health packages
        return ApiResponseHelper.successResponse(
                "Health packages fetched successfully",
                healthPackages
        );
    }


    @Transactional
    @PostMapping("{labId}/package")
    public ResponseEntity<?> createHealthPackage(
            @PathVariable("labId") Long labId,
            @RequestBody HealthPackageRequest packageRequest, // Assuming a DTO is used to accept the data
            @RequestHeader("Authorization") String token) {

        // Authenticate the user
        User currentUser = userAuthService.authenticateUser(token)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch the lab and check if it exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        Lab lab = labOptional.get();

        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Check if the test IDs belong to the lab or not
        List<Test> tests = testRepository.findAllById(packageRequest.getTestIds());
        if (tests.size() != packageRequest.getTestIds().size()) {
            return ApiResponseHelper.errorResponse("One or more test IDs do not exist", HttpStatus.BAD_REQUEST);
        }

        // Create a new health package
        HealthPackage healthPackage = new HealthPackage();
        healthPackage.setPackageName(packageRequest.getPackageName());
        healthPackage.setPrice(packageRequest.getPrice());
        healthPackage.setDiscount(packageRequest.getDiscount());
        healthPackage.setTests(new HashSet<>(tests)); // Convert List to Set before adding tests

        // Establish bidirectional relationship
        healthPackage.getLabs().add(lab); // Add lab to healthPackage
        lab.getHealthPackages().add(healthPackage); // Add healthPackage to lab

        // Save the health package to the database
        healthPackageRepository.save(healthPackage);
        labRepository.save(lab); // Ensure the lab entity updates the relationship

        // Return the success response with the created health package
        return ApiResponseHelper.successResponse(
                "Health package created successfully",
                healthPackage
        );
    }

    //get package by id
    @Transactional
    @GetMapping("{labId}/package/{packageId}")
    public ResponseEntity<?> getHealthPackage(
            @PathVariable("labId") Long labId,
            @PathVariable("packageId") Long packageId,
            @RequestHeader("Authorization") String token
    ) {
        // Authenticate the user
        User currentUser = userAuthService.authenticateUser(token)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch the lab and check if it exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));


        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health package based on the provided package ID
        var healthPackageOptional = healthPackageRepository.findById(packageId);

        // Check if the health package exists
        if (healthPackageOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        HealthPackage healthPackage = healthPackageOptional.get();

        // Check if the health package is associated with the lab
        if (!healthPackage.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        // Return the success response with the fetched health package
        return ApiResponseHelper.successResponse(
                "Health package fetched successfully",
                healthPackage
        );
    }


    //update package
    @Transactional
    @PutMapping("{labId}/package/{packageId}")
    public ResponseEntity<?> updateHealthPackage(
            @PathVariable("labId") Long labId,
            @PathVariable("packageId") Long packageId,
            @RequestBody HealthPackageRequest packageRequest, // Assuming a DTO is used to accept the data
            @RequestHeader("Authorization") String token
    ) {
        // Authenticate the user
        User currentUser = userAuthService.authenticateUser(token)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch the lab and check if it exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));


        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health package based on the provided package ID
        var healthPackageOptional = healthPackageRepository.findById(packageId);

        // Check if the health package exists
        if (healthPackageOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        HealthPackage healthPackage = healthPackageOptional.get();

        // Check if the health package is associated with the lab
        if (!healthPackage.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        // Fetch the health tests based on the provided test IDs
        List<Test> tests = testRepository.findAllById(packageRequest.getTestIds());  // Fetching tests by their IDs

        // Check if all test IDs exist in the database
        if (tests.size() != packageRequest.getTestIds().size()) {
            return ApiResponseHelper.errorResponse("One or more test IDs do not exist", HttpStatus.BAD_REQUEST);
        }

        // Update the health package
        healthPackage.setPackageName(packageRequest.getPackageName());
        healthPackage.setPrice(packageRequest.getPrice());
        healthPackage.setDiscount(packageRequest.getDiscount());
        healthPackage.setTests(new HashSet<>(tests));  // Convert List to Set before adding tests

        // Save the updated health package to the database
        healthPackageRepository.save(healthPackage);

        // Return the success response with the updated health package
        return ApiResponseHelper.successResponse(
                "Health package updated successfully",
                healthPackage
        );
    }


    //delete package by their respective id
    @Transactional
    @DeleteMapping("{labId}/package/{packageId}")
    public ResponseEntity<?> deleteHealthPackage(
            @PathVariable("labId") Long labId,
            @PathVariable("packageId") Long packageId,
            @RequestHeader("Authorization") String token
    ) {
        // Authenticate the user
        User currentUser = userAuthService.authenticateUser(token)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch the lab and check if it exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        // Check if the lab is active
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (isAccessible == false) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Verify if the current user is associated with the lab
        if (!currentUser.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Fetch the health package based on the provided package ID
        var healthPackageOptional = healthPackageRepository.findById(packageId);

        // Check if the health package exists
        if (healthPackageOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Health package not found", HttpStatus.NOT_FOUND);
        }

        HealthPackage healthPackage = healthPackageOptional.get();

        // Check if the health package is associated with the lab
        if (!healthPackage.getLabs().contains(lab)) {
            return ApiResponseHelper.errorResponse("Health package not associated with this lab", HttpStatus.NOT_FOUND);
        }

        // Remove the association of the health package with the lab
        lab.getHealthPackages().remove(healthPackage);

        // Save the updated lab to ensure the association is removed
        labRepository.save(lab);

        // Delete the health package from the database
        healthPackageRepository.delete(healthPackage);

        // Return the success response
        return ApiResponseHelper.successResponse(
                "Health package deleted successfully",
                null
        );
    }


}
