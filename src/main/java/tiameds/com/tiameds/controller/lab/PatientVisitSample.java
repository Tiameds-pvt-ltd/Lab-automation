package tiameds.com.tiameds.controller.lab;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.PatientVisitSampleDto;
import tiameds.com.tiameds.dto.lab.VisitSampleDto;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.SampleEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.SampleAssocationRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;
import tiameds.com.tiameds.repository.LabRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/lab")
@Tag(name = "patient visit sample", description = "manage the patient visit sample in the lab")
public class PatientVisitSample {
    private final VisitRepository visitRepository;
    private final SampleAssocationRepository sampleAssocationRepository;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;
    private final LabRepository labRepository;

    public PatientVisitSample(VisitRepository visitRepository, SampleAssocationRepository sampleAssocationRepository, UserAuthService userAuthService, LabAccessableFilter labAccessableFilter, LabRepository labRepository) {
        this.visitRepository = visitRepository;
        this.sampleAssocationRepository = sampleAssocationRepository;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
        this.labRepository = labRepository;
    }
    @PostMapping("/add-samples")
    @Transactional
    public ResponseEntity<?> createSampleWithPatientVisit(@RequestBody PatientVisitSampleDto request,
                                                          @RequestHeader("Authorization") String token) {
        // Authenticate user
        if (userAuthService.authenticateUser(token).isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        // 1. Find the visit by ID
        VisitEntity visit = visitRepository.findById(request.getVisitId()).orElse(null);
        if (visit == null) {
            return ResponseEntity.badRequest().body("Visit not found");
        }

        // 2. Fetch or create samples
        Set<SampleEntity> samples = new HashSet<>();
        for (String sampleName : request.getSampleNames()) {
            SampleEntity sample = sampleAssocationRepository.findByName(sampleName)
                    .orElseGet(() -> {
                        SampleEntity newSample = new SampleEntity();
                        newSample.setName(sampleName);
                        return sampleAssocationRepository.save(newSample); // Save new sample
                    });

            samples.add(sample);
        }

        // 3. Associate samples with visit correctly
        visit.getSamples().addAll(samples);
        visitRepository.save(visit);

        //update the visit status
        visit.setVisitStatus("Collected");
        visitRepository.save(visit);
        return ResponseEntity.ok("Samples added to visit successfully");
//        return ApiResponseHelper.successResponse("Samples added to visit successfully", visit);
    }

    @PutMapping("/update-samples")
    @Transactional
    public ResponseEntity<?> updateSampleWithPatientVisit(@RequestBody PatientVisitSampleDto request,
                                                          @RequestHeader("Authorization") String token) {
        // Authenticate user
        if (userAuthService.authenticateUser(token).isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // 1. Find the visit by ID
        VisitEntity visit = visitRepository.findById(request.getVisitId()).orElse(null);
        if (visit == null) {
            return ResponseEntity.badRequest().body("Visit not found");
        }

        // 2. update the samples
        Set<SampleEntity> samples = new HashSet<>();
        for (String sampleName : request.getSampleNames()) {
            SampleEntity sample = sampleAssocationRepository.findByName(sampleName)
                    .orElseGet(() -> {
                        SampleEntity newSample = new SampleEntity();
                        newSample.setName(sampleName);
                        return sampleAssocationRepository.save(newSample); // Save new sample
                    });

            samples.add(sample);
        }

        // 3. Associate samples with visit correctly
        visit.setSamples(samples);
        visitRepository.save(visit);
        return ResponseEntity.ok("Samples updated to visit successfully");
    }

    @DeleteMapping("/delete-samples")
    @Transactional
    public ResponseEntity<?> deleteSampleWithPatientVisit(@RequestBody PatientVisitSampleDto request,
                                                          @RequestHeader("Authorization") String token) {
        // Authenticate user
        if (userAuthService.authenticateUser(token).isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        // 1. Find the visit by ID
        VisitEntity visit = visitRepository.findById(request.getVisitId()).orElse(null);
        if (visit == null) {
            return ResponseEntity.badRequest().body("Visit not found");
        }
        // 2. Fetch or create samples
        Set<SampleEntity> samplesToRemove = new HashSet<>();
        for (String sampleName : request.getSampleNames()) {
            SampleEntity sample = sampleAssocationRepository.findByName(sampleName)
                    .orElseGet(() -> {
                        SampleEntity newSample = new SampleEntity();
                        newSample.setName(sampleName);
                        return sampleAssocationRepository.save(newSample); // Save new sample
                    });
            samplesToRemove.add(sample);
        }
        // 3. Remove samples from the visit
        visit.getSamples().removeAll(samplesToRemove);
        visitRepository.save(visit);

        //check if the visit has no samples
        if (visit.getSamples().isEmpty()) {
            visit.setVisitStatus("Pending");
            visitRepository.save(visit);  // Save the updated status
        }
        return ResponseEntity.ok("Samples deleted from visit successfully");
    }

    @GetMapping("/{labId}/get-visit-samples")
    @Transactional
    public ResponseEntity<?> getVisitSamplesByDateRange(
            @PathVariable("labId") Long labId,
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String visitStatus) {

        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }
        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null) {
            return ResponseEntity.badRequest().body("Lab not found");
        }
        if (startDate == null || endDate == null) {
            return ApiResponseHelper.errorResponse("Start date and end date are required", HttpStatus.BAD_REQUEST);
        }
        List<VisitEntity> visits;
        if (visitStatus != null && !visitStatus.isBlank()) {
            visits = visitRepository.findAllByPatient_LabsAndVisitDateBetweenAndVisitStatus(
                    lab, startDate, endDate, visitStatus
            );
        } else {
            visits = visitRepository.findAllByPatient_LabsAndVisitDateBetween(
                    lab, startDate, endDate
            );
        }
        List<VisitSampleDto> visitSamples = visits.stream()
                .map(visit -> new VisitSampleDto(
                        visit.getVisitId(),
                        visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName(),
                        visit.getPatient().getGender(),
                        visit.getPatient().getDateOfBirth().toString(),
                        visit.getPatient().getPhone(),
                        visit.getPatient().getEmail(),
                        visit.getVisitDate(),
                        visit.getVisitStatus(),
                        //get gender using visit.getPatient().getGender()
                        visit.getPatient().getGender(),
                        visit.getSamples().stream()
                                .map(SampleEntity::getName)
                                .collect(Collectors.toSet()),
                        visit.getTests().stream()
                                .map(test -> test.getId())
                                .collect(Collectors.toList()),
                        visit.getPackages().stream()
                                .map(pkg -> pkg.getId())
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
        return ApiResponseHelper.successResponse("Visits filtered by date and status", visitSamples);
    }
}
