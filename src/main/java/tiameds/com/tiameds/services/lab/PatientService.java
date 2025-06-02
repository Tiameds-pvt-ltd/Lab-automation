//package tiameds.com.tiameds.services.lab;
//
//import jakarta.transaction.Transactional;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import tiameds.com.tiameds.dto.lab.BillingDTO;
//import tiameds.com.tiameds.dto.lab.PatientDTO;
//import tiameds.com.tiameds.dto.lab.VisitDTO;
//import tiameds.com.tiameds.entity.*;
//import tiameds.com.tiameds.repository.*;
//import tiameds.com.tiameds.utils.ApiResponseHelper;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.stream.Collectors;
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
//
//    public PatientService(LabRepository labRepository, TestRepository testRepository, HealthPackageRepository healthPackageRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, HealthPackageRepository packageRepository, InsuranceRepository insuranceRepository, BillingRepository billingRepository) {
//        this.labRepository = labRepository;
//        this.testRepository = testRepository;
//        this.healthPackageRepository = healthPackageRepository;
//        this.patientRepository = patientRepository;
//        this.doctorRepository = doctorRepository;
//        this.packageRepository = packageRepository;
//        this.insuranceRepository = insuranceRepository;
//        this.billingRepository = billingRepository;
//    }
//
/// /    @Transactional
/// /    public Optional<PatientEntity> findByPhoneOrEmail(String phone, String email) {
/// /        return patientRepository.findByPhoneOrEmail(phone, email);
/// /    }
//
////    @Transactional
////    public Optional<PatientEntity> findByPhoneOrFirstName(String phone, String firstName) {
////        return patientRepository.findByPhoneOrFirstName(phone, firstName);
////    }
////
////    @Transactional
////    public PatientDTO savePatientWithDetails(Lab lab, PatientDTO patientDTO) {
////        PatientEntity patient = new PatientEntity();
////        patient.setFirstName(patientDTO.getFirstName());
////        patient.setLastName(patientDTO.getLastName());
////        patient.setEmail(patientDTO.getEmail());
////        patient.setPhone(patientDTO.getPhone());
////        patient.setAddress(patientDTO.getAddress());
////        patient.setCity(patientDTO.getCity());
////        patient.setState(patientDTO.getState());
////        patient.setZip(patientDTO.getZip());
////        patient.setBloodGroup(patientDTO.getBloodGroup());
////        patient.setDateOfBirth(patientDTO.getDateOfBirth());
////        patient.setGender(patientDTO.getGender());
////
////        // Set the lab for the patient
////        patient.getLabs().add(lab);
////
////        // Handle visit and billing (if provided)
////        if (patientDTO.getVisit() != null) {
////            VisitDTO visitDTO = patientDTO.getVisit();
////            VisitEntity visit = mapVisitDTOToEntity(visitDTO, lab);
////            visit.setPatient(patient);
////            patient.getVisits().add(visit);
////        }
////
////        // Save the patient and related entities
////        patientRepository.save(patient);
////
////        return new PatientDTO(patient);
////    }
////
//////    @Transactional
////    public PatientDTO addVisitAndBillingToExistingPatient(Lab lab, PatientDTO patientDTO, PatientEntity existingPatient) {
////        VisitDTO visitDTO = patientDTO.getVisit();
////        if (visitDTO != null) {
////            VisitEntity visit = mapVisitDTOToEntity(visitDTO, lab);
////            visit.setPatient(existingPatient);
////            existingPatient.getVisits().add(visit);
////        }
////
////        // Save updated patient
////        patientRepository.save(existingPatient);
////
////        return new PatientDTO(existingPatient);
////    }
////
////    private VisitEntity mapVisitDTOToEntity(VisitDTO visitDTO, Lab lab) {
////        VisitEntity visit = new VisitEntity();
////        visit.setVisitDate(visitDTO.getVisitDate());
////        visit.setVisitType(visitDTO.getVisitType());
////        visit.setVisitStatus(visitDTO.getVisitStatus());
////        visit.setVisitDescription(visitDTO.getVisitDescription());
////
////        // Associate doctor
//////        Doctors doctor = doctorRepository.findById(visitDTO.getDoctorId())
//////                .orElseThrow(() -> new RuntimeException("Doctor not found"));
//////        if (!lab.getDoctors().contains(doctor)) {
//////            throw new RuntimeException("Doctor does not belong to the lab");
//////        }
//////        visit.setDoctor(doctor);
////
////        // Associate doctor only if doctorId is provided
////
////        if (visitDTO.getDoctorId() != null) {
////            // Find the doctor by ID
////            Optional<Doctors> doctorOpt = doctorRepository.findById(visitDTO.getDoctorId());
////
////            if (doctorOpt.isPresent()) {
////                Doctors doctor = doctorOpt.get();
////
////                // Check if the doctor belongs to the lab
////                if (!lab.getDoctors().contains(doctor)) {
////                    throw new RuntimeException("Doctor does not belong to the lab");
////                }
////
////                // Set the doctor if everything is valid
////                visit.setDoctor(doctor);
////            } else {
////                // If no doctor is found, set the doctor to null
////                visit.setDoctor(null);
////            }
////        } else {
////            // Handle case where no doctorId is provided
////            visit.setDoctor(null); // Doctor will be null if no ID is provided
////        }
////
////        // Associate tests
////        List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
////        if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
////            throw new RuntimeException("Test does not belong to the lab");
////        }
////        visit.setTests(new HashSet<>(tests));
////
////        // Associate health packages
////        List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
////        if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
////            throw new RuntimeException("Health package does not belong to the lab");
////        }
////        visit.setPackages(new HashSet<>(healthPackages));
////
////        // Associate insurance
////        List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
////        if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
////            throw new RuntimeException("Insurance does not belong to the lab");
////        }
////        visit.setInsurance(new HashSet<>(insurance));
////        visit.getLabs().add(lab);
////
////        // Handle billing details and ensure it's attached to the current transaction context
////        BillingDTO billingDTO = visitDTO.getBilling();
////        if (billingDTO != null) {
////            BillingEntity billing;
////
////            // Check if billingId is provided (Long instead of long)
////            if (billingDTO.getBillingId() != null) {
////                billing = billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity());
////            } else {
////                billing = new BillingEntity(); // Create new if no billingId
////            }
////
////            // Set billing properties
////            billing.setId(billingDTO.getBillingId());
////            billing.setTotalAmount(billingDTO.getTotalAmount());
////            billing.setPaymentStatus(billingDTO.getPaymentStatus());
////            billing.setPaymentMethod(billingDTO.getPaymentMethod());
////            billing.setPaymentDate(billingDTO.getPaymentDate());
////            billing.setDiscount(billingDTO.getDiscount());
////            billing.setGstRate(billingDTO.getGstRate());
////            billing.setGstAmount(billingDTO.getGstAmount());
////            billing.setCgstAmount(billingDTO.getCgstAmount());
////            billing.setSgstAmount(billingDTO.getSgstAmount());
////            billing.setIgstAmount(billingDTO.getIgstAmount());
////            billing.setNetAmount(billingDTO.getNetAmount());
////            billing.getLabs().add(lab);
////
////            // Attach the billing entity to the visit
////            visit.setBilling(billing);
////        }
////        return visit;
////    }
////    ==================
//
//    public boolean existsByPhone(String phone) {
//        return patientRepository.existsByPhone(phone);
//    }
//
//
//    //get all patients by lab id
//    public List<PatientDTO> getAllPatientsByLabId(Long labId) {
//        return patientRepository.findAllByLabsId(labId).stream()
//                .map(patient -> {
//                    PatientDTO patientDTO = new PatientDTO();
//                    patientDTO.setId(patient.getPatientId());
//                    patientDTO.setFirstName(patient.getFirstName());
//                    patientDTO.setLastName(patient.getLastName());
//                    patientDTO.setEmail(patient.getEmail());
//                    patientDTO.setPhone(patient.getPhone());
//                    patientDTO.setAddress(patient.getAddress());
//                    patientDTO.setCity(patient.getCity());
//                    patientDTO.setState(patient.getState());
//                    patientDTO.setZip(patient.getZip());
//                    patientDTO.setBloodGroup(patient.getBloodGroup());
//                    patientDTO.setDateOfBirth(patient.getDateOfBirth());
//                    patientDTO.setGender(patient.getGender());
//                    return patientDTO;
//                })
//                .collect(Collectors.toList());
//    }
//
//
//    //get patient by id of the lab
//    public Object getPatientById(Long patientId, Long labId) {
//
//        // Get the lab by ID
//        Optional<Lab> lab = labRepository.findById(labId);
//        if (lab.isEmpty()) {
//            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//        }
//
//        // Check if the patient exists and belongs to the given lab
//        Optional<PatientEntity> patient = patientRepository.findById(patientId);
//        if (patient.isEmpty() || !patient.get().getLabs().contains(lab.get())) {
//            return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
//        }
//
//        // Make the response
//        PatientEntity patientEntity = patient.get();
//        PatientDTO patientDTO = new PatientDTO();
//        patientDTO.setFirstName(patientEntity.getFirstName());
//        patientDTO.setLastName(patientEntity.getLastName());
//        patientDTO.setEmail(patientEntity.getEmail());
//        patientDTO.setPhone(patientEntity.getPhone());
//        patientDTO.setAddress(patientEntity.getAddress());
//        patientDTO.setCity(patientEntity.getCity());
//        patientDTO.setState(patientEntity.getState());
//        patientDTO.setZip(patientEntity.getZip());
//        patientDTO.setBloodGroup(patientEntity.getBloodGroup());
//        patientDTO.setDateOfBirth(patientEntity.getDateOfBirth());
//
//        return patientDTO;
//    }
//
//    public boolean existsById(Long patientId) {
//        return patientRepository.existsById(patientId);
//    }
//
//    public void updatePatient(Long patientId, Long labId, PatientDTO patientDTO) {
//        // Check if the lab exists
//        Lab lab = labRepository.findById(labId)
//                .orElseThrow(() -> new RuntimeException("Lab not found"));
//
//        // Check if the patient exists and belongs to the lab
//        PatientEntity patientEntity = patientRepository.findById(patientId)
//                .filter(patient -> patient.getLabs().stream()
//                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
//                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
//
//        // Update the patient details from the DTO
//        patientEntity.setFirstName(patientDTO.getFirstName());
//        patientEntity.setLastName(patientDTO.getLastName());
//        patientEntity.setEmail(patientDTO.getEmail());
//        patientEntity.setPhone(patientDTO.getPhone());
//        patientEntity.setAddress(patientDTO.getAddress());
//        patientEntity.setCity(patientDTO.getCity());
//        patientEntity.setState(patientDTO.getState());
//        patientEntity.setZip(patientDTO.getZip());
//        patientEntity.setBloodGroup(patientDTO.getBloodGroup());
//        patientEntity.setDateOfBirth(patientDTO.getDateOfBirth());
//
//        // Update lab relationship (optional, if labs can be updated)
//        patientEntity.getLabs().clear();
//        patientEntity.getLabs().add(lab);
//
//        // Save the updated patient
//        patientRepository.save(patientEntity);
//    }
//
//    public void deletePatient(Long patientId, Long labId) {
//        // Check if the lab exists
//        Lab lab = labRepository.findById(labId)
//                .orElseThrow(() -> new RuntimeException("Lab not found"));
//
//        // Check if the patient exists and belongs to the lab
//        PatientEntity patientEntity = patientRepository.findById(patientId)
//                .filter(patient -> patient.getLabs().stream()
//                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
//                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
//
//        // Delete the patient
//        patientRepository.delete(patientEntity);
//    }
//
//
////   ====+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//    @Transactional
//    public Optional<PatientEntity> findByPhoneAndFirstName(String phone, String firstName) {
//        return patientRepository.findByPhoneAndFirstName(phone, firstName);
//    }
//
//    @Transactional
//    public PatientDTO savePatientWithDetails(Lab lab, PatientDTO patientDTO) {
//        // Check if patient exists (both phone AND first name must match)
//        Optional<PatientEntity> existingPatient = findByPhoneAndFirstName(
//                patientDTO.getPhone(),
//                patientDTO.getFirstName()
//        );
//
//        if (existingPatient.isPresent()) {
//            return addVisitAndBillingToExistingPatient(lab, patientDTO, existingPatient.get());
//        }
//
//        // Create new patient
//        PatientEntity patient = mapPatientDTOToEntity(patientDTO);
//
//        // Find guardian (first patient with this phone number)
//        Optional<PatientEntity> guardian = patientRepository.findFirstByPhoneOrderByPatientIdAsc(patientDTO.getPhone());
//        guardian.ifPresent(g -> patient.setGuardian(g));
//
//        // Set the lab for the patient
//        patient.getLabs().add(lab);
//
//        // Handle visit and billing if provided
//        if (patientDTO.getVisit() != null) {
//            VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab);
//            visit.setPatient(patient);
//            patient.getVisits().add(visit);
//        }
//        // Save the patient
//        patientRepository.save(patient);
//        return new PatientDTO(patient);
//    }
//
//    @Transactional
//    public PatientDTO addVisitAndBillingToExistingPatient(Lab lab, PatientDTO patientDTO, PatientEntity existingPatient) {
//        // Update patient details if needed
//        if (patientDTO.getLastName() != null) existingPatient.setLastName(patientDTO.getLastName());
//        if (patientDTO.getEmail() != null) existingPatient.setEmail(patientDTO.getEmail());
//        // Add other fields you want to update here...
//
//        // Add new visit if provided
//        if (patientDTO.getVisit() != null) {
//            VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab);
//            visit.setPatient(existingPatient);
//            existingPatient.getVisits().add(visit);
//        }
//
//        // Save updated patient
//        patientRepository.save(existingPatient);
//        return new PatientDTO(existingPatient);
//    }
//
//    private PatientEntity mapPatientDTOToEntity(PatientDTO dto) {
//        PatientEntity entity = new PatientEntity();
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
//        entity.setGender(dto.getGender());
//        return entity;
//    }
//
//    private VisitEntity mapVisitDTOToEntity(VisitDTO visitDTO, Lab lab) {
//        VisitEntity visit = new VisitEntity();
//        visit.setVisitDate(visitDTO.getVisitDate());
//        visit.setVisitType(visitDTO.getVisitType());
//        visit.setVisitStatus(visitDTO.getVisitStatus());
//        visit.setVisitDescription(visitDTO.getVisitDescription());
//
//        // Associate doctor if provided
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
//        // Associate tests
//        if (visitDTO.getTestIds() != null && !visitDTO.getTestIds().isEmpty()) {
//            List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
//            if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
//                throw new RuntimeException("Test does not belong to the lab");
//            }
//            visit.setTests(new HashSet<>(tests));
//        }
//        // Associate health packages
//        if (visitDTO.getPackageIds() != null && !visitDTO.getPackageIds().isEmpty()) {
//            List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
//            if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
//                throw new RuntimeException("Health package does not belong to the lab");
//            }
//            visit.setPackages(new HashSet<>(healthPackages));
//        }
//        // Associate insurance
//        if (visitDTO.getInsuranceIds() != null && !visitDTO.getInsuranceIds().isEmpty()) {
//            List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
//            if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
//                throw new RuntimeException("Insurance does not belong to the lab");
//            }
//            visit.setInsurance(new HashSet<>(insurance));
//        }
//        visit.getLabs().add(lab);
//        // Handle billing
//        if (visitDTO.getBilling() != null) {
//            BillingEntity billing = mapBillingDTOToEntity(visitDTO.getBilling(), lab);
//            visit.setBilling(billing);
//        }
//        return visit;
//    }
//
//    private BillingEntity mapBillingDTOToEntity(BillingDTO billingDTO, Lab lab) {
//        BillingEntity billing = billingDTO.getBillingId() != null ?
//                billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity()) :
//                new BillingEntity();
//
//        billing.setTotalAmount(billingDTO.getTotalAmount());
//        billing.setPaymentStatus(billingDTO.getPaymentStatus());
//        billing.setPaymentMethod(billingDTO.getPaymentMethod());
//        billing.setPaymentDate(billingDTO.getPaymentDate());
//        billing.setDiscount(billingDTO.getDiscount());
//        billing.setGstRate(billingDTO.getGstRate());
//        billing.setGstAmount(billingDTO.getGstAmount());
//        billing.setCgstAmount(billingDTO.getCgstAmount());
//        billing.setSgstAmount(billingDTO.getSgstAmount());
//        billing.setIgstAmount(billingDTO.getIgstAmount());
//        billing.setNetAmount(billingDTO.getNetAmount());
//        billing.getLabs().add(lab);
//        return billing;
//    }
//
//
//}


//===================================================
//
//
//package tiameds.com.tiameds.services.lab;
//import jakarta.transaction.Transactional;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import tiameds.com.tiameds.dto.lab.BillingDTO;
//import tiameds.com.tiameds.dto.lab.PatientDTO;
//import tiameds.com.tiameds.dto.lab.TestDiscountDTO;
//import tiameds.com.tiameds.dto.lab.VisitDTO;
//import tiameds.com.tiameds.entity.*;
//import tiameds.com.tiameds.repository.*;
//import tiameds.com.tiameds.utils.ApiResponseHelper;
//
//import java.math.BigDecimal;
//import java.util.*;
//import java.util.stream.Collectors;
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
//
//    public PatientService(LabRepository labRepository, TestRepository testRepository, HealthPackageRepository healthPackageRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, HealthPackageRepository packageRepository, InsuranceRepository insuranceRepository, BillingRepository billingRepository, TestDiscountRepository testDiscountRepository) {
//        this.labRepository = labRepository;
//        this.testRepository = testRepository;
//        this.healthPackageRepository = healthPackageRepository;
//        this.patientRepository = patientRepository;
//        this.doctorRepository = doctorRepository;
//        this.packageRepository = packageRepository;
//        this.insuranceRepository = insuranceRepository;
//        this.billingRepository = billingRepository;
//        this.testDiscountRepository = testDiscountRepository;
//    }
//
//    public boolean existsByPhone(String phone) {
//        return patientRepository.existsByPhone(phone);
//    }
//
//    //get all patients by lab id
//    public List<PatientDTO> getAllPatientsByLabId(Long labId) {
//        return patientRepository.findAllByLabsId(labId).stream()
//                .map(patient -> {
//                    PatientDTO patientDTO = new PatientDTO();
//                    patientDTO.setId(patient.getPatientId());
//                    patientDTO.setFirstName(patient.getFirstName());
//                    patientDTO.setLastName(patient.getLastName());
//                    patientDTO.setEmail(patient.getEmail());
//                    patientDTO.setPhone(patient.getPhone());
//                    patientDTO.setAddress(patient.getAddress());
//                    patientDTO.setCity(patient.getCity());
//                    patientDTO.setState(patient.getState());
//                    patientDTO.setZip(patient.getZip());
//                    patientDTO.setBloodGroup(patient.getBloodGroup());
//                    patientDTO.setDateOfBirth(patient.getDateOfBirth());
//                    patientDTO.setGender(patient.getGender());
//                    return patientDTO;
//                })
//                .collect(Collectors.toList());
//    }
//
//    //get patient by id of the lab
//    public Object getPatientById(Long patientId, Long labId) {
//
//        // Get the lab by ID
//        Optional<Lab> lab = labRepository.findById(labId);
//        if (lab.isEmpty()) {
//            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//        }
//
//        // Check if the patient exists and belongs to the given lab
//        Optional<PatientEntity> patient = patientRepository.findById(patientId);
//        if (patient.isEmpty() || !patient.get().getLabs().contains(lab.get())) {
//            return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
//        }
//
//        // Make the response
//        PatientEntity patientEntity = patient.get();
//        PatientDTO patientDTO = new PatientDTO();
//        patientDTO.setFirstName(patientEntity.getFirstName());
//        patientDTO.setLastName(patientEntity.getLastName());
//        patientDTO.setEmail(patientEntity.getEmail());
//        patientDTO.setPhone(patientEntity.getPhone());
//        patientDTO.setAddress(patientEntity.getAddress());
//        patientDTO.setCity(patientEntity.getCity());
//        patientDTO.setState(patientEntity.getState());
//        patientDTO.setZip(patientEntity.getZip());
//        patientDTO.setBloodGroup(patientEntity.getBloodGroup());
//        patientDTO.setDateOfBirth(patientEntity.getDateOfBirth());
//
//        return patientDTO;
//    }
//
//    public boolean existsById(Long patientId) {
//        return patientRepository.existsById(patientId);
//    }
//
//    public void updatePatient(Long patientId, Long labId, PatientDTO patientDTO) {
//        // Check if the lab exists
//        Lab lab = labRepository.findById(labId)
//                .orElseThrow(() -> new RuntimeException("Lab not found"));
//
//        // Check if the patient exists and belongs to the lab
//        PatientEntity patientEntity = patientRepository.findById(patientId)
//                .filter(patient -> patient.getLabs().stream()
//                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
//                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
//
//        // Update the patient details from the DTO
//        patientEntity.setFirstName(patientDTO.getFirstName());
//        patientEntity.setLastName(patientDTO.getLastName());
//        patientEntity.setEmail(patientDTO.getEmail());
//        patientEntity.setPhone(patientDTO.getPhone());
//        patientEntity.setAddress(patientDTO.getAddress());
//        patientEntity.setCity(patientDTO.getCity());
//        patientEntity.setState(patientDTO.getState());
//        patientEntity.setZip(patientDTO.getZip());
//        patientEntity.setBloodGroup(patientDTO.getBloodGroup());
//        patientEntity.setDateOfBirth(patientDTO.getDateOfBirth());
//
//        // Update lab relationship (optional, if labs can be updated)
//        patientEntity.getLabs().clear();
//        patientEntity.getLabs().add(lab);
//
//        // Save the updated patient
//        patientRepository.save(patientEntity);
//    }
//
//    public void deletePatient(Long patientId, Long labId) {
//        // Check if the lab exists
//        Lab lab = labRepository.findById(labId)
//                .orElseThrow(() -> new RuntimeException("Lab not found"));
//
//        // Check if the patient exists and belongs to the lab
//        PatientEntity patientEntity = patientRepository.findById(patientId)
//                .filter(patient -> patient.getLabs().stream()
//                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
//                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));
//
//        // Delete the patient
//        patientRepository.delete(patientEntity);
//    }
//
//
////   ====+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//    @Transactional
//    public Optional<PatientEntity> findByPhoneAndFirstName(String phone, String firstName) {
//        return patientRepository.findByPhoneAndFirstName(phone, firstName);
//    }
//    @Transactional
//    public PatientDTO savePatientWithDetails(Lab lab, PatientDTO patientDTO) {
//        // Check if patient exists (both phone AND first name must match)
//        Optional<PatientEntity> existingPatient = findByPhoneAndFirstName(
//                patientDTO.getPhone(),
//                patientDTO.getFirstName()
//        );
//        if (existingPatient.isPresent()) {
//            return addVisitAndBillingToExistingPatient(lab, patientDTO, existingPatient.get());
//        }
//        // Create new patient
//        PatientEntity patient = mapPatientDTOToEntity(patientDTO);
//
//        // Find guardian (first patient with this phone number)
//        Optional<PatientEntity> guardian = patientRepository.findFirstByPhoneOrderByPatientIdAsc(patientDTO.getPhone());
//        guardian.ifPresent(g -> patient.setGuardian(g));
//
//        // Set the lab for the patient
//        patient.getLabs().add(lab);
//
//        // Handle visit and billing if provided
//        if (patientDTO.getVisit() != null) {
//            VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab);
//            visit.setPatient(patient);
//            patient.getVisits().add(visit);
//        }
//        // Save the patient
//        patientRepository.save(patient);
//        return new PatientDTO(patient);
//    }
//
//    @Transactional
//    public PatientDTO addVisitAndBillingToExistingPatient(Lab lab, PatientDTO patientDTO, PatientEntity existingPatient) {
//        // Update patient details if needed
//        if (patientDTO.getLastName() != null) existingPatient.setLastName(patientDTO.getLastName());
//        if (patientDTO.getEmail() != null) existingPatient.setEmail(patientDTO.getEmail());
//        // Add other fields you want to update here...
//
//        // Add new visit if provided
//        if (patientDTO.getVisit() != null) {
//            VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab);
//            visit.setPatient(existingPatient);
//            existingPatient.getVisits().add(visit);
//        }
//        // Save updated patient
//        patientRepository.save(existingPatient);
//        return new PatientDTO(existingPatient);
//    }
//
//    private PatientEntity mapPatientDTOToEntity(PatientDTO dto) {
//        PatientEntity entity = new PatientEntity();
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
//        entity.setGender(dto.getGender());
//        return entity;
//    }
//
//    private VisitEntity mapVisitDTOToEntity(VisitDTO visitDTO, Lab lab) {
//        VisitEntity visit = new VisitEntity();
//        visit.setVisitDate(visitDTO.getVisitDate());
//        visit.setVisitType(visitDTO.getVisitType());
//        visit.setVisitStatus(visitDTO.getVisitStatus());
//        visit.setVisitDescription(visitDTO.getVisitDescription());
//
//        // Associate doctor if provided
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
//        // Associate tests
//        if (visitDTO.getTestIds() != null && !visitDTO.getTestIds().isEmpty()) {
//            List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
//            if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
//                throw new RuntimeException("Test does not belong to the lab");
//            }
//            visit.setTests(new HashSet<>(tests));
//        }
//        // Associate health packages
//        if (visitDTO.getPackageIds() != null && !visitDTO.getPackageIds().isEmpty()) {
//            List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
//            if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
//                throw new RuntimeException("Health package does not belong to the lab");
//            }
//            visit.setPackages(new HashSet<>(healthPackages));
//        }
//        // Associate insurance
//        if (visitDTO.getInsuranceIds() != null && !visitDTO.getInsuranceIds().isEmpty()) {
//            List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
//            if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
//                throw new RuntimeException("Insurance does not belong to the lab");
//            }
//            visit.setInsurance(new HashSet<>(insurance));
//        }
//        visit.getLabs().add(lab);
//        // Handle billing
//        if (visitDTO.getBilling() != null) {
//            BillingEntity billing = mapBillingDTOToEntity(visitDTO.getBilling(), lab);
//            visit.setBilling(billing);
//
//            //  Handle test discounts if provided
//            handleTestDiscounts(visit, visitDTO.getListofeachtestdiscount());
//        }
//        return visit;
//    }
//    private BillingEntity mapBillingDTOToEntity(BillingDTO billingDTO, Lab lab) {
//        BillingEntity billing = billingDTO.getBillingId() != null ?
//                billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity()) :
//                new BillingEntity();
//        billing.setTotalAmount(billingDTO.getTotalAmount());
//        billing.setPaymentStatus(billingDTO.getPaymentStatus());
//        billing.setPaymentMethod(billingDTO.getPaymentMethod());
//        billing.setPaymentDate(billingDTO.getPaymentDate());
//        billing.setDiscount(billingDTO.getDiscount());
//        billing.setGstRate(billingDTO.getGstRate());
//        billing.setGstAmount(billingDTO.getGstAmount());
//        billing.setCgstAmount(billingDTO.getCgstAmount());
//        billing.setSgstAmount(billingDTO.getSgstAmount());
//        billing.setIgstAmount(billingDTO.getIgstAmount());
//        billing.setNetAmount(billingDTO.getNetAmount());
//        billing.getLabs().add(lab);
//        // Ensure the billing entity is managed by the current transaction context
//        return billing;
//    }
//
//    //handle test discounts
//    public void handleTestDiscounts(VisitEntity visit, List<TestDiscountDTO> testDiscounts) {
//        if (testDiscounts != null && !testDiscounts.isEmpty()) {
//            Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
//                    .map(discountDTO -> {
//                        TestDiscountEntity discountEntity = new TestDiscountEntity();
//                        discountEntity.setTestId(discountDTO.getTestId());
//                        discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
//                        discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
//                        discountEntity.setFinalPrice(discountDTO.getFinalPrice());
//                        discountEntity.setCreatedBy("system");
//                        discountEntity.setUpdatedBy("system");
//                        discountEntity.setBilling(visit.getBilling()); // now managed
//                        return discountEntity;
//                    })
//                    .collect(Collectors.toSet());
//
//            testDiscountRepository.saveAll(discountEntities);
//            visit.setTestDiscounts(discountEntities); // if bi-directional
//        }
//    }
//}
//



//======================================================================================================================

package tiameds.com.tiameds.services.lab;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PutMapping;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.TestDiscountDTO;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.*;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientService {
    private final LabRepository labRepository;
    private final TestRepository testRepository;
    private final HealthPackageRepository healthPackageRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final HealthPackageRepository packageRepository;
    private final InsuranceRepository insuranceRepository;
    private final BillingRepository billingRepository;
    private final TestDiscountRepository testDiscountRepository;

    public PatientService(LabRepository labRepository,
                          TestRepository testRepository,
                          HealthPackageRepository healthPackageRepository,
                          PatientRepository patientRepository,
                          DoctorRepository doctorRepository,
                          HealthPackageRepository packageRepository,
                          InsuranceRepository insuranceRepository,
                          BillingRepository billingRepository,
                          TestDiscountRepository testDiscountRepository) {
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.packageRepository = packageRepository;
        this.insuranceRepository = insuranceRepository;
        this.billingRepository = billingRepository;
        this.testDiscountRepository = testDiscountRepository;
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
    public void updatePatient(Long patientId, Long labId, PatientDTO patientDTO) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        PatientEntity patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().stream()
                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));

        updatePatientEntityFromDTO(patientEntity, patientDTO);
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

    @Transactional
    public Optional<PatientEntity> findByPhoneAndFirstName(String phone, String firstName) {
        return patientRepository.findByPhoneAndFirstName(phone, firstName);
    }

    @Transactional(rollbackOn = Exception.class)
    public PatientDTO savePatientWithDetails(Lab lab, PatientDTO patientDTO) {
        try {
            Optional<PatientEntity> existingPatient = findByPhoneAndFirstName(
                    patientDTO.getPhone(),
                    patientDTO.getFirstName()
            );

            if (existingPatient.isPresent()) {
                return addVisitAndBillingToExistingPatient(lab, patientDTO, existingPatient.get());
            }

            PatientEntity patient = mapPatientDTOToEntity(patientDTO);
            Optional<PatientEntity> guardian = patientRepository.findFirstByPhoneOrderByPatientIdAsc(patientDTO.getPhone());
            guardian.ifPresent(patient::setGuardian);

            patient.getLabs().add(lab);

            if (patientDTO.getVisit() != null) {
                VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab);
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
    public PatientDTO addVisitAndBillingToExistingPatient(Lab lab, PatientDTO patientDTO, PatientEntity existingPatient) {
        if (patientDTO.getLastName() != null) existingPatient.setLastName(patientDTO.getLastName());
        if (patientDTO.getEmail() != null) existingPatient.setEmail(patientDTO.getEmail());

        if (patientDTO.getVisit() != null) {
            VisitEntity visit = mapVisitDTOToEntity(patientDTO.getVisit(), lab);
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
        patientDTO.setGender(patient.getGender());
        return patientDTO;
    }

    private void updatePatientEntityFromDTO(PatientEntity entity, PatientDTO dto) {
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
    }

    private PatientEntity mapPatientDTOToEntity(PatientDTO dto) {
        PatientEntity entity = new PatientEntity();
        updatePatientEntityFromDTO(entity, dto);
        entity.setGender(dto.getGender());
        return entity;
    }

    private VisitEntity mapVisitDTOToEntity(VisitDTO visitDTO, Lab lab) {
        VisitEntity visit = new VisitEntity();
        visit.setVisitDate(visitDTO.getVisitDate());
        visit.setVisitType(visitDTO.getVisitType());
        visit.setVisitStatus(visitDTO.getVisitStatus());
        visit.setVisitDescription(visitDTO.getVisitDescription());

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
            BillingEntity billing = mapBillingDTOToEntity(visitDTO.getBilling(), lab);
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
                            discountEntity.setCreatedBy("system");
                            discountEntity.setUpdatedBy("system");
                            discountEntity.setBilling(finalBilling); // now managed
                            return discountEntity;
                        })
                        .collect(Collectors.toSet());
                testDiscountRepository.saveAll(discountEntities);
            }
        }

        return visit;
    }

    private BillingEntity mapBillingDTOToEntity(BillingDTO billingDTO, Lab lab) {
        BillingEntity billing = billingDTO.getBillingId() != null ?
                billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity()) :
                new BillingEntity();
        billing.setTotalAmount(billingDTO.getTotalAmount() != null ? billingDTO.getTotalAmount() : BigDecimal.ZERO);
        billing.setPaymentStatus(billingDTO.getPaymentStatus() != null ? billingDTO.getPaymentStatus() : "UNPAID");
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
        billing.getLabs().add(lab);

        // handle test discounts if provided
        return billing;
    }


//    ----------------------------------------------------------------------------------------------

    @Transactional(rollbackOn = Exception.class)
    public PatientDTO updatePatientDetails(PatientEntity existingPatient, Lab lab, PatientDTO patientDTO) {
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

            // Handle visit update if provided
            if (patientDTO.getVisit() != null) {
                handleVisitUpdate(existingPatient, lab, patientDTO.getVisit());
            }

            // Save the updated patient
            PatientEntity savedPatient = patientRepository.save(existingPatient);
            return new PatientDTO(savedPatient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update patient: " + e.getMessage(), e);
        }
    }

    private void handleVisitUpdate(PatientEntity patient, Lab lab, VisitDTO visitDTO) {
        // Check if we're updating an existing visit or creating a new one
        if (visitDTO.getVisitId() != null) {
            // Update existing visit
            Optional<VisitEntity> existingVisitOpt = patient.getVisits().stream()
                    .filter(v -> v.getVisitId().equals(visitDTO.getVisitId()))
                    .findFirst();

            if (existingVisitOpt.isPresent()) {
                VisitEntity existingVisit = existingVisitOpt.get();
                updateVisitDetails(existingVisit, lab, visitDTO);
            } else {
                // Visit ID provided but not found - create new visit
                VisitEntity newVisit = mapVisitDTOToEntity(visitDTO, lab);
                newVisit.setPatient(patient);
                patient.getVisits().add(newVisit);
            }
        } else {
            // Create new visit
            VisitEntity newVisit = mapVisitDTOToEntity(visitDTO, lab);
            newVisit.setPatient(patient);
            patient.getVisits().add(newVisit);
        }
    }

    private void updateVisitDetails(VisitEntity visit, Lab lab, VisitDTO visitDTO) {
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
            updateBillingDetails(visit, lab, visitDTO.getBilling(), visitDTO.getListOfEachTestDiscount());
        }
    }

    private void updateBillingDetails(VisitEntity visit, Lab lab, BillingDTO billingDTO, List<TestDiscountDTO> testDiscounts) {
        BillingEntity billing = visit.getBilling();
        if (billing == null) {
            billing = new BillingEntity();
            visit.setBilling(billing);
        }

        // Update billing fields
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
        if (billingDTO.getGstRate() != null) {
            billing.setGstRate(billingDTO.getGstRate());
        }
        if (billingDTO.getGstAmount() != null) {
            billing.setGstAmount(billingDTO.getGstAmount());
        }
        if (billingDTO.getCgstAmount() != null) {
            billing.setCgstAmount(billingDTO.getCgstAmount());
        }
        if (billingDTO.getSgstAmount() != null) {
            billing.setSgstAmount(billingDTO.getSgstAmount());
        }
        if (billingDTO.getIgstAmount() != null) {
            billing.setIgstAmount(billingDTO.getIgstAmount());
        }
        if (billingDTO.getNetAmount() != null) {
            billing.setNetAmount(billingDTO.getNetAmount());
        }
        if (billingDTO.getDiscountReason() != null) {
            billing.setDiscountReason(billingDTO.getDiscountReason());
        }
        if (billingDTO.getDiscountPercentage() != null) {
            billing.setDiscount(billingDTO.getDiscountPercentage());
        }

        billing.getLabs().add(lab);
        billing = billingRepository.save(billing);

        // Handle test discounts
        if (testDiscounts != null && !testDiscounts.isEmpty()) {
            // First, remove existing discounts for this billing
            testDiscountRepository.deleteByBilling(billing);

            // Then add the new discounts
            BillingEntity finalBilling = billing;
            Set<TestDiscountEntity> discountEntities = testDiscounts.stream()
                    .map(discountDTO -> {
                        TestDiscountEntity discountEntity = new TestDiscountEntity();
                        discountEntity.setTestId(discountDTO.getTestId());
                        discountEntity.setDiscountAmount(discountDTO.getDiscountAmount());
                        discountEntity.setDiscountPercent(discountDTO.getDiscountPercent());
                        discountEntity.setFinalPrice(discountDTO.getFinalPrice());
                        discountEntity.setCreatedBy("system");
                        discountEntity.setUpdatedBy("system");
                        discountEntity.setBilling(finalBilling);
                        return discountEntity;
                    })
                    .collect(Collectors.toSet());
            testDiscountRepository.saveAll(discountEntities);
        }
    }


}