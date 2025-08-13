//package tiameds.com.tiameds.services.lab;
//
//import jakarta.transaction.Transactional;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import tiameds.com.tiameds.dto.lab.*;
//import tiameds.com.tiameds.entity.*;
//import tiameds.com.tiameds.repository.*;
//import tiameds.com.tiameds.utils.ApiResponseHelper;
//
//import java.math.BigDecimal;
//import java.time.*;
//import java.util.*;
//import java.util.stream.Collectors;
//
//import static java.util.Arrays.stream;
//
//@Service
//public class PatientService {
//    private final LabRepository labRepository;
//    private final TestRepository testRepository;
//    private final HealthPackageRepository healthPackageRepository;
//    private final PatientRepository patientRepository;
//    private final DoctorRepository doctorRepository;
//    private final HealthPackageRepository packageRepository;
//    private final InsuranceRepository insuranceRepository;
//    private final BillingRepository billingRepository;
//    private final TestDiscountRepository testDiscountRepository;
//    private final VisitRepository visitRepository;
//
//    public PatientService(LabRepository labRepository,
//                          TestRepository testRepository,
//                          HealthPackageRepository healthPackageRepository,
//                          PatientRepository patientRepository,
//                          DoctorRepository doctorRepository,
//                          HealthPackageRepository packageRepository,
//                          InsuranceRepository insuranceRepository,
//                          BillingRepository billingRepository,
//                          TestDiscountRepository testDiscountRepository, VisitRepository visitRepository) {
//        this.labRepository = labRepository;
//        this.testRepository = testRepository;
//        this.healthPackageRepository = healthPackageRepository;
//        this.patientRepository = patientRepository;
//        this.doctorRepository = doctorRepository;
//        this.packageRepository = packageRepository;
//        this.insuranceRepository = insuranceRepository;
//        this.billingRepository = billingRepository;
//        this.testDiscountRepository = testDiscountRepository;
//        this.visitRepository = visitRepository;
//    }
//
//    public boolean existsByPhone(String phone) {
//        return patientRepository.existsByPhone(phone);
//    }
//
//    public List<PatientDTO> getAllPatientsByLabId(Long labId) {
//        return patientRepository.findAllByLabsId(labId).stream()
//                .map(this::convertToPatientDTO)
//                .collect(Collectors.toList());
//    }
//
//    public Object getPatientById(Long patientId, Long labId) {
//        Optional<Lab> lab = labRepository.findById(labId);
//        if (lab.isEmpty()) {
//            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//        }
//
//        Optional<PatientEntity> patient = patientRepository.findById(patientId);
//        if (patient.isEmpty() || !patient.get().getLabs().contains(lab.get())) {
//            return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
//        }
//
//        return convertToPatientDTO(patient.get());
//    }
//
//    public boolean existsById(Long patientId) {
//        return patientRepository.existsById(patientId);
//    }
//
//    @Transactional
//    public void updatePatient(Long patientId, Long labId, PatientDTO patientDTO, String currentUser) {
//        Lab lab = labRepository.findById(labId)
//                .orElseThrow(() -> new RuntimeException("Lab not found"));
//
//        PatientEntity patientEntity = patientRepository.findById(patientId)
//                .filter(patient -> patient.getLabs().stream()
//                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
//                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
//
//        updatePatientEntityFromDTO(patientEntity, patientDTO, String.valueOf(currentUser));
//        patientEntity.getLabs().clear();
//        patientEntity.getLabs().add(lab);
//
//        patientRepository.save(patientEntity);
//    }
//
//    @Transactional
//    public void deletePatient(Long patientId, Long labId) {
//        Lab lab = labRepository.findById(labId)
//                .orElseThrow(() -> new RuntimeException("Lab not found"));
//
//        PatientEntity patientEntity = patientRepository.findById(patientId)
//                .filter(patient -> patient.getLabs().stream()
//                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
//                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
//
//        patientRepository.delete(patientEntity);
//    }
//
//    public Optional<PatientEntity> findByPhoneAndFirstNameAndLabsId(String phone, String firstName, long id) {
//        return patientRepository.findByPhoneAndFirstNameAndLabsId(phone, firstName, id);
//    }
//
//    @Transactional(rollbackOn = Exception.class)
//    public PatientDTO savePatientWithDetails(Lab lab, PatientDTO patientDTO, String currentUser) {
//        try {
//            Optional<PatientEntity> existingPatient = findByPhoneAndFirstNameAndLabsId(
//                    patientDTO.getPhone(),
//                    patientDTO.getFirstName()
//                    , lab.getId()
//            );
//
//            if (existingPatient.isPresent()) {
//                return addVisitAndBillingToExistingPatient(lab, patientDTO, existingPatient.get(), currentUser);
//            }
//
//            PatientEntity patient = mapPatientDTOToEntity(patientDTO, currentUser);
//            Optional<PatientEntity> guardian = patientRepository.findFirstByPhoneOrderByPatientIdAsc(patientDTO.getPhone());
//            guardian.ifPresent(patient::setGuardian);
//
//            patient.getLabs().add(lab);
//
//            if (patientDTO.getVisit() != null) {
//                VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab, currentUser);
//                visit.setPatient(patient);
//                patient.getVisits().add(visit);
//            }
//
//            PatientEntity savedPatient = patientRepository.save(patient);
//            return new PatientDTO(savedPatient);
//        } catch (Exception e) {
/// /            log.error("Error saving patient with details", e);
//            throw new RuntimeException("Failed to save patient: " + e.getMessage(), e);
//        }
//    }
//
//    @Transactional(rollbackOn = Exception.class)
//    public PatientDTO addVisitAndBillingToExistingPatient(Lab lab, PatientDTO patientDTO, PatientEntity existingPatient, String currentUser) {
//        if (patientDTO.getLastName() != null) existingPatient.setLastName(patientDTO.getLastName());
//        if (patientDTO.getEmail() != null) existingPatient.setEmail(patientDTO.getEmail());
//
//        if (patientDTO.getVisit() != null) {
//            VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab, currentUser);
//            visit.setPatient(existingPatient);
//            existingPatient.getVisits().add(visit);
//        }
//        patientRepository.save(existingPatient);
//        return new PatientDTO(existingPatient);
//    }
//
//    private PatientDTO convertToPatientDTO(PatientEntity patient) {
//        PatientDTO patientDTO = new PatientDTO();
//        patientDTO.setId(patient.getPatientId());
//        patientDTO.setFirstName(patient.getFirstName());
//        patientDTO.setLastName(patient.getLastName());
//        patientDTO.setEmail(patient.getEmail());
//        patientDTO.setPhone(patient.getPhone());
//        patientDTO.setAddress(patient.getAddress());
//        patientDTO.setCity(patient.getCity());
//        patientDTO.setState(patient.getState());
//        patientDTO.setZip(patient.getZip());
//        patientDTO.setBloodGroup(patient.getBloodGroup());
//        patientDTO.setDateOfBirth(patient.getDateOfBirth());
//        patientDTO.setAge(patient.getAge());
//
//        patientDTO.setGender(patient.getGender());
//        return patientDTO;
//    }
//
//    private void updatePatientEntityFromDTO(PatientEntity entity, PatientDTO dto, String currentUser) {
//        entity.setFirstName(dto.getFirstName());
//        entity.setLastName(dto.getLastName());
//        entity.setEmail(dto.getEmail());
//        entity.setPhone(dto.getPhone());
//        entity.setAddress(dto.getAddress());
//        entity.setCity(dto.getCity());
//        entity.setState(dto.getState());
//        entity.setZip(dto.getZip());
//        entity.setBloodGroup(dto.getBloodGroup());
//        entity.setDateOfBirth(dto.getDateOfBirth());
//        entity.setAge(dto.getAge());
//        entity.setCreatedBy(currentUser);
//    }
//
//    private PatientEntity mapPatientDTOToEntity(PatientDTO dto, String currentUser) {
//        PatientEntity entity = new PatientEntity();
//        updatePatientEntityFromDTO(entity, dto, currentUser);
//        entity.setGender(dto.getGender());
//        return entity;
//    }
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
//
//        LocalDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
//
//        visit.setVisitTime(currentDateTime); // Set visit time to current date and time
//
//
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
//        if (visitDTO.getTestIds() != null && !visitDTO.getTestIds().isEmpty()) {
//            List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
//            if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
//                throw new RuntimeException("Test does not belong to the lab");
//            }
//            visit.setTests(new HashSet<>(tests));
//        }
//
//        if (visitDTO.getPackageIds() != null && !visitDTO.getPackageIds().isEmpty()) {
//            List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
//            if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
//                throw new RuntimeException("Health package does not belong to the lab");
//            }
//            visit.setPackages(new HashSet<>(healthPackages));
//        }
//
//        if (visitDTO.getInsuranceIds() != null && !visitDTO.getInsuranceIds().isEmpty()) {
//            List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
//            if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
//                throw new RuntimeException("Insurance does not belong to the lab");
//            }
//            visit.setInsurance(new HashSet<>(insurance));
//        }
//        visit.getLabs().add(lab);
//
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
//
//        return visit;
//    }
//
/// /    private BillingEntity mapBillingDTOToEntity(BillingDTO billingDTO, Lab lab , String currentUser) {
/// /        BillingEntity billing = billingDTO.getBillingId() != null ?
/// /                billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity()) :
/// /                new BillingEntity();
/// /        billing.setTotalAmount(billingDTO.getTotalAmount() != null ? billingDTO.getTotalAmount() : BigDecimal.ZERO);
/// /        billing.setPaymentStatus(billingDTO.getPaymentStatus() != null ? billingDTO.getPaymentStatus() : "PAID");
/// /        billing.setPaymentMethod(billingDTO.getPaymentMethod() != null ? billingDTO.getPaymentMethod() : "CASH");
/// /        billing.setPaymentDate(billingDTO.getPaymentDate() != null ? billingDTO.getPaymentDate() : LocalDate.now().toString());
/// /        billing.setDiscount(billingDTO.getDiscount() != null ? billingDTO.getDiscount() : BigDecimal.ZERO);
/// /        billing.setGstRate(billingDTO.getGstRate() != null ? billingDTO.getGstRate() : BigDecimal.ZERO);
/// /        billing.setGstAmount(billingDTO.getGstAmount() != null ? billingDTO.getGstAmount() : BigDecimal.ZERO);
/// /        billing.setCgstAmount(billingDTO.getCgstAmount() != null ? billingDTO.getCgstAmount() : BigDecimal.ZERO);
/// /        billing.setSgstAmount(billingDTO.getSgstAmount() != null ? billingDTO.getSgstAmount() : BigDecimal.ZERO);
/// /        billing.setIgstAmount(billingDTO.getIgstAmount() != null ? billingDTO.getIgstAmount() : BigDecimal.ZERO);
/// /        billing.setNetAmount(billingDTO.getNetAmount() != null ? billingDTO.getNetAmount() : BigDecimal.ZERO);
/// /
//
//    /// /      //----------------------------------------------------
/// /        billing.setUpiId(billingDTO.getUpiId());
/// /        billing.setReceivedAmount(billingDTO.getReceivedAmount() != null ? billingDTO.getReceivedAmount() : BigDecimal.ZERO);
/// /        billing.setRefundAmount(billingDTO.getRefundAmount() != null ? billingDTO.getRefundAmount() : BigDecimal.ZERO);
/// /        billing.setUpiAmount(billingDTO.getUpiAmount() != null ? billingDTO.getUpiAmount() : BigDecimal.ZERO);
/// /        billing.setCardAmount(billingDTO.getCardAmount() != null ? billingDTO.getCardAmount() : BigDecimal.ZERO);
/// /        billing.setCashAmount(billingDTO.getCashAmount() != null ? billingDTO.getCashAmount() : BigDecimal.ZERO);
/// /        billing.setDueAmount(billingDTO.getDueAmount() != null ? billingDTO.getDueAmount() : BigDecimal.ZERO);
/// /        billing.setBillingTime(LocalTime.parse(LocalTime.now().toString()));
/// /        billing.setBillingDate(LocalDate.now().toString());
/// /        billing.setCreatedBy(currentUser);
/// /
/// /
/// /        billing.setDiscountReason(billingDTO.getDiscountReason());
/// /        billing.getLabs().add(lab);
/// /
/// /        // handle test discounts if provided
/// /        return billing;
/// /    }
//
//
//    //-----------------new code for updating patient details-----------------
//    @Transactional(rollbackOn = Exception.class)
//    public PatientDTO updatePatientDetails(PatientEntity existingPatient, Lab lab, PatientDTO patientDTO, String username) {
//        try {
//            // Update basic patient information
//            if (patientDTO.getFirstName() != null) {
//                existingPatient.setFirstName(patientDTO.getFirstName());
//            }
//            if (patientDTO.getLastName() != null) {
//                existingPatient.setLastName(patientDTO.getLastName());
//            }
//            if (patientDTO.getEmail() != null) {
//                existingPatient.setEmail(patientDTO.getEmail());
//            }
//            if (patientDTO.getPhone() != null) {
//                existingPatient.setPhone(patientDTO.getPhone());
//            }
//            if (patientDTO.getAddress() != null) {
//                existingPatient.setAddress(patientDTO.getAddress());
//            }
//            if (patientDTO.getCity() != null) {
//                existingPatient.setCity(patientDTO.getCity());
//            }
//            if (patientDTO.getState() != null) {
//                existingPatient.setState(patientDTO.getState());
//            }
//            if (patientDTO.getZip() != null) {
//                existingPatient.setZip(patientDTO.getZip());
//            }
//            if (patientDTO.getBloodGroup() != null) {
//                existingPatient.setBloodGroup(patientDTO.getBloodGroup());
//            }
//            if (patientDTO.getDateOfBirth() != null) {
//                existingPatient.setDateOfBirth(patientDTO.getDateOfBirth());
//            }
//            if (patientDTO.getGender() != null) {
//                existingPatient.setGender(patientDTO.getGender());
//            }
//
//            if (patientDTO.getAge() != null) {
//                existingPatient.setAge(patientDTO.getAge());
//            }
//
//            // upddate by
//            if (patientDTO.getUpdatedBy() != null) {
//                existingPatient.setUpdatedBy(username);
//            }
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
//        // Handle tests update
//        if (visitDTO.getTestIds() != null) {
//            List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
//            if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
//                throw new RuntimeException("Test does not belong to the lab");
//            }
//            visit.setTests(new HashSet<>(tests));
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
////        // Update billing fields
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
////        if (billingDTO.getGstRate() != null) {
////            billing.setGstRate(billingDTO.getGstRate());
////        }
////        if (billingDTO.getGstAmount() != null) {
////            billing.setGstAmount(billingDTO.getGstAmount());
////        }
////        if (billingDTO.getCgstAmount() != null) {
////            billing.setCgstAmount(billingDTO.getCgstAmount());
////        }
////        if (billingDTO.getSgstAmount() != null) {
////            billing.setSgstAmount(billingDTO.getSgstAmount());
////        }
////        if (billingDTO.getIgstAmount() != null) {
////            billing.setIgstAmount(billingDTO.getIgstAmount());
////        }
////        if (billingDTO.getNetAmount() != null) {
////            billing.setNetAmount(billingDTO.getNetAmount());
////        }
////        if (billingDTO.getDiscountReason() != null) {
////            billing.setDiscountReason(billingDTO.getDiscountReason());
////        }
////        if (billingDTO.getDiscountPercentage() != null) {
////            billing.setDiscount(billingDTO.getDiscountPercentage());
////        }
////
////        //-------------------------------------------------
////        if (billingDTO.getUpiId() != null) {
////            billing.setUpiId(billingDTO.getUpiId());
////        }
////
////        if (billingDTO.getReceivedAmount() != null) {
////            billing.setReceivedAmount(billingDTO.getReceivedAmount());
////        }
////
////        if (billingDTO.getRefundAmount() != null) {
////            billing.setRefundAmount(billingDTO.getRefundAmount());
////        }
////
////        if (billingDTO.getUpiAmount() != null) {
////            billing.setUpiAmount(billingDTO.getUpiAmount());
////        }
////
////        if (billingDTO.getCardAmount() != null) {
////            billing.setCardAmount(billingDTO.getCardAmount());
////        }
////
////        if (billingDTO.getCashAmount() != null) {
////            billing.setCashAmount(billingDTO.getCashAmount());
////        }
////
////        if (billingDTO.getDueAmount() != null) {
////            billing.setDueAmount(billingDTO.getDueAmount());
////        }
////
////
////        //-------------------------------------------------
////        billing.setCreatedBy(username);
////        LocalTime billingTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
////        billing.setBillingTime(billingTime);
////
////        billing.setBillingDate(LocalDate.now().toString());
////        billing.setUpdatedBy(username);
////
////        billing.getLabs().add(lab);
////        billing = billingRepository.save(billing);
////
////        // Handle test discounts
////        if (testDiscounts != null && !testDiscounts.isEmpty()) {
////            // First, remove existing discounts for this billing
////            testDiscountRepository.deleteByBilling(billing);
////
////            // Then add the new discounts
////            BillingEntity finalBilling = billing;
////            Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
////                    .map(discountDTO -> {
////                        TestDiscountEntity discountEntity = new TestDiscountEntity();
////                        discountEntity.setTestId(discountDTO.getTestId());
////                        discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
////                        discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
////                        discountEntity.setFinalPrice(discountDTO.getFinalPrice());
////                        discountEntity.setCreatedBy("system");
////                        discountEntity.setUpdatedBy("system");
////                        discountEntity.setBilling(finalBilling);
////                        return discountEntity;
////                    })
////                    .collect(Collectors.toSet());
////            testDiscountRepository.saveAll(discountEntities);
////        }
////    }
//
//    public List<PatientList> getPatientbytheirPhoneAndLabId(String phone, Long labId) {
//        List<PatientEntity> patients = patientRepository.findByPhoneStartingWithAndLabId(phone, labId);
//        return patients.stream()
//                .map(patient -> {
//                    PatientList patientList = new PatientList();
//                    patientList.setId(patient.getPatientId());
//                    patientList.setFirstName(patient.getFirstName());
//                    patientList.setPhone(patient.getPhone());
//                    patientList.setCity(patient.getCity());
//                    patientList.setGender(patient.getGender());
//                    patientList.setDateOfBirth(patient.getDateOfBirth());
//                    // Optional: Debug log
//                    System.out.println("Mapped PatientList: " + patientList);
//                    return patientList;
//                })
//                .collect(Collectors.toList());
//    }
//
//    public void cancelVisit(Long visitId, Long labId, String username, CancellationDataDTO cancellationData) {
//        Optional<Lab> labOpt = labRepository.findById(labId);
//        if (labOpt.isEmpty()) {
//            throw new RuntimeException("Lab not found");
//        }
//        Lab lab = labOpt.get();
//        Optional<VisitEntity> visitOpt = visitRepository.findById(visitId);
//        if (visitOpt.isEmpty()) {
//            throw new RuntimeException("Visit not found");
//        }
//        VisitEntity visit = visitOpt.get();
//        if (!visit.getLabs().contains(lab)) {
//            throw new RuntimeException("Visit does not belong to the specified lab");
//        }
//
//        // Update visit cancellation details
//        visit.setVisitCancellationReason(cancellationData.getVisitCancellationReason());
//        visit.setVisitCancellationDate(LocalDate.parse(cancellationData.getVisitCancellationDate()));
//        visit.setVisitCancellationTime(LocalDateTime.of(LocalDate.parse(cancellationData.getVisitCancellationDate()), LocalTime.parse(cancellationData.getVisitCancellationTime())));
//
//        visit.setVisitStatus("CANCELLED");
//        visit.setUpdatedBy(username);
//        visit.setVisitCancellationBy(username);
//    }
//
//
////===================================================================================================================
//
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
//        billing.setReceivedAmount(billingDTO.getReceivedAmount() != null ? billingDTO.getReceivedAmount() : BigDecimal.ZERO);
//        billing.setDueAmount(billingDTO.getDueAmount() != null ? billingDTO.getDueAmount() : BigDecimal.ZERO);
//
//        // Audit fields
//        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
//        billing.setBillingDate(LocalDate.now().toString());
//        billing.setCreatedBy(currentUser);
//        billing.setUpdatedBy(currentUser);
//
//        // Clear old transactions if any (if updating)
//        if (billing.getId() != null && billing.getTransactions() != null) {
//            billing.getTransactions().clear();
//        }
//
//        // Handle transactions
//        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
//            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
//                TransactionEntity transaction = new TransactionEntity();
//
//                transaction.setPaymentMethod(transactionDTO.getPaymentMethod() != null ? transactionDTO.getPaymentMethod() : "CASH");
//                transaction.setUpiId(transactionDTO.getUpiId());
//                transaction.setUpiAmount(transactionDTO.getUpiAmount() != null ? transactionDTO.getUpiAmount() : BigDecimal.ZERO);
//                transaction.setCardAmount(transactionDTO.getCardAmount() != null ? transactionDTO.getCardAmount() : BigDecimal.ZERO);
//                transaction.setCashAmount(transactionDTO.getCashAmount() != null ? transactionDTO.getCashAmount() : BigDecimal.ZERO);
//                transaction.setReceivedAmount(transactionDTO.getReceivedAmount() != null ? transactionDTO.getReceivedAmount() : BigDecimal.ZERO);
//                transaction.setRefundAmount(transactionDTO.getRefundAmount() != null ? transactionDTO.getRefundAmount() : BigDecimal.ZERO);
//                transaction.setDueAmount(transactionDTO.getDueAmount() != null ? transactionDTO.getDueAmount() : BigDecimal.ZERO);
//                transaction.setPaymentDate(transactionDTO.getPaymentDate() != null ? transactionDTO.getPaymentDate() : LocalDate.now().toString());
//                transaction.setRemarks(transactionDTO.getRemarks());
//
//                // Set bidirectional relationship
//                transaction.setBilling(billing);
//                billing.getTransactions().add(transaction);
//            }
//        }
//
//        // Add lab association (avoid duplicates)
//        billing.getLabs().add(lab);
//
//
//
//        return billing;
//    }
//
//
//    @Transactional
//    public BillingDTO saveBillingWithTransactions(BillingDTO billingDTO, Lab lab, String currentUser) {
//        BillingEntity billing = mapBillingDTOToEntity(billingDTO, lab, currentUser);
//
//        // Save the billing entity (transactions will be cascaded)
//        BillingEntity savedBilling = billingRepository.save(billing);
//
//        // Return the saved DTO
//        return new BillingDTO(savedBilling);
//    }
//    private void updateBillingDetails(VisitEntity visit, Lab lab, BillingDTO billingDTO, List<TestDiscountDTO> testDiscounts, String username) {
//        BillingEntity billing = visit.getBilling();
//        if (billing == null) {
//            billing = new BillingEntity();
//            visit.setBilling(billing);
//        }
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
//        if (billingDTO.getDiscount() != null) {
//            billing.setDiscount(billingDTO.getDiscount());
//        }
//        if (billingDTO.getGstRate() != null) {
//            billing.setGstRate(billingDTO.getGstRate());
//        }
//        if (billingDTO.getGstAmount() != null) {
//            billing.setGstAmount(billingDTO.getGstAmount());
//        }
//        if (billingDTO.getCgstAmount() != null) {
//            billing.setCgstAmount(billingDTO.getCgstAmount());
//        }
//        if (billingDTO.getSgstAmount() != null) {
//            billing.setSgstAmount(billingDTO.getSgstAmount());
//        }
//        if (billingDTO.getIgstAmount() != null) {
//            billing.setIgstAmount(billingDTO.getIgstAmount());
//        }
//        if (billingDTO.getNetAmount() != null) {
//            billing.setNetAmount(billingDTO.getNetAmount());
//        }
//        if (billingDTO.getDiscountReason() != null) {
//            billing.setDiscountReason(billingDTO.getDiscountReason());
//        }
//        if (billingDTO.getReceivedAmount() != null) {
//            billing.setReceivedAmount(billingDTO.getReceivedAmount());
//        }
//        if (billingDTO.getDueAmount() != null) {
//            billing.setDueAmount(billingDTO.getDueAmount());
//        }
//
//        // Update audit fields
//        billing.setBillingTime(LocalTime.now(ZoneId.of("Asia/Kolkata")));
//        billing.setBillingDate(LocalDate.now().toString());
//        billing.setUpdatedBy(username);
//
//        // Handle transactions update
//        if (billingDTO.getTransactions() != null) {
//            // Clear existing transactions
//            billing.getTransactions().clear();
//
//            // Add new transactions
//            BillingEntity finalBilling = billing;
//            billingDTO.getTransactions().forEach(transactionDTO -> {
//                TransactionEntity transaction = new TransactionEntity();
//                transaction.setPaymentMethod(transactionDTO.getPaymentMethod());
//                transaction.setUpiId(transactionDTO.getUpiId());
//                transaction.setUpiAmount(transactionDTO.getUpiAmount());
//                transaction.setCardAmount(transactionDTO.getCardAmount());
//                transaction.setCashAmount(transactionDTO.getCashAmount());
//                transaction.setReceivedAmount(transactionDTO.getReceivedAmount());
//                transaction.setRefundAmount(transactionDTO.getRefundAmount());
//                transaction.setDueAmount(transactionDTO.getDueAmount());
//                transaction.setPaymentDate(transactionDTO.getPaymentDate() != null ?
//                        transactionDTO.getPaymentDate() : LocalDate.now().toString());
//                transaction.setRemarks(transactionDTO.getRemarks());
//                transaction.setBilling(finalBilling);
//                finalBilling.getTransactions().add(transaction);
//            });
//        }
//
//        billing.getLabs().add(lab);
//        billing = billingRepository.save(billing);
//
//        // Handle test discounts
//        if (testDiscounts != null && !testDiscounts.isEmpty()) {
//            // First, remove existing discounts for this billing
//            testDiscountRepository.deleteByBilling(billing);
//
//            // Then add the new discounts
//            BillingEntity finalBilling1 = billing;
//            Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
//                    .map(discountDTO -> {
//                        TestDiscountEntity discountEntity = new TestDiscountEntity();
//                        discountEntity.setTestId(discountDTO.getTestId());
//                        discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
//                        discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
//                        discountEntity.setFinalPrice(discountDTO.getFinalPrice());
//                        discountEntity.setCreatedBy(username);
//                        discountEntity.setUpdatedBy(username);
//                        discountEntity.setBilling(finalBilling1);
//                        return discountEntity;
//                    })
//                    .collect(Collectors.toSet());
//            testDiscountRepository.saveAll(discountEntities);
//        }
//    }
//
//
//}


//=========================================================================================================


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

    public boolean existsByPhone(String phone) {
        return patientRepository.existsByPhone(phone);
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

    public boolean existsById(Long patientId) {
        return patientRepository.existsById(patientId);
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
        return visit;
    }

    @Transactional(rollbackOn = Exception.class)
    public PatientDTO updatePatientDetails(PatientEntity existingPatient, Lab lab, PatientDTO patientDTO, String username) {
        try {
            // Update basic patient information
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

            // upddate by
            if (patientDTO.getUpdatedBy() != null) {
                existingPatient.setUpdatedBy(username);
            }

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

        // Handle tests update
        if (visitDTO.getTestIds() != null) {
            List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
            if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
                throw new RuntimeException("Test does not belong to the lab");
            }
            visit.setTests(new HashSet<>(tests));
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

        // Clear old transactions if any (if updating)
        if (billing.getId() != null && billing.getTransactions() != null) {
            billing.getTransactions().clear();
        }

        // Handle transactions
        if (billingDTO.getTransactions() != null && !billingDTO.getTransactions().isEmpty()) {
            for (TransactionDTO transactionDTO : billingDTO.getTransactions()) {
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
        // Add lab association (avoid duplicates)
        billing.getLabs().add(lab);

        return billing;
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
//        }
//        if (billingDTO.getGstRate() != null) {
//            billing.setGstRate(billingDTO.getGstRate());
//        }
//        if (billingDTO.getGstAmount() != null) {
//            billing.setGstAmount(billingDTO.getGstAmount());
//        }
//        if (billingDTO.getCgstAmount() != null) {
//            billing.setCgstAmount(billingDTO.getCgstAmount());
//        }
//        if (billingDTO.getSgstAmount() != null) {
//            billing.setSgstAmount(billingDTO.getSgstAmount());
//        }
//        if (billingDTO.getIgstAmount() != null) {
//            billing.setIgstAmount(billingDTO.getIgstAmount());
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

        // Handle transactions update
        if (billingDTO.getTransactions() != null) {
            // Clear existing transactions
            billing.getTransactions().clear();

            // Add new transactions
            BillingEntity finalBilling = billing;
            billingDTO.getTransactions().forEach(transactionDTO -> {
                TransactionEntity transaction = new TransactionEntity();
                transaction.setPaymentMethod(transactionDTO.getPaymentMethod());
                transaction.setUpiId(transactionDTO.getUpiId());
                transaction.setUpiAmount(transactionDTO.getUpiAmount());
                transaction.setCardAmount(transactionDTO.getCardAmount());
                transaction.setCashAmount(transactionDTO.getCashAmount());
                transaction.setReceivedAmount(transactionDTO.getReceivedAmount());
                transaction.setRefundAmount(transactionDTO.getRefundAmount());
                transaction.setDueAmount(transactionDTO.getDueAmount());
                transaction.setPaymentDate(transactionDTO.getPaymentDate() != null ?
                        transactionDTO.getPaymentDate() : LocalDate.now().toString());
                transaction.setRemarks(transactionDTO.getRemarks());
                transaction.setCreatedBy(username);
                transaction.setBilling(finalBilling);
                finalBilling.getTransactions().add(transaction);
            });
        }

        billing.getLabs().add(lab);
        billing = billingRepository.save(billing);

        // Handle test discounts
        if (testDiscounts != null && !testDiscounts.isEmpty()) {
            // First, remove existing discounts for this billing
            testDiscountRepository.deleteByBilling(billing);

            // Then add the new discounts
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



    public Optional<BillingEntity> getBillingById(Long billingId) {
        return billingRepository.findById(billingId);
    }

//    public BillDTO addPartialPayment(BillingEntity billing, BillDTO billDTO, String username) {
//        // Validate input
//        if (billing == null || billDTO == null) {
//            throw new IllegalArgumentException("Billing and billDTO cannot be null");
//        }
//
//        // Get the transaction from DTO
//        TransactionDTO transactionDTO = billDTO.getTransaction();
//        if (transactionDTO == null) {
//            throw new IllegalArgumentException("Transaction data is required");
//        }
//
//        // Create new transaction entity
//        TransactionEntity transaction = new TransactionEntity();
//
//        // Set payment method (default to CASH if null)
//        transaction.setPaymentMethod(
//                transactionDTO.getPaymentMethod() != null ?
//                        transactionDTO.getPaymentMethod() : "CASH"
//        );
//
//        // Set payment amounts (default to 0 if null)
//        transaction.setUpiId(transactionDTO.getUpiId());
//        transaction.setUpiAmount(
//                transactionDTO.getUpiAmount() != null ?
//                        transactionDTO.getUpiAmount() : BigDecimal.ZERO
//        );
//        transaction.setCardAmount(
//                transactionDTO.getCardAmount() != null ?
//                        transactionDTO.getCardAmount() : BigDecimal.ZERO
//        );
//        transaction.setCashAmount(
//                transactionDTO.getCashAmount() != null ?
//                        transactionDTO.getCashAmount() : BigDecimal.ZERO
//        );
//
//        // Validate received amount
//        if (transactionDTO.getReceivedAmount() == null ||
//                transactionDTO.getReceivedAmount().compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Received amount must be positive");
//        }
//        transaction.setReceivedAmount(transactionDTO.getReceivedAmount());
//
//        // Set other transaction fields
//        transaction.setRefundAmount(
//                transactionDTO.getRefundAmount() != null ?
//                        transactionDTO.getRefundAmount() : BigDecimal.ZERO
//        );
//        transaction.setDueAmount(
//                transactionDTO.getDueAmount() != null ?
//                        transactionDTO.getDueAmount() : BigDecimal.ZERO
//        );
//        transaction.setPaymentDate(
//                transactionDTO.getPaymentDate() != null ?
//                        transactionDTO.getPaymentDate() : LocalDate.now().toString()
//        );
//        transaction.setRemarks(
//                transactionDTO.getRemarks() != null ?
//                        transactionDTO.getRemarks() : "Payment via " + transaction.getPaymentMethod()
//        );
//        transaction.setCreatedAt(LocalDateTime.now());
//        transaction.setCreatedBy(username);
//
//        // Link transaction to billing
//        transaction.setBilling(billing);
//        billing.getTransactions().add(transaction);
//
//        // Calculate new amounts
//        BigDecimal newReceivedAmount = billing.getReceivedAmount().add(transaction.getReceivedAmount());
//        BigDecimal newDueAmount = billing.getTotalAmount().subtract(newReceivedAmount);
//
//        // Update billing
//        billing.setReceivedAmount(newReceivedAmount);
//        billing.setDueAmount(newDueAmount);
//
//        // Update payment status
//        if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
//            billing.setPaymentStatus("PAID");
//        } else if (newReceivedAmount.compareTo(BigDecimal.ZERO) > 0) {
//            billing.setPaymentStatus("PARTIAL");
//        } else {
//            billing.setPaymentStatus("UNPAID");
//        }
//
//        // Update payment method if provided in DTO
//        if (billDTO.getPaymentMethod() != null) {
//            billing.setPaymentMethod(billDTO.getPaymentMethod());
//        }
//
//        // Update payment date if provided in DTO
//        if (billDTO.getPaymentDate() != null) {
//            billing.setPaymentDate(billDTO.getPaymentDate());
//        }
//
//        // Save and return
//        billing = billingRepository.save(billing);
//        return new BillDTO(billing);
//    }

    public BillDTO addPartialPayment(BillingEntity billing, BillDtoDue billDTO, String username) {
        // Validate input
        if (billing == null || billDTO == null) {
            throw new IllegalArgumentException("Billing and billDTO cannot be null");
        }

        // Get the transaction from DTO
        TransactionDTO transactionDTO = billDTO.getTransaction();
        if (transactionDTO == null) {
            throw new IllegalArgumentException("Transaction data is required");
        }

        // Create new transaction entity
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

