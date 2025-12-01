package tiameds.com.tiameds.services.lab;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tiameds.com.tiameds.dto.lab.*;
import tiameds.com.tiameds.dto.visits.PatientVisitDTO;
import tiameds.com.tiameds.dto.visits.VisitDetailsDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.*;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class VisitService {
    private final PatientRepository patientRepository;
    private final LabRepository labRepository;
    private final TestRepository testRepository;
    private final HealthPackageRepository healthPackageRepository;
    private final DoctorRepository doctorRepository;
    private final InsuranceRepository insuranceRepository;
    private final BillingRepository billingRepository;
    private final VisitRepository visitRepository;
    private final TestDiscountRepository testDiscountRepository;
    private final VisitTestResultRepository visitTestResultRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public VisitService(PatientRepository patientRepository,
                        LabRepository labRepository,
                        TestRepository testRepository,
                        HealthPackageRepository healthPackageRepository,
                        DoctorRepository doctorRepository,
                        InsuranceRepository insuranceRepository,
                        BillingRepository billingRepository,
                        VisitRepository visitRepository, 
                        TestDiscountRepository testDiscountRepository,
                        VisitTestResultRepository visitTestResultRepository,
                        SequenceGeneratorService sequenceGeneratorService) {
        this.patientRepository = patientRepository;
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.doctorRepository = doctorRepository;
        this.insuranceRepository = insuranceRepository;
        this.billingRepository = billingRepository;
        this.visitRepository = visitRepository;
        this.testDiscountRepository = testDiscountRepository;
        this.visitTestResultRepository = visitTestResultRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @Transactional
    public void addVisit(Long labId, Long patientId, VisitDTO visitDTO, Optional<User> currentUser) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            ApiResponseHelper.successResponseWithDataAndMessage("Lab not found", HttpStatus.NOT_FOUND, null);
        }
        if (!currentUser.get().getLabs().contains(labOptional.get())) {
            ApiResponseHelper.successResponseWithDataAndMessage("User is not a member of this lab", HttpStatus.UNAUTHORIZED, null);
        }
        // Check if the patient belongs to the lab
        Optional<PatientEntity> patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().contains(labOptional.get()));
        if (patientEntity.isEmpty()) {
            ApiResponseHelper.successResponseWithDataAndMessage("Patient not belong to the lab", HttpStatus.BAD_REQUEST, null);
        }
        // Check if the doctor exists
        Optional<Doctors> doctorOptional = doctorRepository.findById(visitDTO.getDoctorId());
        if (doctorOptional.isEmpty()) {
            ApiResponseHelper.errorResponse("Doctor not found", HttpStatus.NOT_FOUND);
        }
        VisitEntity visit = new VisitEntity();
        
        // Generate unique visit code using sequence generator
        String visitCode = sequenceGeneratorService.generateCode(labOptional.get().getId(), EntityType.VISIT);
        visit.setVisitCode(visitCode);
        
        visit.setPatient(patientEntity.get());
        visit.setVisitDate(visitDTO.getVisitDate());
        visit.setVisitType(visitDTO.getVisitType());
        visit.setVisitStatus(visitDTO.getVisitStatus());
        visit.setVisitDescription(visitDTO.getVisitDescription());
        visit.setDoctor(doctorOptional.get());
        visit.getLabs().add(labOptional.get());
        // Set tests
        Set<Test> tests = testRepository.findAllById(visitDTO.getTestIds()).stream().collect(Collectors.toSet());
        visit.setTests(tests);
        // Set health packages
        Set<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds()).stream().collect(Collectors.toSet());
        visit.setPackages(healthPackages);
        // Set insurances
        List<InsuranceEntity> insurances = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
        if (insurances.stream().anyMatch(insurance -> !insurance.getLabs().contains(labOptional.get()))) {
            ApiResponseHelper.errorResponse("Insurance not belong to the lab", HttpStatus.BAD_REQUEST);
        }
        visit.setInsurance(new HashSet<>(insurances));

        // Handle billing information
        BillingEntity billingEntity = new BillingEntity();
        
        // Generate unique billing code using sequence generator
        String billingCode = sequenceGeneratorService.generateCode(labOptional.get().getId(), EntityType.BILLING);
        billingEntity.setBillingCode(billingCode);
        
        billingEntity.setTotalAmount(visitDTO.getBilling().getTotalAmount());
        billingEntity.setPaymentStatus(visitDTO.getBilling().getPaymentStatus());
        billingEntity.setPaymentMethod(visitDTO.getBilling().getPaymentMethod());
        billingEntity.setPaymentDate(visitDTO.getBilling().getPaymentDate());
        billingEntity.setDiscount(visitDTO.getBilling().getDiscount());
//        billingEntity.setGstRate(visitDTO.getBilling().getGstRate());
//        billingEntity.setGstAmount(visitDTO.getBilling().getGstAmount());
//        billingEntity.setCgstAmount(visitDTO.getBilling().getCgstAmount());
//        billingEntity.setSgstAmount(visitDTO.getBilling().getSgstAmount());
//        billingEntity.setIgstAmount(visitDTO.getBilling().getIgstAmount());
        billingEntity.setNetAmount(visitDTO.getBilling().getNetAmount());
        billingRepository.save(billingEntity);
        visit.setBilling(billingEntity);

        // Save the visit
        visitRepository.save(visit);
    }

    public List<PatientDTO> getVisits(Long labId, Optional<User> currentUser) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a member of this lab");
        }
        List<VisitEntity> visits = visitRepository.findAllByPatient_Labs(labOptional.get());
        return visits.stream()
                .map(this::mapVisitToPatientDTO)
                .collect(Collectors.toList());
    }

    private PatientDTO mapVisitToPatientDTO(VisitEntity visitEntity) {
        // Map patient details
        PatientDTO patientDTO = new PatientDTO();
        patientDTO.setId(visitEntity.getPatient().getPatientId());
        patientDTO.setFirstName(visitEntity.getPatient().getFirstName());
//        patientDTO.setLastName(visitEntity.getPatient().getLastName());
//        patientDTO.setEmail(visitEntity.getPatient().getEmail());
        patientDTO.setPhone(visitEntity.getPatient().getPhone());
//        patientDTO.setAddress(visitEntity.getPatient().getAddress());
        patientDTO.setCity(visitEntity.getPatient().getCity());
//        patientDTO.setState(visitEntity.getPatient().getState());
//        patientDTO.setZip(visitEntity.getPatient().getZip());
//        patientDTO.setBloodGroup(visitEntity.getPatient().getBloodGroup());
        patientDTO.setDateOfBirth(visitEntity.getPatient().getDateOfBirth());
        patientDTO.setGender(visitEntity.getPatient().getGender());
        patientDTO.setAge(visitEntity.getPatient().getAge());

        // Map visit details
        VisitDTO visitDTO = new VisitDTO();
        visitDTO.setVisitId(visitEntity.getVisitId());
        visitDTO.setVisitDate(visitEntity.getVisitDate());
        visitDTO.setVisitType(visitEntity.getVisitType());
        visitDTO.setVisitStatus(visitEntity.getVisitStatus());
        visitDTO.setVisitDescription(visitEntity.getVisitDescription());
//        visitDTO.setDoctorId(visitEntity.getDoctor().getId());
        // doctor id be may be null
        visitDTO.setDoctorId(visitEntity.getDoctor() != null ? visitEntity.getDoctor().getId() : null);
        visitDTO.setTestIds(visitEntity.getTests().stream().map(Test::getId).collect(Collectors.toList()));
        visitDTO.setPackageIds(visitEntity.getPackages().stream().map(HealthPackage::getId).collect(Collectors.toList()));
        visitDTO.setInsuranceIds(visitEntity.getInsurance().stream().map(InsuranceEntity::getId).collect(Collectors.toList()));

        // Map billing details
        if (visitEntity.getBilling() != null) {
            BillingDTO billingDTO = new BillingDTO();
            billingDTO.setBillingId(visitEntity.getBilling().getId());
            billingDTO.setTotalAmount(visitEntity.getBilling().getTotalAmount());
            billingDTO.setPaymentStatus(visitEntity.getBilling().getPaymentStatus());
            billingDTO.setPaymentMethod(visitEntity.getBilling().getPaymentMethod());
            billingDTO.setPaymentDate(visitEntity.getBilling().getPaymentDate());
            billingDTO.setDiscount(visitEntity.getBilling().getDiscount());


//            billingDTO.setGstRate(visitEntity.getBilling().getGstRate());
//            billingDTO.setGstAmount(visitEntity.getBilling().getGstAmount());
//            billingDTO.setCgstAmount(visitEntity.getBilling().getCgstAmount());
//            billingDTO.setSgstAmount(visitEntity.getBilling().getSgstAmount());
//            billingDTO.setIgstAmount(visitEntity.getBilling().getIgstAmount());

            billingDTO.setNetAmount(visitEntity.getBilling().getNetAmount());
            billingDTO.setDiscountReason(visitEntity.getBilling().getDiscountReason());


            if (billingDTO.getTransactions() != null) {
                List<TransactionDTO> transactionDTOs = visitEntity.getBilling().getTransactions().stream()
                        .map(TransactionDTO::new)
                        .collect(Collectors.toList());
                billingDTO.setTransactions((Set<TransactionDTO>) transactionDTOs);
            } else {
                billingDTO.setTransactions(new HashSet<>());
            }

            visitDTO.setBilling(billingDTO);
        }
        // Map test discounts
        if(visitEntity.getBilling() != null) {
            List<TestDiscountEntity> testDiscounts = testDiscountRepository.findAllByBilling(visitEntity.getBilling());
            List<TestDiscountDTO> testDiscountDTOs = testDiscounts.stream()
                    .map(testDiscount -> new TestDiscountDTO(
                            testDiscount.getTestId(),
                            testDiscount.getDiscountAmount(),
                            testDiscount.getDiscountPercent(),
                            testDiscount.getFinalPrice(),
                            testDiscount.getCreatedBy(),
                            testDiscount.getUpdatedBy()
                    )).collect(Collectors.toList());
            visitDTO.setListOfEachTestDiscount(testDiscountDTOs);
        } else {
            visitDTO.setListOfEachTestDiscount(new ArrayList<>());
        }
        patientDTO.setVisit(visitDTO);
        return patientDTO;
    }

    @Transactional
    public void updateVisit(Long labId, Long visitId, VisitDTO visitDTO, Optional<User> currentUser) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .filter(visitEntity -> visitEntity.getPatient().getLabs().contains(labOptional.get()))
                .orElseThrow(() -> new IllegalArgumentException("Visit not found or does not belong to the lab"));
        Optional<Doctors> doctorOptional = doctorRepository.findById(visitDTO.getDoctorId());
        if (doctorOptional.isEmpty()) {
            ApiResponseHelper.errorResponse("Doctor not found", HttpStatus.NOT_FOUND);
        }
        visit.setVisitDate(visitDTO.getVisitDate());
        visit.setVisitType(visitDTO.getVisitType());
        visit.setVisitStatus(visitDTO.getVisitStatus());
        visit.setVisitDescription(visitDTO.getVisitDescription());
        visit.setDoctor(doctorOptional.get());
        visit.getLabs().add(labOptional.get());
        Set<Test> tests = testRepository.findAllById(visitDTO.getTestIds()).stream().collect(Collectors.toSet());
        visit.setTests(tests);
        Set<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds()).stream().collect(Collectors.toSet());
        visit.setPackages(healthPackages);
        List<InsuranceEntity> insurances = insuranceRepository.findAllById(
                visitDTO.getInsuranceIds()
                        .stream()
                        .collect(Collectors.toList()) // Keep it as List<Long>
        );
        if (insurances.stream().anyMatch(insurance -> !insurance.getLabs().contains(labOptional.get()))) {
            ApiResponseHelper.errorResponse("Insurance not belong to the lab", HttpStatus.BAD_REQUEST);
        }
        visit.setInsurance(new HashSet<>(insurances));
        // Handle billing information
        BillingEntity billingEntity = visit.getBilling();
        billingEntity.setTotalAmount(visitDTO.getBilling().getTotalAmount());
        billingEntity.setPaymentStatus(visitDTO.getBilling().getPaymentStatus());
        billingEntity.setPaymentMethod(visitDTO.getBilling().getPaymentMethod());
        billingEntity.setPaymentDate(visitDTO.getBilling().getPaymentDate());
        billingEntity.setDiscount(visitDTO.getBilling().getDiscount());
//        billingEntity.setGstRate(visitDTO.getBilling().getGstRate());
//        billingEntity.setGstAmount(visitDTO.getBilling().getGstAmount());
//        billingEntity.setCgstAmount(visitDTO.getBilling().getCgstAmount());
//        billingEntity.setSgstAmount(visitDTO.getBilling().getSgstAmount());
//        billingEntity.setIgstAmount(visitDTO.getBilling().getIgstAmount());
        billingEntity.setNetAmount(visitDTO.getBilling().getNetAmount());
        billingEntity.getLabs().add(labOptional.get());
        billingRepository.save(billingEntity);
        visit.setBilling(billingEntity);
        // Save the visit
        visitRepository.save(visit);

    }

    @Transactional
    public void deleteVisit(Long labId, Long visitId, Optional<User> currentUser) {
        // Validate lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new IllegalArgumentException("Lab not found");
        }
        
        // Validate user access to lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            throw new IllegalArgumentException("User is not a member of this lab");
        }
        
        // Find and validate visit exists and belongs to the lab
        Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            throw new IllegalArgumentException("Visit not found");
        }
        
        VisitEntity visit = visitOptional.get();
        
        // Verify visit belongs to the lab
        if (!visit.getPatient().getLabs().contains(labOptional.get())) {
            throw new IllegalArgumentException("Visit does not belong to the specified lab");
        }
        
        // Delete all related entities in the correct order
        
        // 1. Delete VisitTestResults (cascade should handle this, but being explicit)
        if (visit.getTestResults() != null && !visit.getTestResults().isEmpty()) {
            visitTestResultRepository.deleteAll(visit.getTestResults());
        }
        
        // 2. Clear many-to-many relationships
        visit.getTests().clear();
        visit.getPackages().clear();
        visit.getInsurance().clear();
        visit.getSamples().clear();
        visit.getLabs().clear();
        
        // 3. Delete billing and related entities
        if (visit.getBilling() != null) {
            BillingEntity billing = visit.getBilling();
            
            // Delete test discounts related to billing
            if (billing.getTestDiscounts() != null && !billing.getTestDiscounts().isEmpty()) {
                testDiscountRepository.deleteAll(billing.getTestDiscounts());
            }
            
            // Delete transactions related to billing
            if (billing.getTransactions() != null && !billing.getTransactions().isEmpty()) {
                // Note: TransactionRepository would be needed if transactions need explicit deletion
                // For now, relying on cascade delete
            }
            
            // Delete the billing entity
            billingRepository.delete(billing);
        }
        
        // 4. Finally delete the visit entity
        visitRepository.delete(visit);
    }

    @Transactional
    public int deleteAllVisitsForLab(Long labId, Optional<User> currentUser) {
        // Validate lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new IllegalArgumentException("Lab not found");
        }
        
        // Validate user access to lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            throw new IllegalArgumentException("User is not a member of this lab");
        }
        
        Lab lab = labOptional.get();
        
        // Find all visits for this lab
        List<VisitEntity> visits = visitRepository.findAllByPatient_Labs(lab);
        
        if (visits.isEmpty()) {
            return 0; // No visits to delete
        }
        
        int deletedCount = 0;
        
        // Delete each visit and all its related data
        for (VisitEntity visit : visits) {
            try {
                // 1. Delete VisitTestResults
                if (visit.getTestResults() != null && !visit.getTestResults().isEmpty()) {
                    visitTestResultRepository.deleteAll(visit.getTestResults());
                }
                
                // 2. Clear many-to-many relationships
                visit.getTests().clear();
                visit.getPackages().clear();
                visit.getInsurance().clear();
                visit.getSamples().clear();
                visit.getLabs().clear();
                
                // 3. Delete billing and related entities
                if (visit.getBilling() != null) {
                    BillingEntity billing = visit.getBilling();
                    
                    // Delete test discounts related to billing
                    if (billing.getTestDiscounts() != null && !billing.getTestDiscounts().isEmpty()) {
                        testDiscountRepository.deleteAll(billing.getTestDiscounts());
                    }
                    
                    // Delete transactions related to billing (cascade should handle this)
                    // Note: If explicit deletion is needed, TransactionRepository would be required
                    
                    // Delete the billing entity
                    billingRepository.delete(billing);
                }
                
                // 4. Delete the visit entity
                visitRepository.delete(visit);
                deletedCount++;
                
            } catch (Exception e) {
                // Log the error but continue with other visits
                System.err.println("Error deleting visit " + visit.getVisitId() + ": " + e.getMessage());
                // You might want to use a proper logger instead of System.err
            }
        }
        
        return deletedCount;
    }

    public Object getVisit(Long labId, Long visitId, Optional<User> currentUser) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }
        Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .filter(visitEntity -> visitEntity.getPatient().getLabs().contains(labOptional.get()))
                .orElseThrow(() -> new IllegalArgumentException("Visit not found or does not belong to the lab"));

        //find test discount for the
        PatientDTO patientDTO = mapVisitToPatientDTO(visit);
        return patientDTO;
    }

    public Object getVisitByPatient(Long labId, Long patientId, Optional<User> currentUser) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }
        Optional<PatientEntity> patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().contains(labOptional.get()));
        if (patientEntity.isEmpty()) {
            return ApiResponseHelper.errorResponse("Patient not belong to the lab", HttpStatus.BAD_REQUEST);
        }
        List<VisitEntity> visits = visitRepository.findAllByPatient(patientEntity.get());
        List<PatientDTO> patientDTOList = visits.stream()
                .map(this::mapVisitToPatientDTO)
                .collect(Collectors.toList());
        return patientDTOList;
    }

    public @NotNull List<PatientDetailsDto> getVisitsByDateRange(Long labId, Optional<User> currentUser, LocalDate startDate, LocalDate endDate) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }

        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a member of this lab");
        }

        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date are required");
        }

        List<VisitEntity> visits = visitRepository.findAllByPatient_LabsAndVisitDateBetween(
                labOptional.get(), startDate, endDate
        );

        List<PatientDetailsDto> result = visits.stream()
                .map(visit -> {
                    PatientDetailsDto dto = new PatientDetailsDto();
                    // Map patient details
                    PatientEntity patient = visit.getPatient();
                    dto.setId(patient.getId());
                    dto.setFirstName(patient.getFirstName());
                    dto.setLastName(patient.getLastName());
                    dto.setCity(patient.getCity());
                    dto.setDateOfBirth(patient.getDateOfBirth());
                    dto.setGender(patient.getGender());

                    // Map visit details
                    VisitDetailDto visitDetailDto = new VisitDetailDto();
                    visitDetailDto.setVisitId(visit.getVisitId());
                    visitDetailDto.setVisitDate(visit.getVisitDate());
                    visitDetailDto.setVisitType(visit.getVisitType());
                    visitDetailDto.setVisitStatus(visit.getVisitStatus());
                    visitDetailDto.setDoctorId(visit.getDoctor() != null ? visit.getDoctor().getId() : null);

                    // Map test and package IDs
                    visitDetailDto.setTestIds(visit.getTests().stream()
                            .map(Test::getId)
                            .collect(Collectors.toList()));
                    visitDetailDto.setPackageIds(visit.getPackages().stream()
                            .map(HealthPackage::getId)
                            .collect(Collectors.toList()));

                    // Map billing details
                    BillingEntity billing = visit.getBilling();
                    if (billing != null) {
                        BellingDetailsDto billingDto = new BellingDetailsDto();
                        billingDto.setBillingId(Long.valueOf(billing.getId()));
                        billingDto.setTotalAmount(billing.getTotalAmount());
                        billingDto.setPaymentStatus(billing.getPaymentStatus());
                        billingDto.setPaymentMethod(billing.getPaymentMethod());
                        billingDto.setPaymentDate(billing.getPaymentDate() != null ? billing.getPaymentDate().toString() : null);
                        billingDto.setDiscount(billing.getDiscount());
                        billingDto.setNetAmount(billing.getNetAmount());
                        billingDto.setDiscountReason(billing.getDiscountReason());
//                        billingDto.setDiscountPercentage(billing.getDiscountPercentage());
                        visitDetailDto.setBellingDetailsDto(billingDto);
                    }

                    // Map test discounts
                    List<TestDiscountEntity> testDiscounts = testDiscountRepository.findAllByBilling(billing);
                    List<TestDiscountDTO> testDiscountDTOs = testDiscounts.stream()
                            .map(testDiscount -> new TestDiscountDTO(
                                    testDiscount.getTestId(),
                                    testDiscount.getDiscountAmount(),
                                    testDiscount.getDiscountPercent(),
                                    testDiscount.getFinalPrice(),
                                    testDiscount.getCreatedBy(),
                                    testDiscount.getUpdatedBy()
                            )).collect(Collectors.toList());

                    dto.setVisitDetailDto(visitDetailDto);
                    return dto;
                })
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            ApiResponseHelper.successResponseWithDataAndMessage("No visits found for the given date range", HttpStatus.OK, Collections.emptyList());
        }
        return result;
    }


    public Object getVisitDateWise(Long labId, LocalDate startDate, LocalDate endDate, Optional<User> currentUser) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }
        if (startDate == null || endDate == null) {
            return ApiResponseHelper.errorResponse("Start date and end date are required", HttpStatus.BAD_REQUEST);
        }
        List<VisitEntity> visits = visitRepository.findAllByPatient_LabsAndVisitDateBetween(
                labOptional.get(), startDate, endDate
        );
        List<PatientDTO> patientDTOList = visits.stream()
                .map(this::mapVisitToPatientDTO)
                .collect(Collectors.toList());
        return patientDTOList;
    }


    public List<PatientVisitDTO> getPatientVisits(Long labId, LocalDate startDate, LocalDate endDate, Optional<User> currentUser) {
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a member of this lab");
        }
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date are required");
        }
        List<VisitEntity> visits = visitRepository.findAllByPatient_LabsAndVisitDateBetween(
                labOptional.get(), startDate, endDate
        );

        // make a response object
        List<PatientVisitDTO> patientVisitDTOs = visits.stream()
                .map(visit -> {
                    PatientEntity patient = visit.getPatient();
                    VisitDetailsDTO visitDetailsDTO = new VisitDetailsDTO(visit);
                    return new PatientVisitDTO(
                            patient.getPatientId(),
                            patient.getFirstName(),
                            patient.getLastName(),
                            patient.getPatientCode(),
                            patient.getPhone(),
                            patient.getCity(),
                            patient.getDateOfBirth(),
                            patient.getAge(),
                            patient.getGender(),
                            patient.getVisits().isEmpty()
                                    ? null
                                    : visitDetailsDTO,
                            visit.getCreatedBy(),
                            visit.getUpdatedBy()
                    );
                })
                .collect(Collectors.toList());

        if (patientVisitDTOs.isEmpty()) {
            //send Message no visit found
            ApiResponseHelper.successResponseWithDataAndMessage("No visits found for the given date range", HttpStatus.OK, Collections.emptyList());
        }
        return patientVisitDTOs;
    }
}




