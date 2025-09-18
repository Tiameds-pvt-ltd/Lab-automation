package tiameds.com.tiameds.services.lab;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.*;
import tiameds.com.tiameds.dto.visits.BillDTO;
import tiameds.com.tiameds.dto.visits.BillDtoDue;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.*;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PatientService {
    private final LabRepository labRepository;
    private final TestRepository testRepository;
    private final HealthPackageRepository healthPackageRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final HealthPackageRepository packageRepository;
    private final InsuranceRepository insuranceRepository;

    @Autowired
    private final BillingRepository billingRepository;
    private final TestDiscountRepository testDiscountRepository;
    private final VisitRepository visitRepository;

    public PatientService(LabRepository labRepository,
                          TestRepository testRepository,
                          HealthPackageRepository healthPackageRepository,
                          PatientRepository patientRepository,
                          DoctorRepository doctorRepository,
                          HealthPackageRepository packageRepository,
                          InsuranceRepository insuranceRepository,
                          BillingRepository billingRepository,
                          TestDiscountRepository testDiscountRepository, VisitRepository visitRepository
    ) {
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.packageRepository = packageRepository;
        this.insuranceRepository = insuranceRepository;
        this.billingRepository = billingRepository;
        this.testDiscountRepository = testDiscountRepository;
        this.visitRepository = visitRepository;
    }


    public List<PatientDTO> getAllPatientsByLabId(Long labId) {
        return patientRepository.findAllByLabsId(labId).stream()
                .map(this::convertToPatientDTO)
                .collect(Collectors.toList());
    }

    public Object getPatientById(Long patientId, Long labId) {
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        Optional<PatientEntity> patient = patientRepository.findById(patientId);
        if (patient.isEmpty() || !patient.get().getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
        }
        return convertToPatientDTO(patient.get());
    }


    @Transactional
    public void updatePatient(Long patientId, Long labId, PatientDTO patientDTO, String currentUser) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        PatientEntity patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().stream()
                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
        updatePatientEntityFromDTO(patientEntity, patientDTO, String.valueOf(currentUser));
        patientEntity.getLabs().clear();
        patientEntity.getLabs().add(lab);
        patientRepository.save(patientEntity);
    }

    @Transactional
    public void deletePatient(Long patientId, Long labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        PatientEntity patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().stream()
                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
        patientRepository.delete(patientEntity);
    }

    public Optional<PatientEntity> findByPhoneAndFirstNameAndLabsId(String phone, String firstName, long id) {
        return patientRepository.findByPhoneAndFirstNameAndLabsId(phone, firstName, id);
    }

    @Transactional(rollbackOn = Exception.class)
    public PatientDTO savePatientWithDetails(Lab lab, PatientDTO patientDTO, String currentUser) {
        try {
            Optional<PatientEntity> existingPatient = findByPhoneAndFirstNameAndLabsId(
                    patientDTO.getPhone(),
                    patientDTO.getFirstName()
                    , lab.getId()
            );
            if (existingPatient.isPresent()) {
                return addVisitAndBillingToExistingPatient(lab, patientDTO, existingPatient.get(), currentUser);
            }

            PatientEntity patient = mapPatientDTOToEntity(patientDTO, currentUser);
            Optional<PatientEntity> guardian = patientRepository.findFirstByPhoneOrderByPatientIdAsc(patientDTO.getPhone());
            guardian.ifPresent(patient::setGuardian);
            patient.getLabs().add(lab);
            if (patientDTO.getVisit() != null) {
                VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab, currentUser);
                visit.setPatient(patient);
                patient.getVisits().add(visit);
            }
            PatientEntity savedPatient = patientRepository.save(patient);
            return new PatientDTO(savedPatient);
        } catch (Exception e) {
//            log.error("Error saving patient with details", e);
            throw new RuntimeException("Failed to save patient: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackOn = Exception.class)
    public PatientDTO addVisitAndBillingToExistingPatient(Lab lab, PatientDTO patientDTO, PatientEntity existingPatient, String currentUser) {
        if (patientDTO.getLastName() != null) existingPatient.setLastName(patientDTO.getLastName());
        if (patientDTO.getEmail() != null) existingPatient.setEmail(patientDTO.getEmail());
        if (patientDTO.getVisit() != null) {
            VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab, currentUser);
            visit.setPatient(existingPatient);
            existingPatient.getVisits().add(visit);
        }
        patientRepository.save(existingPatient);
        return new PatientDTO(existingPatient);
    }

    private PatientDTO convertToPatientDTO(PatientEntity patient) {
        PatientDTO patientDTO = new PatientDTO();
        patientDTO.setId(patient.getPatientId());
        patientDTO.setFirstName(patient.getFirstName());
        patientDTO.setLastName(patient.getLastName());
        patientDTO.setEmail(patient.getEmail());
        patientDTO.setPhone(patient.getPhone());
        patientDTO.setAddress(patient.getAddress());
        patientDTO.setCity(patient.getCity());
        patientDTO.setState(patient.getState());
        patientDTO.setZip(patient.getZip());
        patientDTO.setBloodGroup(patient.getBloodGroup());
        patientDTO.setDateOfBirth(patient.getDateOfBirth());
        patientDTO.setAge(patient.getAge());

        patientDTO.setGender(patient.getGender());
        return patientDTO;
    }

    private void updatePatientEntityFromDTO(PatientEntity entity, PatientDTO dto, String currentUser) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZip(dto.getZip());
        entity.setBloodGroup(dto.getBloodGroup());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setAge(dto.getAge());
        entity.setCreatedBy(currentUser);
    }

    private PatientEntity mapPatientDTOToEntity(PatientDTO dto, String currentUser) {
        PatientEntity entity = new PatientEntity();
        updatePatientEntityFromDTO(entity, dto, currentUser);
        entity.setGender(dto.getGender());
        return entity;
    }

    private VisitEntity mapVisitDTOToEntity(VisitDTO visitDTO, Lab lab, String currentUser) {
        VisitEntity visit = new VisitEntity();
        visit.setVisitDate(visitDTO.getVisitDate());
        visit.setVisitType(visitDTO.getVisitType());
        visit.setVisitStatus(visitDTO.getVisitStatus());
        visit.setVisitDescription(visitDTO.getVisitDescription());

        //----new fields for cancellation
        visit.setVisitCancellationReason(visitDTO.getVisitCancellationReason());
        visit.setVisitCancellationDate(visitDTO.getVisitCancellationDate());
        visit.setVisitCancellationBy(visitDTO.getVisitCancellationBy());
        visit.setVisitCancellationTime(visitDTO.getVisitCancellationTime());
        visit.setCreatedBy(currentUser);
        LocalDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        visit.setVisitTime(currentDateTime); // Set visit time to current date and time
        if (visitDTO.getDoctorId() != null) {
            Optional<Doctors> doctorOpt = doctorRepository.findById(visitDTO.getDoctorId());
            if (doctorOpt.isPresent()) {
                Doctors doctor = doctorOpt.get();
                if (!lab.getDoctors().contains(doctor)) {
                    throw new RuntimeException("Doctor does not belong to the lab");
                }
                visit.setDoctor(doctor);
            }
        }
        if (visitDTO.getTestIds() != null && !visitDTO.getTestIds().isEmpty()) {
            List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
            if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
                throw new RuntimeException("Test does not belong to the lab");
            }
            visit.setTests(new HashSet<>(tests));
        }
        if (visitDTO.getPackageIds() != null && !visitDTO.getPackageIds().isEmpty()) {
            List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
            if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
                throw new RuntimeException("Health package does not belong to the lab");
            }
            visit.setPackages(new HashSet<>(healthPackages));
        }
        if (visitDTO.getInsuranceIds() != null && !visitDTO.getInsuranceIds().isEmpty()) {
            List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
            if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
                throw new RuntimeException("Insurance does not belong to the lab");
            }
            visit.setInsurance(new HashSet<>(insurance));
        }
        visit.getLabs().add(lab);
        // handle billing
        if (visitDTO.getBilling() != null) {
            BillingEntity billing = mapBillingDTOToEntity(visitDTO.getBilling(), lab, currentUser);
            billing = billingRepository.save(billing); // 💥 Save billing before using it in discounts
            visit.setBilling(billing);
            if (visitDTO.getListOfEachTestDiscount() != null && !visitDTO.getListOfEachTestDiscount().isEmpty()) {
                BillingEntity finalBilling = billing;
                Set<TestDiscountEntity> discountEntities = visitDTO.getListOfEachTestDiscount().stream()
                        .map(discountDTO -> {
                            TestDiscountEntity discountEntity = new TestDiscountEntity();
                            discountEntity.setTestId(discountDTO.getTestId());
                            discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
                            discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
                            discountEntity.setFinalPrice(discountDTO.getFinalPrice());
                            discountEntity.setCreatedBy(currentUser);
                            discountEntity.setUpdatedBy(currentUser);
                            discountEntity.setBilling(finalBilling); // now managed
                            return discountEntity;
                        })
                        .collect(Collectors.toSet());
                testDiscountRepository.saveAll(discountEntities);
            }
        }
        // handle visit TestResults
        if (visitDTO.getTestResult() != null && !visitDTO.getTestResult().isEmpty()) {
            Set<VisitTestResult> testResults = visitDTO.getTestResult().stream()
                    .map(testResultDTO -> {
                        VisitTestResult testResult = new VisitTestResult();
                        // Set visit reference
                        testResult.setVisit(visit);
                        // Set isFilled only if provided in DTO, else stays false
                        if (testResultDTO.getIsFilled() != null) {
                            testResult.setIsFilled(testResultDTO.getIsFilled());
                            testResult.setReportStatus(testResultDTO.getReportStatus() != null
                                    ? testResultDTO.getReportStatus()
                                    : "PENDING");
                        }
                        // Fetch and set test entity
                        testResult.setTest(
                                testResultDTO.getTestId() != null
                                        ? testRepository.findById(testResultDTO.getTestId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                "Test not found with ID: " + testResultDTO.getTestId()))
                                        : null
                        );
                        // Set audit fields
                        testResult.setCreatedBy(currentUser);
                        testResult.setUpdatedBy(currentUser);
                        testResult.setTestStatus("ACTIVE");

                        return testResult;
                    })
                    .collect(Collectors.toSet());
            visit.setTestResults(testResults);
        }
        return visit;
    }

    public List<PatientList> getPatientbytheirPhoneAndLabId(String phone, Long labId) {
        List<PatientEntity> patients = patientRepository.findByPhoneStartingWithAndLabId(phone, labId);
        return patients.stream()
                .map(patient -> {
                    PatientList patientList = new PatientList();
                    patientList.setId(patient.getPatientId());
                    patientList.setFirstName(patient.getFirstName());
                    patientList.setPhone(patient.getPhone());
                    patientList.setCity(patient.getCity());
                    patientList.setGender(patient.getGender());
                    patientList.setDateOfBirth(patient.getDateOfBirth());
                    patientList.setAge(patient.getAge());
                    // Optional: Debug log
                    System.out.println("Mapped PatientList: " + patientList);
                    return patientList;
                })
                .collect(Collectors.toList());
    }

    public void cancelVisit(Long visitId, Long labId, String username, CancellationDataDTO cancellationData) {
        Optional<Lab> labOpt = labRepository.findById(labId);
        if (labOpt.isEmpty()) {
            throw new RuntimeException("Lab not found");
        }
        Lab lab = labOpt.get();
        Optional<VisitEntity> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isEmpty()) {
            throw new RuntimeException("Visit not found");
        }
        VisitEntity visit = visitOpt.get();
        if (!visit.getLabs().contains(lab)) {
            throw new RuntimeException("Visit does not belong to the specified lab");
        }

        // Update visit cancellation details
        visit.setVisitCancellationReason(cancellationData.getVisitCancellationReason());
        visit.setVisitCancellationDate(LocalDate.parse(cancellationData.getVisitCancellationDate()));
        visit.setVisitCancellationTime(LocalDateTime.of(LocalDate.parse(cancellationData.getVisitCancellationDate()), LocalTime.parse(cancellationData.getVisitCancellationTime())));

        visit.setVisitStatus("CANCELLED");
        visit.setUpdatedBy(username);
        visit.setVisitCancellationBy(username);
    }

    private BillingEntity mapBillingDTOToEntity(BillingDTO billingDTO, Lab lab, String currentUser) {
        BillingEntity billing = billingDTO.getBillingId() != null ?
                billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity()) :
                new BillingEntity();

        // Map basic billing fields with null checks and defaults
        billing.setTotalAmount(billingDTO.getTotalAmount() != null ? billingDTO.getTotalAmount() : BigDecimal.ZERO);
        billing.setPaymentStatus(billingDTO.getPaymentStatus() != null ? billingDTO.getPaymentStatus() : "PAID");
        billing.setPaymentMethod(billingDTO.getPaymentMethod() != null ? billingDTO.getPaymentMethod() : "CASH");
        billing.setPaymentDate(billingDTO.getPaymentDate() != null ? billingDTO.getPaymentDate() : LocalDate.now().toString());
        billing.setDiscount(billingDTO.getDiscount() != null ? billingDTO.getDiscount() : BigDecimal.ZERO);
        billing.setGstRate(billingDTO.getGstRate() != null ? billingDTO.getGstRate() : BigDecimal.ZERO);
        billing.setGstAmount(billingDTO.getGstAmount() != null ? billingDTO.getGstAmount() : BigDecimal.ZERO);
        billing.setCgstAmount(billingDTO.getCgstAmount() != null ? billingDTO.getCgstAmount() : BigDecimal.ZERO);
        billing.setSgstAmount(billingDTO.getSgstAmount() != null ? billingDTO.getSgstAmount() : BigDecimal.ZERO);
        billing.setIgstAmount(billingDTO.getIgstAmount() != null ? billingDTO.getIgstAmount() : BigDecimal.ZERO);
        billing.setNetAmount(billingDTO.getNetAmount() != null ? billingDTO.getNetAmount() : BigDecimal.ZERO);
        billing.setDiscountReason(billingDTO.getDiscountReason());
        billing.setReceivedAmount(billingDTO.getReceivedAmount() != null ? billingDTO.getReceivedAmount() : BigDecimal.ZERO);
        billing.setDueAmount(billingDTO.getDueAmount() != null ? billingDTO.getDueAmount() : BigDecimal.ZERO);

        // Audit fields
        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
        billing.setBillingDate(LocalDate.now().toString());
        billing.setCreatedBy(currentUser);
        billing.setUpdatedBy(currentUser);

        // Keep existing transactions - do not clear them

        // Handle transactions - only create if there's actual money movement
        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
                // Only create transaction if there's actual money movement
                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO;
                BigDecimal refundAmount = transactionDTO.getRefundAmount() != null ? transactionDTO.getRefundAmount() : BigDecimal.ZERO;
                
                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0 || refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    TransactionEntity transaction = new TransactionEntity();

                    transaction.setPaymentMethod(transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH");
                    transaction.setUpiId(transactionDTO.getUpiId());
                    transaction.setUpiAmount(transactionDTO.getUpiAmount() != null ? transactionDTO.getUpiAmount() : BigDecimal.ZERO);
                    transaction.setCardAmount(transactionDTO.getCardAmount() != null ? transactionDTO.getCardAmount() : BigDecimal.ZERO);
                    transaction.setCashAmount(transactionDTO.getCashAmount() != null ? transactionDTO.getCashAmount() : BigDecimal.ZERO);
                    transaction.setReceivedAmount(transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO);
                    transaction.setRefundAmount(transactionDTO.getRefundAmount() != null ? transactionDTO.getRefundAmount() : BigDecimal.ZERO);
                    transaction.setDueAmount(transactionDTO.getDueAmount() != null ? transactionDTO.getDueAmount() : BigDecimal.ZERO);
                    transaction.setPaymentDate(transactionDTO.getPaymentDate() != null ? transactionDTO.getPaymentDate() : LocalDate.now().toString());
                    transaction.setRemarks(transactionDTO.getRemarks());
                    transaction.setCreatedBy(currentUser);

                    // Set bidirectional relationship
                    transaction.setBilling(billing);
                    billing.getTransactions().add(transaction);
                }
            }
        }
        // Add lab association (avoid duplicates)
        billing.getLabs().add(lab);

        return billing;
    }




    public Optional<BillingEntity> getBillingById(Long billingId) {
        return billingRepository.findById(billingId);
    }

    public BillDTO addPartialPayment(BillingEntity billing, BillDtoDue billDTO, String username) {
        if (billing == null || billDTO == null) {
            throw new IllegalArgumentException("Billing and billDTO cannot be null");
        }
        TransactionDTO transactionDTO = billDTO.getTransaction();
        if (transactionDTO == null) {
            throw new IllegalArgumentException("Transaction data is required");
        }
        TransactionEntity transaction = new TransactionEntity();

        // Set payment method (default to CASH if null)
        transaction.setPaymentMethod(
                transactionDTO.getPaymentMethod() != null ?
                        transactionDTO.getPaymentMethod() : "CASH"
        );

        // Set payment amounts (default to 0 if null)
        transaction.setUpiId(transactionDTO.getUpiId());
        transaction.setUpiAmount(
                transactionDTO.getUpiAmount() != null ?
                        transactionDTO.getUpiAmount() : BigDecimal.ZERO
        );
        transaction.setCardAmount(
                transactionDTO.getCardAmount() != null ?
                        transactionDTO.getCardAmount() : BigDecimal.ZERO
        );
        transaction.setCashAmount(
                transactionDTO.getCashAmount() != null ?
                        transactionDTO.getCashAmount() : BigDecimal.ZERO
        );

        // Validate received amount
        if (transactionDTO.getReceivedAmount() == null ||
                transactionDTO.getReceivedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Received amount must be positive");
        }
        transaction.setReceivedAmount(transactionDTO.getReceivedAmount());

        // Set other transaction fields
        transaction.setRefundAmount(
                transactionDTO.getRefundAmount() != null ?
                        transactionDTO.getRefundAmount() : BigDecimal.ZERO
        );
        transaction.setDueAmount(
                transactionDTO.getDueAmount() != null ?
                        transactionDTO.getDueAmount() : BigDecimal.ZERO
        );
        transaction.setPaymentDate(
                transactionDTO.getPaymentDate() != null ?
                        transactionDTO.getPaymentDate() : LocalDate.now().toString()
        );
        transaction.setRemarks(
                transactionDTO.getRemarks() != null ?
                        transactionDTO.getRemarks() : "Payment via " + transaction.getPaymentMethod()
        );
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCreatedBy(username);

        // Link transaction to billing
        transaction.setBilling(billing);
        billing.getTransactions().add(transaction);

        // Calculate new amounts
        BigDecimal newReceivedAmount = billing.getReceivedAmount().add(transaction.getReceivedAmount());
        BigDecimal newDueAmount = billing.getTotalAmount().subtract(newReceivedAmount);

        // Update billing
        billing.setReceivedAmount(newReceivedAmount);
        billing.setDueAmount(newDueAmount);

        // Update payment status
        if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
            billing.setPaymentStatus("PAID");
        } else if (newReceivedAmount.compareTo(BigDecimal.ZERO) > 0) {
            billing.setPaymentStatus("DUE");
        } else {
            billing.setPaymentStatus("DUE");
        }
        // Update payment method if provided in DTO
        if (billDTO.getPaymentMethod() != null) {
            billing.setPaymentMethod(billDTO.getPaymentMethod());
        }
        // Update payment date if provided in DTO
        if (billDTO.getPaymentDate() != null) {
            billing.setPaymentDate(billDTO.getPaymentDate());
        }
        // Save and return
        billing = billingRepository.save(billing);
        return new BillDTO(billing);
    }

}


