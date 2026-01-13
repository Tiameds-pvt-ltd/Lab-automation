//package tiameds.com.tiameds.services.lab;
//
//import jakarta.persistence.EntityNotFoundException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import tiameds.com.tiameds.dto.lab.BillingDTO;
//import tiameds.com.tiameds.dto.lab.PatientDTO;
//import tiameds.com.tiameds.dto.lab.TestDiscountDTO;
//import tiameds.com.tiameds.dto.lab.TransactionDTO;
//import tiameds.com.tiameds.dto.lab.VisitDTO;
//import tiameds.com.tiameds.entity.*;
//import tiameds.com.tiameds.repository.*;
//
//import java.math.BigDecimal;
//import java.time.*;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//import org.slf4j.Logger;
//
//@Service
//public class UpdatePatientService {
//
//    private final PatientRepository patientRepository;
//    private final DoctorRepository doctorRepository;
//    private final TestRepository testRepository;
//    private final HealthPackageRepository healthPackageRepository;
//    private final InsuranceRepository insuranceRepository;
//    private final BillingRepository billingRepository;
//    private final TestDiscountRepository testDiscountRepository;
//    private final VisitRepository visitRepository;
//    private final LabRepository labRepository;
//    private final TransactionRepository transactionRepository;
//    private final BillingManagementService billingManagementService;
//    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UpdatePatientService.class);
//
//    public UpdatePatientService(PatientRepository patientRepository, DoctorRepository doctorRepository, TestRepository testRepository, HealthPackageRepository healthPackageRepository, InsuranceRepository insuranceRepository, BillingRepository billingRepository, TestDiscountRepository testDiscountRepository, VisitRepository visitRepository, LabRepository labRepository, TransactionRepository transactionRepository, BillingManagementService billingManagementService) {
//        this.patientRepository = patientRepository;
//        this.doctorRepository = doctorRepository;
//        this.testRepository = testRepository;
//        this.healthPackageRepository = healthPackageRepository;
//        this.insuranceRepository = insuranceRepository;
//        this.billingRepository = billingRepository;
//        this.testDiscountRepository = testDiscountRepository;
//        this.visitRepository = visitRepository;
//        this.labRepository = labRepository;
//        this.transactionRepository = transactionRepository;
//        this.billingManagementService = billingManagementService;
//    }
//
//    @Transactional
//    public PatientDTO updatePatientDetails(PatientEntity existingPatient, Lab lab, PatientDTO patientDTO, String username) {
//        try {
//            // Update basic patient information
//            updateBasicPatientInfo(existingPatient, patientDTO, username);
//
//            // Handle visit update if provided
//            if (patientDTO.getVisit() != null) {
//                handleVisitUpdate(existingPatient, lab, patientDTO.getVisit(), username);
//            }
//
//            // Save the updated patient
//            PatientEntity savedPatient = patientRepository.save(existingPatient);
//            return new PatientDTO(savedPatient);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to update patient: " + e.getMessage(), e);
//        }
//    }
//
//    private void updateBasicPatientInfo(PatientEntity existingPatient, PatientDTO patientDTO, String username) {
//        if (patientDTO.getLastName() != null) {
//            existingPatient.setLastName(patientDTO.getLastName());
//        }
//        if (patientDTO.getEmail() != null) {
//            existingPatient.setEmail(patientDTO.getEmail());
//        }
//        if (patientDTO.getPhone() != null) {
//            existingPatient.setPhone(patientDTO.getPhone());
//        }
//        if (patientDTO.getAddress() != null) {
//            existingPatient.setAddress(patientDTO.getAddress());
//        }
//        if (patientDTO.getCity() != null) {
//            existingPatient.setCity(patientDTO.getCity());
//        }
//        if (patientDTO.getState() != null) {
//            existingPatient.setState(patientDTO.getState());
//        }
//        if (patientDTO.getZip() != null) {
//            existingPatient.setZip(patientDTO.getZip());
//        }
//        if (patientDTO.getBloodGroup() != null) {
//            existingPatient.setBloodGroup(patientDTO.getBloodGroup());
//        }
//        if (patientDTO.getDateOfBirth() != null) {
//            existingPatient.setDateOfBirth(patientDTO.getDateOfBirth());
//        }
//        if (patientDTO.getGender() != null) {
//            existingPatient.setGender(patientDTO.getGender());
//        }
//        if (patientDTO.getAge() != null) {
//            existingPatient.setAge(patientDTO.getAge());
//        }
//        existingPatient.setUpdatedBy(username);
//    }
//
//    private void handleVisitUpdate(PatientEntity patient, Lab lab, VisitDTO visitDTO, String username) {
//        // Check if we're updating an existing visit or creating a new one
//        if (visitDTO.getVisitId() != null) {
//            // Update existing visit
//            Optional<VisitEntity> existingVisitOpt = patient.getVisits().stream()
//                    .filter(v -> v.getVisitId().equals(visitDTO.getVisitId()))
//                    .findFirst();
//
//            if (existingVisitOpt.isPresent()) {
//                VisitEntity existingVisit = existingVisitOpt.get();
//                updateVisitDetails(existingVisit, lab, visitDTO, username);
//            } else {
//                // Visit ID provided but not found - create new visit
//                VisitEntity newVisit = mapVisitDTOToEntity(visitDTO, lab, username);
//                newVisit.setPatient(patient);
//                patient.getVisits().add(newVisit);
//            }
//        } else {
//            // Create new visit
//            VisitEntity newVisit = mapVisitDTOToEntity(visitDTO, lab, username);
//            newVisit.setPatient(patient);
//            patient.getVisits().add(newVisit);
//        }
//    }
//
//    private void updateVisitDetails(VisitEntity visit, Lab lab, VisitDTO visitDTO, String username) {
//        // Update basic visit information
//        if (visitDTO.getVisitDate() != null) {
//            visit.setVisitDate(visitDTO.getVisitDate());
//        }
//        if (visitDTO.getVisitType() != null) {
//            visit.setVisitType(visitDTO.getVisitType());
//        }
//        if (visitDTO.getVisitStatus() != null) {
//            visit.setVisitStatus(visitDTO.getVisitStatus());
//        }
//        if (visitDTO.getVisitDescription() != null) {
//            visit.setVisitDescription(visitDTO.getVisitDescription());
//        }
//
//        // Handle cancellation fields
//        if (visitDTO.getVisitCancellationReason() != null) {
//            visit.setVisitCancellationReason(visitDTO.getVisitCancellationReason());
//        }
//        if (visitDTO.getVisitCancellationDate() != null) {
//            visit.setVisitCancellationDate(visitDTO.getVisitCancellationDate());
//        }
//        if (visitDTO.getVisitCancellationBy() != null) {
//            visit.setVisitCancellationBy(visitDTO.getVisitCancellationBy());
//        }
//        if (visitDTO.getVisitCancellationTime() != null) {
//            visit.setVisitCancellationTime(visitDTO.getVisitCancellationTime());
//        }
//
//        visit.setUpdatedBy(username);
//
//        // Handle doctor update
//        if (visitDTO.getDoctorId() != null) {
//            Optional<Doctors> doctorOpt = doctorRepository.findById(visitDTO.getDoctorId());
//            if (doctorOpt.isPresent()) {
//                Doctors doctor = doctorOpt.get();
//                if (!lab.getDoctors().contains(doctor)) {
//                    throw new RuntimeException("Doctor does not belong to the lab");
//                }
//                visit.setDoctor(doctor);
//            }
//        }
//
//        // Handle tests update with billing recalculation
//        if (visitDTO.getTestIds() != null) {
//            if (visitDTO.getTestIds().isEmpty()) {
//                // If empty list provided, remove all tests
//                Set<Test> oldTests = visit.getTests();
//                visit.setTests(new HashSet<>());
//
//                // Set all test results to CANCELLED
//                clearTestResultsForRemovedTests(visit, oldTests);
//
//                // Recalculate billing for removed tests
//                if (!oldTests.isEmpty()) {
//                    recalculateBillingForTestChanges(visit, lab, new HashSet<>(), oldTests, username);
//                }
//            } else {
//                // Update with new tests
//                List<Test> newTests = testRepository.findAllById(visitDTO.getTestIds());
//                if (newTests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
//                    throw new RuntimeException("Test does not belong to the lab");
//                }
//
//                // Calculate billing adjustments for test changes
//                Set<Test> oldTests = visit.getTests();
//                Set<Test> addedTests = new HashSet<>(newTests);
//                addedTests.removeAll(oldTests);
//
//                Set<Test> removedTests = new HashSet<>(oldTests);
//                removedTests.removeAll(newTests);
//
//                // Update tests
//                visit.setTests(new HashSet<>(newTests));
//
//                // Clear test results for removed tests and set status to CANCELLED
//                if (!removedTests.isEmpty()) {
//                    clearTestResultsForRemovedTests(visit, removedTests);
//                }
//
//                // Create test results for added tests with ACTIVE status
//                if (!addedTests.isEmpty()) {
//                    createTestResultsForAddedTests(visit, addedTests, username);
//                }
//
//                // Recalculate billing if there are test changes
//                if (!addedTests.isEmpty() || !removedTests.isEmpty()) {
//                    recalculateBillingForTestChanges(visit, lab, addedTests, removedTests, username);
//                }
//            }
//        }
//
//        // Handle packages update
//        if (visitDTO.getPackageIds() != null) {
//            List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
//            if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
//                throw new RuntimeException("Health package does not belong to the lab");
//            }
//            visit.setPackages(new HashSet<>(healthPackages));
//        }
//
//        // Handle insurance update
//        if (visitDTO.getInsuranceIds() != null) {
//            List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
//            if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
//                throw new RuntimeException("Insurance does not belong to the lab");
//            }
//            visit.setInsurance(new HashSet<>(insurance));
//        }
//
//        // Handle billing update
//        if (visitDTO.getBilling() != null) {
//            updateBillingDetails(visit, lab, visitDTO.getBilling(), visitDTO.getListOfEachTestDiscount(), username);
//        }
//    }
//
////    private void updateBillingDetails(VisitEntity visit, Lab lab, BillingDTO billingDTO, List<TestDiscountDTO> testDiscounts, String username) {
////        BillingEntity billing = visit.getBilling();
////        if (billing == null) {
////            billing = new BillingEntity();
////            visit.setBilling(billing);
////        }
////
////        // Update basic billing fields
////        if (billingDTO.getTotalAmount() != null) {
////            billing.setTotalAmount(billingDTO.getTotalAmount());
////        }
////        if (billingDTO.getPaymentStatus() != null) {
////            billing.setPaymentStatus(billingDTO.getPaymentStatus());
////        }
////        if (billingDTO.getPaymentMethod() != null) {
////            billing.setPaymentMethod(billingDTO.getPaymentMethod());
////        }
////        if (billingDTO.getPaymentDate() != null) {
////            billing.setPaymentDate(billingDTO.getPaymentDate());
////        }
////        if (billingDTO.getDiscount() != null) {
////            billing.setDiscount(billingDTO.getDiscount());
////        }
////        if (billingDTO.getNetAmount() != null) {
////            billing.setNetAmount(billingDTO.getNetAmount());
////        }
////        if (billingDTO.getDiscountReason() != null) {
////            billing.setDiscountReason(billingDTO.getDiscountReason());
////        }
////        // Note: receivedAmount and dueAmount are calculated by BillingManagementService
////        // Do not override them from DTO to avoid calculation conflicts
////
////        // Update audit fields
////        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
////        billing.setBillingDate(LocalDate.now().toString());
////        billing.setUpdatedBy(username);
////
////        billing.getLabs().add(lab);
////        billing = billingRepository.save(billing);
////
////        // Handle transactions update - Use BillingManagementService for proper payment and refund handling
////        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
////            // Process each transaction through BillingManagementService to ensure proper refund calculation
////            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
////                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO;
////
////                // Only process payments (receivedAmount > 0)
////                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
////                    // Use BillingManagementService to handle payment and potential refunds
////                    billing = billingManagementService.addPayment(
////                            billing.getId(),
////                            receivedAmount,
////                            transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH",
////                            transactionDTO.getUpiId(),
////                            transactionDTO.getUpiAmount(),
////                            transactionDTO.getCardAmount(),
////                            transactionDTO.getCashAmount(),
////                            username
////                    );
////                }
////            }
////        }
////
////        // Update due amounts in existing transactions to maintain data consistency
////        if (billing.getId() != null) {
////            billingManagementService.updateDueAmountsInAllTransactions(billing.getId(), username);
////        }
////
////        // Handle test discounts - ALWAYS clear existing and set new ones
////        if (testDiscounts != null) {
////            // Always remove existing discounts for this billing
////            testDiscountRepository.deleteByBilling(billing);
////
////            // Clear from billing entity as well
////            billing.getTestDiscounts().clear();
////
////            // Add new discounts if provided
////            if (!testDiscounts.isEmpty()) {
////                BillingEntity finalBilling1 = billing;
////                Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
////                        .map(discountDTO -> {
////                            TestDiscountEntity discountEntity = new TestDiscountEntity();
////                            discountEntity.setTestId(discountDTO.getTestId());
////                            discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
////                            discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
////                            discountEntity.setFinalPrice(discountDTO.getFinalPrice());
////                            discountEntity.setCreatedBy(username);
////                            discountEntity.setUpdatedBy(username);
////                            discountEntity.setBilling(finalBilling1);
////                            return discountEntity;
////                        })
////                        .collect(Collectors.toSet());
////                testDiscountRepository.saveAll(discountEntities);
////            }
////        }
////    }
//
//    //==================
//
//    /**
//     * FIXED: Update billing details with discount recalculation
//     */
//    private void updateBillingDetails(VisitEntity visit, Lab lab, BillingDTO billingDTO, List<TestDiscountDTO> testDiscounts, String username) {
//        BillingEntity billing = visit.getBilling();
//        if (billing == null) {
//            billing = new BillingEntity();
//            visit.setBilling(billing);
//        }
//
//        // Store old values for comparison
//        BigDecimal oldDiscount = safeGetAmount(billing.getDiscount());
//        BigDecimal oldNetAmount = safeGetAmount(billing.getNetAmount());
//        BigDecimal oldTotalAmount = safeGetAmount(billing.getTotalAmount());
//
//        // Update basic billing fields
//        if (billingDTO.getTotalAmount() != null) {
//            billing.setTotalAmount(billingDTO.getTotalAmount());
//        }
//        if (billingDTO.getPaymentStatus() != null) {
//            billing.setPaymentStatus(billingDTO.getPaymentStatus());
//        }
//        if (billingDTO.getPaymentMethod() != null) {
//            billing.setPaymentMethod(billingDTO.getPaymentMethod());
//        }
//        if (billingDTO.getPaymentDate() != null) {
//            billing.setPaymentDate(billingDTO.getPaymentDate());
//        }
//
//        // FIXED: Handle discount changes with recalculation
//        if (billingDTO.getDiscount() != null) {
//            billing.setDiscount(billingDTO.getDiscount());
//        }
//
//        if (billingDTO.getNetAmount() != null) {
//            billing.setNetAmount(billingDTO.getNetAmount());
//        }
//
//        if (billingDTO.getDiscountReason() != null) {
//            billing.setDiscountReason(billingDTO.getDiscountReason());
//        }
//
//        // FIXED: Recalculate net amount if discount changed but net amount not provided
//        if (billingDTO.getDiscount() != null && billingDTO.getNetAmount() == null) {
//            BigDecimal totalAmount = safeGetAmount(billing.getTotalAmount());
//            BigDecimal newDiscount = safeGetAmount(billingDTO.getDiscount());
//            BigDecimal newNetAmount = totalAmount.subtract(newDiscount);
//            billing.setNetAmount(newNetAmount);
//
//            logger.info("FIXED: Recalculated net amount - Total: {}, Discount: {}, New Net: {}",
//                    totalAmount, newDiscount, newNetAmount);
//        }
//
//        // Update audit fields
//        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
//        billing.setBillingDate(LocalDate.now().toString());
//        billing.setUpdatedBy(username);
//
//        billing.getLabs().add(lab);
//        billing = billingRepository.save(billing);
//
//        // FIXED: Recalculate billing if discount changed significantly
//        BigDecimal newDiscount = safeGetAmount(billing.getDiscount());
//        BigDecimal newNetAmount = safeGetAmount(billing.getNetAmount());
//
//        if (oldDiscount.compareTo(newDiscount) != 0) {
//            logger.info("FIXED: Discount changed from {} to {}, triggering recalculation", oldDiscount, newDiscount);
//            billingManagementService.updateBillingAfterDiscountChange(billing.getId(), newNetAmount, username);
//        }
//
//        // Handle transactions through BillingManagementService
//        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
//            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
//                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO;
//
//                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
//                    billing = billingManagementService.addPayment(
//                            billing.getId(),
//                            receivedAmount,
//                            transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH",
//                            transactionDTO.getUpiId(),
//                            transactionDTO.getUpiAmount(),
//                            transactionDTO.getCardAmount(),
//                            transactionDTO.getCashAmount(),
//                            username
//                    );
//                }
//            }
//        }
//
//        // Handle test discounts
//        if (testDiscounts != null) {
//            testDiscountRepository.deleteByBilling(billing);
//            billing.getTestDiscounts().clear();
//
//            if (!testDiscounts.isEmpty()) {
//                BillingEntity finalBilling = billing;
//                Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
//                        .map(discountDTO -> {
//                            TestDiscountEntity discountEntity = new TestDiscountEntity();
//                            discountEntity.setTestId(discountDTO.getTestId());
//                            discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
//                            discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
//                            discountEntity.setFinalPrice(discountDTO.getFinalPrice());
//                            discountEntity.setCreatedBy(username);
//                            discountEntity.setUpdatedBy(username);
//                            discountEntity.setBilling(finalBilling);
//                            return discountEntity;
//                        })
//                        .collect(Collectors.toSet());
//                testDiscountRepository.saveAll(discountEntities);
//            }
//        }
//    }
//
//
//    //===========
//
//    private VisitEntity mapVisitDTOToEntity(VisitDTO visitDTO, Lab lab, String currentUser) {
//        VisitEntity visit = new VisitEntity();
//        visit.setVisitDate(visitDTO.getVisitDate());
//        visit.setVisitType(visitDTO.getVisitType());
//        visit.setVisitStatus(visitDTO.getVisitStatus());
//        visit.setVisitDescription(visitDTO.getVisitDescription());
//
//        //----new fields for cancellation
//        visit.setVisitCancellationReason(visitDTO.getVisitCancellationReason());
//        visit.setVisitCancellationDate(visitDTO.getVisitCancellationDate());
//        visit.setVisitCancellationBy(visitDTO.getVisitCancellationBy());
//        visit.setVisitCancellationTime(visitDTO.getVisitCancellationTime());
//        visit.setCreatedBy(currentUser);
//        LocalDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
//        visit.setVisitTime(currentDateTime); // Set visit time to current date and time
//        if (visitDTO.getDoctorId() != null) {
//            Optional<Doctors> doctorOpt = doctorRepository.findById(visitDTO.getDoctorId());
//            if (doctorOpt.isPresent()) {
//                Doctors doctor = doctorOpt.get();
//                if (!lab.getDoctors().contains(doctor)) {
//                    throw new RuntimeException("Doctor does not belong to the lab");
//                }
//                visit.setDoctor(doctor);
//            }
//        }
//        if (visitDTO.getTestIds() != null && !visitDTO.getTestIds().isEmpty()) {
//            List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
//            if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
//                throw new RuntimeException("Test does not belong to the lab");
//            }
//            visit.setTests(new HashSet<>(tests));
//        }
//        if (visitDTO.getPackageIds() != null && !visitDTO.getPackageIds().isEmpty()) {
//            List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
//            if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
//                throw new RuntimeException("Health package does not belong to the lab");
//            }
//            visit.setPackages(new HashSet<>(healthPackages));
//        }
//        if (visitDTO.getInsuranceIds() != null && !visitDTO.getInsuranceIds().isEmpty()) {
//            List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
//            if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
//                throw new RuntimeException("Insurance does not belong to the lab");
//            }
//            visit.setInsurance(new HashSet<>(insurance));
//        }
//        visit.getLabs().add(lab);
//        // handle billing
//        if (visitDTO.getBilling() != null) {
//            BillingEntity billing = mapBillingDTOToEntity(visitDTO.getBilling(), lab, currentUser);
//            billing = billingRepository.save(billing); // 💥 Save billing before using it in discounts
//            visit.setBilling(billing);
//            if (visitDTO.getListOfEachTestDiscount() != null && !visitDTO.getListOfEachTestDiscount().isEmpty()) {
//                BillingEntity finalBilling = billing;
//                Set<TestDiscountEntity> discountEntities = visitDTO.getListOfEachTestDiscount().stream()
//                        .map(discountDTO -> {
//                            TestDiscountEntity discountEntity = new TestDiscountEntity();
//                            discountEntity.setTestId(discountDTO.getTestId());
//                            discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
//                            discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
//                            discountEntity.setFinalPrice(discountDTO.getFinalPrice());
//                            discountEntity.setCreatedBy(currentUser);
//                            discountEntity.setUpdatedBy(currentUser);
//                            discountEntity.setBilling(finalBilling); // now managed
//                            return discountEntity;
//                        })
//                        .collect(Collectors.toSet());
//                testDiscountRepository.saveAll(discountEntities);
//            }
//        }
//        // handle visit TestResults
//        if (visitDTO.getTestResult() != null && !visitDTO.getTestResult().isEmpty()) {
//            Set<VisitTestResult> testResults = visitDTO.getTestResult().stream()
//                    .map(testResultDTO -> {
//                        VisitTestResult testResult = new VisitTestResult();
//                        // Set visit reference
//                        testResult.setVisit(visit);
//                        // Set isFilled only if provided in DTO, else stays false
//                        if (testResultDTO.getIsFilled() != null) {
//                            testResult.setIsFilled(testResultDTO.getIsFilled());
//                            testResult.setReportStatus(testResultDTO.getReportStatus() != null
//                                    ? testResultDTO.getReportStatus()
//                                    : "PENDING");
//                        }
//                        // Fetch and set test entity
//                        testResult.setTest(
//                                testResultDTO.getTestId() != null
//                                        ? testRepository.findById(testResultDTO.getTestId())
//                                        .orElseThrow(() -> new EntityNotFoundException(
//                                                "Test not found with ID: " + testResultDTO.getTestId()))
//                                        : null
//                        );
//                        // Set audit fields
//                        testResult.setCreatedBy(currentUser);
//                        testResult.setUpdatedBy(currentUser);
//
//                        return testResult;
//                    })
//                    .collect(Collectors.toSet());
//            visit.setTestResults(testResults);
//        }
//        return visit;
//    }
//
//    /**
//     * Clears test results for removed tests and sets status to CANCELLED
//     */
//    private void clearTestResultsForRemovedTests(VisitEntity visit, Set<Test> removedTests) {
//        if (removedTests != null && !removedTests.isEmpty()) {
//            Set<Long> removedTestIds = removedTests.stream()
//                    .map(Test::getId)
//                    .collect(Collectors.toSet());
//
//            // Set status to CANCELLED for removed tests instead of deleting
//            visit.getTestResults().stream()
//                    .filter(testResult -> testResult.getTest() != null &&
//                            removedTestIds.contains(testResult.getTest().getId()))
//                    .forEach(testResult -> {
//                        testResult.setTestStatus("CANCELLED");
////                    testResult.setReportStatus("CANCELLED");        //CANCELLED
//                        testResult.setReportStatus("Pending");        //CANCELLED
//                        testResult.setIsFilled(false);
//                    });
//        }
//    }
//
//    /**
//     * Creates test results for added tests with ACTIVE status
//     */
//    private void createTestResultsForAddedTests(VisitEntity visit, Set<Test> addedTests, String username) {
//        if (addedTests != null && !addedTests.isEmpty()) {
//            Set<VisitTestResult> newTestResults = addedTests.stream()
//                    .map(test -> {
//                        VisitTestResult testResult = new VisitTestResult();
//                        testResult.setVisit(visit);
//                        testResult.setTest(test);
//                        testResult.setTestStatus("ACTIVE");
//                        testResult.setReportStatus("Pending");
//                        testResult.setIsFilled(false);
//                        testResult.setCreatedBy(username);
//                        testResult.setUpdatedBy(username);
//                        return testResult;
//                    })
//                    .collect(Collectors.toSet());
//
//            visit.getTestResults().addAll(newTestResults);
//        }
//    }
//
//    /**
//     * Recalculates billing when tests are added or removed
//     * Uses the comprehensive BillingManagementService for proper business logic
//     */
////    private void recalculateBillingForTestChanges(VisitEntity visit, Lab lab, Set<Test> addedTests, Set<Test> removedTests, String username) {
////        BillingEntity billing = visit.getBilling();
////        if (billing == null) {
////            billing = new BillingEntity();
////            visit.setBilling(billing);
////        }
////
////        // Calculate amounts for added tests
////        BigDecimal addedAmount = addedTests.stream()
////                .map(Test::getPrice)
////                .reduce(BigDecimal.ZERO, BigDecimal::add);
////
////        // Calculate amounts for removed tests
////        BigDecimal removedAmount = removedTests.stream()
////                .map(Test::getPrice)
////                .reduce(BigDecimal.ZERO, BigDecimal::add);
////
////        // Get current total amount
////        BigDecimal currentTotal = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
////
////        // Calculate new total amount
////        BigDecimal newTotalAmount = currentTotal.add(addedAmount).subtract(removedAmount);
////        billing.setTotalAmount(newTotalAmount);
////
////        // Update net amount (assuming no discount changes for now)
////        BigDecimal currentDiscount = billing.getDiscount() != null ? billing.getDiscount() : BigDecimal.ZERO;
////        BigDecimal newNetAmount = newTotalAmount.subtract(currentDiscount);
////
////        //update the actual received amount before recalculation
////
////        // Update audit fields
////        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
////        billing.setBillingDate(LocalDate.now().toString());
////        billing.setUpdatedBy(username);
////        billing.getLabs().add(lab);
////
////        // Save billing first
////        billing = billingRepository.save(billing);
////
////        // Use the comprehensive billing service for proper recalculation
////        if (billing.getId() != null) {
////            // For test additions/removals, we need to recalculate considering existing payments and refunds
////            recalculateBillingForTestModifications(billing, newNetAmount, username);
////        }
////    }
//
//
//    // In UpdatePatientService class - replace the problematic methods:
//
//    /**
//     * FIXED: Recalculates billing when tests are added or removed
//     */
//    private void recalculateBillingForTestChanges(VisitEntity visit, Lab lab, Set<Test> addedTests, Set<Test> removedTests, String username) {
//        BillingEntity billing = visit.getBilling();
//        if (billing == null) {
//            billing = new BillingEntity();
//            visit.setBilling(billing);
//        }
//
//        // Calculate amounts for added/removed tests
//        BigDecimal addedAmount = addedTests.stream()
//                .map(Test::getPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal removedAmount = removedTests.stream()
//                .map(Test::getPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal currentTotal = safeGetAmount(billing.getTotalAmount());
//        BigDecimal newTotalAmount = currentTotal.add(addedAmount).subtract(removedAmount);
//
//        // FIXED: Use net amount for calculation (consider discounts)
//        BigDecimal currentDiscount = safeGetAmount(billing.getDiscount());
//        BigDecimal newNetAmount = newTotalAmount.subtract(currentDiscount);
//
//        // Update basic billing fields
//        billing.setTotalAmount(newTotalAmount);
//        billing.setNetAmount(newNetAmount);
//        billing.setUpdatedBy(username);
//        billing.getLabs().add(lab);
//
//        // Save billing first
//        billing = billingRepository.save(billing);
//
//        // FIXED: Use the proper billing service for recalculation
//        if (billing.getId() != null) {
//            billingManagementService.updateBillingAfterCancellation(billing.getId(), newNetAmount, username);
//        }
//    }
//
///**
// * FIXED: Remove the problematic recalculateBillingForTestModifications method
// * and use the service instead
// */
//
//    /**
//     * FIXED: Safe amount helper
//     */
//    private BigDecimal safeGetAmount(BigDecimal amount) {
//        return amount != null ? amount : BigDecimal.ZERO;
//    }
//
//    /**
//     * Recalculates billing when tests are added or removed, following the specification:
//     * - ARA = sum of payments - sum of refunds
//     * - Due = Total Amount - ARA
//     * - Refund = sum of removed test amounts
//     */
//    private void recalculateBillingForTestModifications(BillingEntity billing, BigDecimal newNetAmount, String username) {
//        // Get current received amount (total payments made)
//        BigDecimal currentReceivedAmount = billing.getReceivedAmount() != null ? billing.getReceivedAmount() : BigDecimal.ZERO;
//
//        // Calculate total refunded amount from existing transactions
//        BigDecimal totalRefundedAmount = BigDecimal.ZERO;
//        if (billing.getTransactions() != null) {
//            totalRefundedAmount = billing.getTransactions().stream()
//                    .map(transaction -> transaction.getRefundAmount() != null ? transaction.getRefundAmount() : BigDecimal.ZERO)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//        }
//
//        // Calculate ARA = sum of payments - sum of refunds
//        BigDecimal actualReceivedAmount = currentReceivedAmount.subtract(totalRefundedAmount);
//
//        // Calculate the difference between new net amount and previous net amount
//        BigDecimal previousNetAmount = billing.getNetAmount() != null ? billing.getNetAmount() : BigDecimal.ZERO;
//        BigDecimal netAmountDifference = newNetAmount.subtract(previousNetAmount);
//
//        // Handle refunds for removed tests
//        BigDecimal refundAmount = BigDecimal.ZERO;
//        if (netAmountDifference.compareTo(BigDecimal.ZERO) < 0) {
//            // Tests were removed, create refund for the removed test amount
//            refundAmount = netAmountDifference.abs();
//
//            // Create refund transaction for the cancelled test
//            createRefundTransactionForCancellation(billing, refundAmount, username);
//
//            // Update ARA after refund: ARA = ARA - refund
//            actualReceivedAmount = actualReceivedAmount.subtract(refundAmount);
//        }
//
//        // Cap ARA to not exceed Total Amount
//        if (actualReceivedAmount.compareTo(newNetAmount) > 0) {
//            actualReceivedAmount = newNetAmount;
//        }
//
//        // Update actualReceivedAmount
//        billing.setActualReceivedAmount(actualReceivedAmount);
//
//        // Calculate due amount: Due = Total Amount - ARA
//        BigDecimal newDueAmount = newNetAmount.subtract(actualReceivedAmount);
//
//        // DEBUG: Log the calculation details
//        System.out.println("=== BILLING RECALCULATION DEBUG ===");
//        System.out.println("Billing ID: " + billing.getId());
//        System.out.println("New Net Amount: " + newNetAmount);
//        System.out.println("Previous Net Amount: " + previousNetAmount);
//        System.out.println("Net Amount Difference: " + netAmountDifference);
//        System.out.println("Current Received Amount: " + currentReceivedAmount);
//        System.out.println("Total Refunded Amount: " + totalRefundedAmount);
//        System.out.println("Actual Received Amount (ARA): " + actualReceivedAmount);
//        System.out.println("Refund Amount: " + refundAmount);
//        System.out.println("Calculated Due Amount: " + newDueAmount);
//        System.out.println("=====================================");
//
//        // Determine payment status
//        String newPaymentStatus;
//        if (newDueAmount.compareTo(BigDecimal.ZERO) > 0) {
//            // Still owe money
//            newPaymentStatus = actualReceivedAmount.compareTo(BigDecimal.ZERO) > 0 ? "PARTIALLY_PAID" : "UNPAID";
//        } else if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
//            // Paid in full
//            newPaymentStatus = "PAID";
//        } else {
//            // Overpaid - this shouldn't happen with proper ARA capping, but handle it
//            newDueAmount = BigDecimal.ZERO;
//            newPaymentStatus = "PAID";
//        }
//
//        // Update billing fields
//        billing.setNetAmount(newNetAmount);
//        billing.setDueAmount(newDueAmount);
//        billing.setPaymentStatus(newPaymentStatus);
//        billing.setUpdatedBy(username);
//
//        // Save the updated billing
//        billingRepository.save(billing);
//
//        // Update due amounts in existing transactions to maintain consistency
//        if (billing.getTransactions() != null) {
//            for (TransactionEntity transaction : billing.getTransactions()) {
//                transaction.setDueAmount(newDueAmount);
//            }
//            transactionRepository.saveAll(billing.getTransactions());
//        }
//    }
//
//    /**
//     * Creates a refund transaction for test cancellation overpayment
//     */
//    private void createRefundTransactionForCancellation(BillingEntity billing, BigDecimal refundAmount, String username) {
//        System.out.println("=== CREATING REFUND TRANSACTION FOR CANCELLATION ===");
//        System.out.println("Billing ID: " + billing.getId());
//        System.out.println("Refund Amount: " + refundAmount);
//        System.out.println("==================================================");
//
//        TransactionEntity refundTransaction = new TransactionEntity();
//        refundTransaction.setBilling(billing);
//        refundTransaction.setPaymentMethod("REFUND");
//        refundTransaction.setRefundAmount(refundAmount);
//        refundTransaction.setReceivedAmount(BigDecimal.ZERO);
//        refundTransaction.setDueAmount(BigDecimal.ZERO);
//        refundTransaction.setPaymentDate(LocalDate.now().toString());
//        refundTransaction.setRemarks("Refund for test cancellation");
//        refundTransaction.setCreatedBy(username != null ? username : "SYSTEM");
//        refundTransaction.setCreatedAt(LocalDateTime.now());
//
//        // Set payment method amounts to zero for refund
//        refundTransaction.setUpiId("");
//        refundTransaction.setUpiAmount(BigDecimal.ZERO);
//        refundTransaction.setCardAmount(BigDecimal.ZERO);
//        refundTransaction.setCashAmount(BigDecimal.ZERO);
//
//        // Add to billing's transaction list
//        billing.getTransactions().add(refundTransaction);
//
//        // Save transaction
//        transactionRepository.save(refundTransaction);
//
//        System.out.println("Refund transaction created successfully - TransactionId: " + refundTransaction.getId());
//    }
//
//
//    /**
//     * Maps BillingDTO to BillingEntity
//     */
//    private BillingEntity mapBillingDTOToEntity(BillingDTO billingDTO, Lab lab, String currentUser) {
//        BillingEntity billing = billingDTO.getBillingId() != null ?
//                billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity()) :
//                new BillingEntity();
//
//        // Map basic billing fields with null checks and defaults
//        billing.setTotalAmount(billingDTO.getTotalAmount() != null ? billingDTO.getTotalAmount() : BigDecimal.ZERO);
//        billing.setPaymentStatus(billingDTO.getPaymentStatus() != null ? billingDTO.getPaymentStatus() : "PAID");
//        billing.setPaymentMethod(billingDTO.getPaymentMethod() != null ? billingDTO.getPaymentMethod() : "CASH");
//        billing.setPaymentDate(billingDTO.getPaymentDate() != null ? billingDTO.getPaymentDate() : LocalDate.now().toString());
//        billing.setDiscount(billingDTO.getDiscount() != null ? billingDTO.getDiscount() : BigDecimal.ZERO);
//        billing.setGstRate(billingDTO.getGstRate() != null ? billingDTO.getGstRate() : BigDecimal.ZERO);
//        billing.setGstAmount(billingDTO.getGstAmount() != null ? billingDTO.getGstAmount() : BigDecimal.ZERO);
//        billing.setCgstAmount(billingDTO.getCgstAmount() != null ? billingDTO.getCgstAmount() : BigDecimal.ZERO);
//        billing.setSgstAmount(billingDTO.getSgstAmount() != null ? billingDTO.getSgstAmount() : BigDecimal.ZERO);
//        billing.setIgstAmount(billingDTO.getIgstAmount() != null ? billingDTO.getIgstAmount() : BigDecimal.ZERO);
//        billing.setNetAmount(billingDTO.getNetAmount() != null ? billingDTO.getNetAmount() : BigDecimal.ZERO);
//        billing.setDiscountReason(billingDTO.getDiscountReason());
//        // Note: receivedAmount and dueAmount are calculated by BillingManagementService
//        // Initialize with zero values, will be updated by BillingManagementService
//        billing.setReceivedAmount(BigDecimal.ZERO);
//        // Due amount will be calculated based on actualReceivedAmount by BillingManagementService
//        billing.setDueAmount(billingDTO.getNetAmount() != null ? billingDTO.getNetAmount() : BigDecimal.ZERO);
//
//        // Audit fields
//        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
//        billing.setBillingDate(LocalDate.now().toString());
//        billing.setCreatedBy(currentUser);
//        billing.setUpdatedBy(currentUser);
//
//        // Keep existing transactions - do not clear them
//
//        // Add lab association (avoid duplicates)
//        billing.getLabs().add(lab);
//        billing = billingRepository.save(billing);
//
//        // Handle transactions - Use BillingManagementService for proper payment and refund handling
//        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
//            // Process each transaction through BillingManagementService to ensure proper refund calculation
//            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
//                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO;
//
//                // Only process payments (receivedAmount > 0)
//                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
//                    // Use BillingManagementService to handle payment and potential refunds
//                    billing = billingManagementService.addPayment(
//                            billing.getId(),
//                            receivedAmount,
//                            transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH",
//                            transactionDTO.getUpiId(),
//                            transactionDTO.getUpiAmount(),
//                            transactionDTO.getCardAmount(),
//                            transactionDTO.getCashAmount(),
//                            currentUser
//                    );
//                }
//            }
//        }
//
//        return billing;
//    }
//
//}








//------------------ in line discount handle in this code

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
import org.slf4j.Logger;

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
    private final TransactionRepository transactionRepository;
    private final BillingManagementService billingManagementService;
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UpdatePatientService.class);

    public UpdatePatientService(PatientRepository patientRepository, DoctorRepository doctorRepository, TestRepository testRepository, HealthPackageRepository healthPackageRepository, InsuranceRepository insuranceRepository, BillingRepository billingRepository, TestDiscountRepository testDiscountRepository, VisitRepository visitRepository, LabRepository labRepository, TransactionRepository transactionRepository, BillingManagementService billingManagementService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.insuranceRepository = insuranceRepository;
        this.billingRepository = billingRepository;
        this.testDiscountRepository = testDiscountRepository;
        this.visitRepository = visitRepository;
        this.labRepository = labRepository;
        this.transactionRepository = transactionRepository;
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
        if (patientDTO.getFirstName() != null) {
            existingPatient.setFirstName(patientDTO.getFirstName());
        }
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

        // Handle billing update - FIXED: Now properly handles inline discounts
        if (visitDTO.getBilling() != null) {
            updateBillingDetails(visit, lab, visitDTO.getBilling(), visitDTO.getListOfEachTestDiscount(), username);
        }
    }

    /**
     * FIXED: Update billing details with proper inline discount handling
     */
    private void updateBillingDetails(VisitEntity visit, Lab lab, BillingDTO billingDTO,
                                      List<TestDiscountDTO> testDiscounts, String username) {
        BillingEntity billing = visit.getBilling();
        if (billing == null) {
            billing = new BillingEntity();
            visit.setBilling(billing);
        }

        // Store old values for comparison
        BigDecimal oldTotalAmount = safeGetAmount(billing.getTotalAmount());
        BigDecimal oldNetAmount = safeGetAmount(billing.getNetAmount());
        BigDecimal oldDiscount = safeGetAmount(billing.getDiscount());

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

        // FIXED: Handle discount and net amount updates with proper logic
        boolean hasInlineDiscounts = testDiscounts != null && !testDiscounts.isEmpty();
        boolean hasGlobalDiscount = billingDTO.getDiscount() != null && billingDTO.getDiscount().compareTo(BigDecimal.ZERO) > 0;

        BigDecimal newTotalAmount = safeGetAmount(billing.getTotalAmount());
        BigDecimal newNetAmount = billingDTO.getNetAmount();
        BigDecimal newDiscount = billingDTO.getDiscount();

        logger.info("FIXED: Billing update - HasInlineDiscounts: {}, HasGlobalDiscount: {}",
                hasInlineDiscounts, hasGlobalDiscount);

        if (hasInlineDiscounts) {
            // FIXED: Handle inline discounts - calculate net amount from test discounts
            BigDecimal totalInlineDiscount = calculateTotalInlineDiscount(testDiscounts);
            newNetAmount = newTotalAmount.subtract(totalInlineDiscount);
            newDiscount = totalInlineDiscount;

            logger.info("FIXED: Inline discount calculation - TotalAmount: {}, InlineDiscount: {}, CalculatedNet: {}",
                    newTotalAmount, totalInlineDiscount, newNetAmount);
        } else if (hasGlobalDiscount && newNetAmount == null) {
            // FIXED: Handle global discount when net amount is not provided
            newNetAmount = newTotalAmount.subtract(safeGetAmount(newDiscount));
            logger.info("FIXED: Global discount calculation - TotalAmount: {}, Discount: {}, CalculatedNet: {}",
                    newTotalAmount, newDiscount, newNetAmount);
        } else if (newNetAmount == null) {
            // No discounts, net amount equals total amount
            newNetAmount = newTotalAmount;
            newDiscount = BigDecimal.ZERO;
        }

        // Set the calculated values
        billing.setNetAmount(newNetAmount);
        billing.setDiscount(newDiscount != null ? newDiscount : BigDecimal.ZERO);

        if (billingDTO.getDiscountReason() != null) {
            billing.setDiscountReason(billingDTO.getDiscountReason());
        }

        // Update audit fields
        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
        billing.setBillingDate(LocalDate.now().toString());
        billing.setUpdatedBy(username);

        billing.getLabs().add(lab);
        billing = billingRepository.save(billing);

        // FIXED: Use appropriate billing service method based on discount type
        BigDecimal finalNewNetAmount = newNetAmount;
        BigDecimal finalNewTotalAmount = newTotalAmount;

        if (hasInlineDiscounts) {
            // Use inline discount method which handles both total and net amounts
            billingManagementService.updateBillingAfterInlineDiscountChange(
                    billing.getId(), finalNewNetAmount, finalNewTotalAmount, username);
        } else if (oldNetAmount.compareTo(finalNewNetAmount) != 0) {
            // Use global discount method for net amount changes
            billingManagementService.updateBillingAfterDiscountChange(
                    billing.getId(), finalNewNetAmount, username);
        }

        // Handle transactions through BillingManagementService
        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ?
                        transactionDTO.getReceivedAmount() : BigDecimal.ZERO;

                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    billing = billingManagementService.addPayment(
                            billing.getId(),
                            receivedAmount,
                            transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH",
                            transactionDTO.getUpiId(),
                            transactionDTO.getUpiAmount(),
                            transactionDTO.getCardAmount(),
                            transactionDTO.getCashAmount(),
                            username
                    );
                }
            }
        }

        // FIXED: Handle test discounts - update after billing is saved
        if (testDiscounts != null) {
            // Always remove existing discounts for this billing
            testDiscountRepository.deleteByBilling(billing);
            billing.getTestDiscounts().clear();

            if (!testDiscounts.isEmpty()) {
                BillingEntity finalBilling = billing;
                Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
                        .map(discountDTO -> {
                            TestDiscountEntity discountEntity = new TestDiscountEntity();
                            discountEntity.setTestId(discountDTO.getTestId());
                            discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
                            discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
                            discountEntity.setFinalPrice(discountDTO.getFinalPrice());
                            discountEntity.setCreatedBy(username);
                            discountEntity.setUpdatedBy(username);
                            discountEntity.setBilling(finalBilling);
                            return discountEntity;
                        })
                        .collect(Collectors.toSet());
                testDiscountRepository.saveAll(discountEntities);

                logger.info("FIXED: Saved {} inline test discounts for billing {}",
                        discountEntities.size(), billing.getId());
            }
        }

        logger.info("FIXED: Billing update completed - Total: {}, Net: {}, Discount: {}",
                newTotalAmount, newNetAmount, newDiscount);
    }

    /**
     * FIXED: Calculate total inline discount from test discounts
     */
    private BigDecimal calculateTotalInlineDiscount(List<TestDiscountDTO> testDiscounts) {
        if (testDiscounts == null || testDiscounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDiscount = testDiscounts.stream()
                .map(discount -> safeGetAmount(discount.getDiscountAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.debug("FIXED: Total inline discount calculated: {}", totalDiscount);
        return totalDiscount;
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
        Instant currentDateTime = Instant.now();
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
                        testResult.setReportStatus("Pending");
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
     * FIXED: Recalculates billing when tests are added or removed
     */
    private void recalculateBillingForTestChanges(VisitEntity visit, Lab lab, Set<Test> addedTests, Set<Test> removedTests, String username) {
        BillingEntity billing = visit.getBilling();
        if (billing == null) {
            billing = new BillingEntity();
            visit.setBilling(billing);
        }

        // Calculate amounts for added/removed tests
        BigDecimal addedAmount = addedTests.stream()
                .map(Test::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal removedAmount = removedTests.stream()
                .map(Test::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentTotal = safeGetAmount(billing.getTotalAmount());
        BigDecimal newTotalAmount = currentTotal.add(addedAmount).subtract(removedAmount);

        // FIXED: Consider existing discounts when calculating new net amount
        BigDecimal currentDiscount = safeGetAmount(billing.getDiscount());
        BigDecimal newNetAmount = newTotalAmount.subtract(currentDiscount);

        // Update basic billing fields
        billing.setTotalAmount(newTotalAmount);
        billing.setNetAmount(newNetAmount);
        billing.setUpdatedBy(username);
        billing.getLabs().add(lab);

        // Save billing first
        billing = billingRepository.save(billing);

        // FIXED: Use the proper billing service for recalculation
        if (billing.getId() != null) {
            billingManagementService.updateBillingAfterCancellation(billing.getId(), newNetAmount, username);
        }
    }

    /**
     * FIXED: Safe amount helper
     */
    private BigDecimal safeGetAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
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
        // Note: receivedAmount and dueAmount are calculated by BillingManagementService
        // Initialize with zero values, will be updated by BillingManagementService
        billing.setReceivedAmount(BigDecimal.ZERO);
        // Due amount will be calculated based on actualReceivedAmount by BillingManagementService
        billing.setDueAmount(billingDTO.getNetAmount() != null ? billingDTO.getNetAmount() : BigDecimal.ZERO);

        // Audit fields
        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
        billing.setBillingDate(LocalDate.now().toString());
        billing.setCreatedBy(currentUser);
        billing.setUpdatedBy(currentUser);

        // Keep existing transactions - do not clear them

        // Add lab association (avoid duplicates)
        billing.getLabs().add(lab);
        billing = billingRepository.save(billing);

        // Handle transactions - Use BillingManagementService for proper payment and refund handling
        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
            // Process each transaction through BillingManagementService to ensure proper refund calculation
            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
                BigDecimal receivedAmount = transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO;

                // Only process payments (receivedAmount > 0)
                if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Use BillingManagementService to handle payment and potential refunds
                    billing = billingManagementService.addPayment(
                            billing.getId(),
                            receivedAmount,
                            transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH",
                            transactionDTO.getUpiId(),
                            transactionDTO.getUpiAmount(),
                            transactionDTO.getCardAmount(),
                            transactionDTO.getCashAmount(),
                            currentUser
                    );
                }
            }
        }

        return billing;
    }
}