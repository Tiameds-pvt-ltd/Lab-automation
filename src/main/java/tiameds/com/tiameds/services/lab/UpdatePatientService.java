package tiameds.com.tiameds.services.lab;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.TestDiscountDTO;
import tiameds.com.tiameds.dto.lab.TransactionDTO;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UpdatePatientService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TestRepository testRepository;
    private final HealthPackageRepository healthPackageRepository;
    private final InsuranceRepository insuranceRepository;
    private final BillingRepository billingRepository;
    private final TestDiscountRepository testDiscountRepository;
    private final VisitRepository visitRepository;
    private final LabRepository labRepository;
    private final BillingManagementService billingManagementService;

    public UpdatePatientService(PatientRepository patientRepository, DoctorRepository doctorRepository, TestRepository testRepository, HealthPackageRepository healthPackageRepository, InsuranceRepository insuranceRepository, BillingRepository billingRepository, TestDiscountRepository testDiscountRepository, VisitRepository visitRepository, LabRepository labRepository, BillingManagementService billingManagementService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.insuranceRepository = insuranceRepository;
        this.billingRepository = billingRepository;
        this.testDiscountRepository = testDiscountRepository;
        this.visitRepository = visitRepository;
        this.labRepository = labRepository;
        this.billingManagementService = billingManagementService;
    }

    @Transactional
    public PatientDTO updatePatientDetails(PatientEntity existingPatient, Lab lab, PatientDTO patientDTO, String username) {
        try {
            // Update basic patient information
            updateBasicPatientInfo(existingPatient, patientDTO, username);

            // Handle visit update if provided
            if (patientDTO.getVisit() != null) {
                handleVisitUpdate(existingPatient, lab, patientDTO.getVisit(), username);
            }

            // Save the updated patient
            PatientEntity savedPatient = patientRepository.save(existingPatient);
            return new PatientDTO(savedPatient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update patient: " + e.getMessage(), e);
        }
    }

    private void updateBasicPatientInfo(PatientEntity existingPatient, PatientDTO patientDTO, String username) {
            if (patientDTO.getLastName() != null) {
                existingPatient.setLastName(patientDTO.getLastName());
            }
            if (patientDTO.getEmail() != null) {
                existingPatient.setEmail(patientDTO.getEmail());
            }
            if (patientDTO.getPhone() != null) {
                existingPatient.setPhone(patientDTO.getPhone());
            }
            if (patientDTO.getAddress() != null) {
                existingPatient.setAddress(patientDTO.getAddress());
            }
            if (patientDTO.getCity() != null) {
                existingPatient.setCity(patientDTO.getCity());
            }
            if (patientDTO.getState() != null) {
                existingPatient.setState(patientDTO.getState());
            }
            if (patientDTO.getZip() != null) {
                existingPatient.setZip(patientDTO.getZip());
            }
            if (patientDTO.getBloodGroup() != null) {
                existingPatient.setBloodGroup(patientDTO.getBloodGroup());
            }
            if (patientDTO.getDateOfBirth() != null) {
                existingPatient.setDateOfBirth(patientDTO.getDateOfBirth());
            }
            if (patientDTO.getGender() != null) {
                existingPatient.setGender(patientDTO.getGender());
            }
            if (patientDTO.getAge() != null) {
                existingPatient.setAge(patientDTO.getAge());
            }
                existingPatient.setUpdatedBy(username);
    }

    private void handleVisitUpdate(PatientEntity patient, Lab lab, VisitDTO visitDTO, String username) {
        // Check if we're updating an existing visit or creating a new one
        if (visitDTO.getVisitId() != null) {
            // Update existing visit
            Optional<VisitEntity> existingVisitOpt = patient.getVisits().stream()
                    .filter(v -> v.getVisitId().equals(visitDTO.getVisitId()))
                    .findFirst();

            if (existingVisitOpt.isPresent()) {
                VisitEntity existingVisit = existingVisitOpt.get();
                updateVisitDetails(existingVisit, lab, visitDTO, username);
            } else {
                // Visit ID provided but not found - create new visit
                VisitEntity newVisit = mapVisitDTOToEntity(visitDTO, lab, username);
                newVisit.setPatient(patient);
                patient.getVisits().add(newVisit);
            }
        } else {
            // Create new visit
            VisitEntity newVisit = mapVisitDTOToEntity(visitDTO, lab, username);
            newVisit.setPatient(patient);
            patient.getVisits().add(newVisit);
        }
    }

    private void updateVisitDetails(VisitEntity visit, Lab lab, VisitDTO visitDTO, String username) {
        // Update basic visit information
        if (visitDTO.getVisitDate() != null) {
            visit.setVisitDate(visitDTO.getVisitDate());
        }
        if (visitDTO.getVisitType() != null) {
            visit.setVisitType(visitDTO.getVisitType());
        }
        if (visitDTO.getVisitStatus() != null) {
            visit.setVisitStatus(visitDTO.getVisitStatus());
        }
        if (visitDTO.getVisitDescription() != null) {
            visit.setVisitDescription(visitDTO.getVisitDescription());
        }

        // Handle cancellation fields
        if (visitDTO.getVisitCancellationReason() != null) {
            visit.setVisitCancellationReason(visitDTO.getVisitCancellationReason());
        }
        if (visitDTO.getVisitCancellationDate() != null) {
            visit.setVisitCancellationDate(visitDTO.getVisitCancellationDate());
        }
        if (visitDTO.getVisitCancellationBy() != null) {
            visit.setVisitCancellationBy(visitDTO.getVisitCancellationBy());
        }
        if (visitDTO.getVisitCancellationTime() != null) {
            visit.setVisitCancellationTime(visitDTO.getVisitCancellationTime());
        }

        visit.setUpdatedBy(username);

        // Handle doctor update
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

        // Handle tests update with billing recalculation
        if (visitDTO.getTestIds() != null) {
            if (visitDTO.getTestIds().isEmpty()) {
                // If empty list provided, remove all tests
                Set<Test> oldTests = visit.getTests();
                visit.setTests(new HashSet<>());
                
                // Set all test results to CANCELLED
                clearTestResultsForRemovedTests(visit, oldTests);
                
                // Recalculate billing for removed tests
                if (!oldTests.isEmpty()) {
                    recalculateBillingForTestChanges(visit, lab, new HashSet<>(), oldTests, username);
                }
            } else {
                // Update with new tests
                List<Test> newTests = testRepository.findAllById(visitDTO.getTestIds());
                if (newTests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
                throw new RuntimeException("Test does not belong to the lab");
            }
                
                // Calculate billing adjustments for test changes
                Set<Test> oldTests = visit.getTests();
                Set<Test> addedTests = new HashSet<>(newTests);
                addedTests.removeAll(oldTests);
                
                Set<Test> removedTests = new HashSet<>(oldTests);
                removedTests.removeAll(newTests);
                
                // Update tests
                visit.setTests(new HashSet<>(newTests));
                
                // Clear test results for removed tests and set status to CANCELLED
                if (!removedTests.isEmpty()) {
                    clearTestResultsForRemovedTests(visit, removedTests);
                }
                
                // Create test results for added tests with ACTIVE status
                if (!addedTests.isEmpty()) {
                    createTestResultsForAddedTests(visit, addedTests, username);
                }
                
                // Recalculate billing if there are test changes
                if (!addedTests.isEmpty() || !removedTests.isEmpty()) {
                    recalculateBillingForTestChanges(visit, lab, addedTests, removedTests, username);
                }
            }
        }

        // Handle packages update
        if (visitDTO.getPackageIds() != null) {
            List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
            if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
                throw new RuntimeException("Health package does not belong to the lab");
            }
            visit.setPackages(new HashSet<>(healthPackages));
        }

        // Handle insurance update
        if (visitDTO.getInsuranceIds() != null) {
            List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
            if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
                throw new RuntimeException("Insurance does not belong to the lab");
            }
            visit.setInsurance(new HashSet<>(insurance));
        }

        // Handle billing update
        if (visitDTO.getBilling() != null) {
            updateBillingDetails(visit, lab, visitDTO.getBilling(), visitDTO.getListOfEachTestDiscount(), username);
        }
    }

    private void updateBillingDetails(VisitEntity visit, Lab lab, BillingDTO billingDTO, List<TestDiscountDTO> testDiscounts, String username) {
        BillingEntity billing = visit.getBilling();
        if (billing == null) {
            billing = new BillingEntity();
            visit.setBilling(billing);
        }

        // Update basic billing fields
        if (billingDTO.getTotalAmount() != null) {
            billing.setTotalAmount(billingDTO.getTotalAmount());
        }
        if (billingDTO.getPaymentStatus() != null) {
            billing.setPaymentStatus(billingDTO.getPaymentStatus());
        }
        if (billingDTO.getPaymentMethod() != null) {
            billing.setPaymentMethod(billingDTO.getPaymentMethod());
        }
        if (billingDTO.getPaymentDate() != null) {
            billing.setPaymentDate(billingDTO.getPaymentDate());
        }
        if (billingDTO.getDiscount() != null) {
            billing.setDiscount(billingDTO.getDiscount());
        }
        if (billingDTO.getNetAmount() != null) {
            billing.setNetAmount(billingDTO.getNetAmount());
        }
        if (billingDTO.getDiscountReason() != null) {
            billing.setDiscountReason(billingDTO.getDiscountReason());
        }
        if (billingDTO.getReceivedAmount() != null) {
            billing.setReceivedAmount(billingDTO.getReceivedAmount());
        }
        if (billingDTO.getDueAmount() != null) {
            billing.setDueAmount(billingDTO.getDueAmount());
        }

        // Update audit fields
        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
        billing.setBillingDate(LocalDate.now().toString());
        billing.setUpdatedBy(username);

        // Handle transactions update - ONLY for payments, NOT for refunds
        // Refunds are handled by BillingManagementService to avoid duplication
        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
            // Add new transactions without clearing existing ones
            BillingEntity finalBilling = billing;
            billingDTO.getTransactions().forEach(transactionDTO -> {
                // Only create transaction if there's actual money movement
                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO;
                
                // ONLY create transactions for payments (receivedAmount > 0), NOT for refunds
                // Refunds are handled by BillingManagementService.updateBillingAfterCancellation()
                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    TransactionEntity transaction = new TransactionEntity();
                    transaction.setPaymentMethod(transactionDTO.getPaymentMethod());
                    transaction.setUpiId(transactionDTO.getUpiId());
                    transaction.setUpiAmount(transactionDTO.getUpiAmount());
                    transaction.setCardAmount(transactionDTO.getCardAmount());
                    transaction.setCashAmount(transactionDTO.getCashAmount());
                    transaction.setReceivedAmount(transactionDTO.getReceivedAmount());
                    transaction.setRefundAmount(BigDecimal.ZERO); // Never set refund amount here
                    transaction.setDueAmount(transactionDTO.getDueAmount());
                    transaction.setPaymentDate(transactionDTO.getPaymentDate() != null ?
                            transactionDTO.getPaymentDate() : LocalDate.now().toString());
                    transaction.setRemarks(transactionDTO.getRemarks());
                    transaction.setCreatedBy(username);
                    transaction.setBilling(finalBilling);
                    finalBilling.getTransactions().add(transaction);
                }
            });
        }

        billing.getLabs().add(lab);
        billing = billingRepository.save(billing);

        // Update due amounts in existing transactions to maintain data consistency
        if (billing.getId() != null) {
            billingManagementService.updateDueAmountsInAllTransactions(billing.getId(), username);
        }

        // Handle test discounts - ALWAYS clear existing and set new ones
        if (testDiscounts != null) {
            // Always remove existing discounts for this billing
            testDiscountRepository.deleteByBilling(billing);

            // Clear from billing entity as well
            billing.getTestDiscounts().clear();

            // Add new discounts if provided
            if (!testDiscounts.isEmpty()) {
            BillingEntity finalBilling1 = billing;
            Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
                    .map(discountDTO -> {
                        TestDiscountEntity discountEntity = new TestDiscountEntity();
                        discountEntity.setTestId(discountDTO.getTestId());
                        discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
                        discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
                        discountEntity.setFinalPrice(discountDTO.getFinalPrice());
                        discountEntity.setCreatedBy(username);
                        discountEntity.setUpdatedBy(username);
                        discountEntity.setBilling(finalBilling1);
                        return discountEntity;
                    })
                    .collect(Collectors.toSet());
            testDiscountRepository.saveAll(discountEntities);
        }
    }
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

                        return testResult;
                    })
                    .collect(Collectors.toSet());
            visit.setTestResults(testResults);
        }
        return visit;
    }

    /**
     * Clears test results for removed tests and sets status to CANCELLED
     */
    private void clearTestResultsForRemovedTests(VisitEntity visit, Set<Test> removedTests) {
        if (removedTests != null && !removedTests.isEmpty()) {
            Set<Long> removedTestIds = removedTests.stream()
                    .map(Test::getId)
                    .collect(Collectors.toSet());
            
            // Set status to CANCELLED for removed tests instead of deleting
            visit.getTestResults().stream()
                .filter(testResult -> testResult.getTest() != null && 
                        removedTestIds.contains(testResult.getTest().getId()))
                .forEach(testResult -> {
                    testResult.setTestStatus("CANCELLED");
//                    testResult.setReportStatus("CANCELLED");        //CANCELLED
                    testResult.setReportStatus("Pending");        //CANCELLED
                    testResult.setIsFilled(false);
                });
        }
    }

    /**
     * Creates test results for added tests with ACTIVE status
     */
    private void createTestResultsForAddedTests(VisitEntity visit, Set<Test> addedTests, String username) {
        if (addedTests != null && !addedTests.isEmpty()) {
            Set<VisitTestResult> newTestResults = addedTests.stream()
                .map(test -> {
                    VisitTestResult testResult = new VisitTestResult();
                    testResult.setVisit(visit);
                    testResult.setTest(test);
                    testResult.setTestStatus("ACTIVE");
                    testResult.setReportStatus("Pending");
                    testResult.setIsFilled(false);
                    testResult.setCreatedBy(username);
                    testResult.setUpdatedBy(username);
                    return testResult;
                })
                .collect(Collectors.toSet());
            
            visit.getTestResults().addAll(newTestResults);
        }
    }

    /**
     * Recalculates billing when tests are added or removed
     * Uses the comprehensive BillingManagementService for proper business logic
     */
    private void recalculateBillingForTestChanges(VisitEntity visit, Lab lab, Set<Test> addedTests, Set<Test> removedTests, String username) {
        BillingEntity billing = visit.getBilling();
        if (billing == null) {
            billing = new BillingEntity();
            visit.setBilling(billing);
        }

        // Calculate amounts for added tests
        BigDecimal addedAmount = addedTests.stream()
                .map(Test::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate amounts for removed tests
        BigDecimal removedAmount = removedTests.stream()
                .map(Test::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get current total amount
        BigDecimal currentTotal = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
        
        // Calculate new total amount
        BigDecimal newTotalAmount = currentTotal.add(addedAmount).subtract(removedAmount);
        billing.setTotalAmount(newTotalAmount);

        // Update net amount (assuming no discount changes for now)
        BigDecimal currentDiscount = billing.getDiscount() != null ? billing.getDiscount() : BigDecimal.ZERO;
        BigDecimal newNetAmount = newTotalAmount.subtract(currentDiscount);

        // Update audit fields
        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
        billing.setBillingDate(LocalDate.now().toString());
        billing.setUpdatedBy(username);
        billing.getLabs().add(lab);

        // Save billing first
        billing = billingRepository.save(billing);

        // Use the comprehensive billing service for proper recalculation
        if (billing.getId() != null) {
            billingManagementService.updateBillingAfterCancellation(billing.getId(), newNetAmount, username);
        }
    }


    /**
     * Maps BillingDTO to BillingEntity
     */
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

        // Handle transactions - ONLY for payments, NOT for refunds
        // Refunds are handled by BillingManagementService to avoid duplication
        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
                // Only create transaction if there's actual money movement
                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO;
                
                // ONLY create transactions for payments (receivedAmount > 0), NOT for refunds
                // Refunds are handled by BillingManagementService.updateBillingAfterCancellation()
                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    TransactionEntity transaction = new TransactionEntity();

                    transaction.setPaymentMethod(transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH");
                    transaction.setUpiId(transactionDTO.getUpiId());
                    transaction.setUpiAmount(transactionDTO.getUpiAmount() != null ? transactionDTO.getUpiAmount() : BigDecimal.ZERO);
                    transaction.setCardAmount(transactionDTO.getCardAmount() != null ? transactionDTO.getCardAmount() : BigDecimal.ZERO);
                    transaction.setCashAmount(transactionDTO.getCashAmount() != null ? transactionDTO.getCashAmount() : BigDecimal.ZERO);
                    transaction.setReceivedAmount(transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO);
                    transaction.setRefundAmount(BigDecimal.ZERO); // Never set refund amount here
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

}