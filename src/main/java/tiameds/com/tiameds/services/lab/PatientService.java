package tiameds.com.tiameds.services.lab;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.*;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    public PatientService(LabRepository labRepository, TestRepository testRepository, HealthPackageRepository healthPackageRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, HealthPackageRepository packageRepository, InsuranceRepository insuranceRepository, BillingRepository billingRepository) {
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.packageRepository = packageRepository;
        this.insuranceRepository = insuranceRepository;
        this.billingRepository = billingRepository;
    }

    @Transactional
    public Optional<PatientEntity> findByPhoneOrEmail(String phone, String email) {
        return patientRepository.findByPhoneOrEmail(phone, email);
    }

    @Transactional
    public PatientDTO savePatientWithDetails(Lab lab, PatientDTO patientDTO) {
        PatientEntity patient = new PatientEntity();
        patient.setFirstName(patientDTO.getFirstName());
        patient.setLastName(patientDTO.getLastName());
        patient.setEmail(patientDTO.getEmail());
        patient.setPhone(patientDTO.getPhone());
        patient.setAddress(patientDTO.getAddress());
        patient.setCity(patientDTO.getCity());
        patient.setState(patientDTO.getState());
        patient.setZip(patientDTO.getZip());
        patient.setBloodGroup(patientDTO.getBloodGroup());
        patient.setDateOfBirth(patientDTO.getDateOfBirth());
        patient.setGender(patientDTO.getGender());

        // Set the lab for the patient
        patient.getLabs().add(lab);

        // Handle visit and billing (if provided)
        if (patientDTO.getVisit() != null) {
            VisitDTO visitDTO = patientDTO.getVisit();
            VisitEntity visit = mapVisitDTOToEntity(visitDTO, lab);
            visit.setPatient(patient);
            patient.getVisits().add(visit);
        }

        // Save the patient and related entities
        patientRepository.save(patient);

        return new PatientDTO(patient);
    }

//    @Transactional
    public PatientDTO addVisitAndBillingToExistingPatient(Lab lab, PatientDTO patientDTO, PatientEntity existingPatient) {
        VisitDTO visitDTO = patientDTO.getVisit();
        if (visitDTO != null) {
            VisitEntity visit = mapVisitDTOToEntity(visitDTO, lab);
            visit.setPatient(existingPatient);
            existingPatient.getVisits().add(visit);
        }

        // Save updated patient
        patientRepository.save(existingPatient);

        return new PatientDTO(existingPatient);
    }

    private VisitEntity mapVisitDTOToEntity(VisitDTO visitDTO, Lab lab) {
        VisitEntity visit = new VisitEntity();
        visit.setVisitDate(visitDTO.getVisitDate());
        visit.setVisitType(visitDTO.getVisitType());
        visit.setVisitStatus(visitDTO.getVisitStatus());
        visit.setVisitDescription(visitDTO.getVisitDescription());

        // Associate doctor
//        Doctors doctor = doctorRepository.findById(visitDTO.getDoctorId())
//                .orElseThrow(() -> new RuntimeException("Doctor not found"));
//        if (!lab.getDoctors().contains(doctor)) {
//            throw new RuntimeException("Doctor does not belong to the lab");
//        }
//        visit.setDoctor(doctor);

        // Associate doctor only if doctorId is provided

        if (visitDTO.getDoctorId() != null) {
            // Find the doctor by ID
            Optional<Doctors> doctorOpt = doctorRepository.findById(visitDTO.getDoctorId());

            if (doctorOpt.isPresent()) {
                Doctors doctor = doctorOpt.get();

                // Check if the doctor belongs to the lab
                if (!lab.getDoctors().contains(doctor)) {
                    throw new RuntimeException("Doctor does not belong to the lab");
                }

                // Set the doctor if everything is valid
                visit.setDoctor(doctor);
            } else {
                // If no doctor is found, set the doctor to null
                visit.setDoctor(null);
            }
        } else {
            // Handle case where no doctorId is provided
            visit.setDoctor(null); // Doctor will be null if no ID is provided
        }

        // Associate tests
        List<Test> tests = testRepository.findAllById(visitDTO.getTestIds());
        if (tests.stream().anyMatch(test -> !lab.getTests().contains(test))) {
            throw new RuntimeException("Test does not belong to the lab");
        }
        visit.setTests(new HashSet<>(tests));

        // Associate health packages
        List<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds());
        if (healthPackages.stream().anyMatch(pkg -> !lab.getHealthPackages().contains(pkg))) {
            throw new RuntimeException("Health package does not belong to the lab");
        }
        visit.setPackages(new HashSet<>(healthPackages));

        // Associate insurance
        List<InsuranceEntity> insurance = insuranceRepository.findAllById(visitDTO.getInsuranceIds());
        if (insurance.stream().anyMatch(ins -> !lab.getInsurance().contains(ins))) {
            throw new RuntimeException("Insurance does not belong to the lab");
        }
        visit.setInsurance(new HashSet<>(insurance));
        visit.getLabs().add(lab);

        // Handle billing details and ensure it's attached to the current transaction context
        BillingDTO billingDTO = visitDTO.getBilling();
        if (billingDTO != null) {
            BillingEntity billing;

            // Check if billingId is provided (Long instead of long)
            if (billingDTO.getBillingId() != null) {
                billing = billingRepository.findById(billingDTO.getBillingId()).orElse(new BillingEntity());
            } else {
                billing = new BillingEntity(); // Create new if no billingId
            }

            // Set billing properties
            billing.setId(billingDTO.getBillingId());
            billing.setTotalAmount(billingDTO.getTotalAmount());
            billing.setPaymentStatus(billingDTO.getPaymentStatus());
            billing.setPaymentMethod(billingDTO.getPaymentMethod());
            billing.setPaymentDate(billingDTO.getPaymentDate());
            billing.setDiscount(billingDTO.getDiscount());
            billing.setGstRate(billingDTO.getGstRate());
            billing.setGstAmount(billingDTO.getGstAmount());
            billing.setCgstAmount(billingDTO.getCgstAmount());
            billing.setSgstAmount(billingDTO.getSgstAmount());
            billing.setIgstAmount(billingDTO.getIgstAmount());
            billing.setNetAmount(billingDTO.getNetAmount());
            billing.getLabs().add(lab);

            // Attach the billing entity to the visit
            visit.setBilling(billing);
        }
        return visit;
    }
//    ==================

    public boolean existsByPhone(String phone) {
        return patientRepository.existsByPhone(phone);
    }


    //get all patients by lab id
    public List<PatientDTO> getAllPatientsByLabId(Long labId) {
        return patientRepository.findAllByLabsId(labId).stream()
                .map(patient -> {
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
                })
                .collect(Collectors.toList());
    }


    //get patient by id of the lab
    public Object getPatientById(Long patientId, Long labId) {

        // Get the lab by ID
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the patient exists and belongs to the given lab
        Optional<PatientEntity> patient = patientRepository.findById(patientId);
        if (patient.isEmpty() || !patient.get().getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
        }

        // Make the response
        PatientEntity patientEntity = patient.get();
        PatientDTO patientDTO = new PatientDTO();
        patientDTO.setFirstName(patientEntity.getFirstName());
        patientDTO.setLastName(patientEntity.getLastName());
        patientDTO.setEmail(patientEntity.getEmail());
        patientDTO.setPhone(patientEntity.getPhone());
        patientDTO.setAddress(patientEntity.getAddress());
        patientDTO.setCity(patientEntity.getCity());
        patientDTO.setState(patientEntity.getState());
        patientDTO.setZip(patientEntity.getZip());
        patientDTO.setBloodGroup(patientEntity.getBloodGroup());
        patientDTO.setDateOfBirth(patientEntity.getDateOfBirth());

        return patientDTO;
    }

    public boolean existsById(Long patientId) {
        return patientRepository.existsById(patientId);
    }

    public void updatePatient(Long patientId, Long labId, PatientDTO patientDTO) {
        // Check if the lab exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        // Check if the patient exists and belongs to the lab
        PatientEntity patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().stream()
                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));

        // Update the patient details from the DTO
        patientEntity.setFirstName(patientDTO.getFirstName());
        patientEntity.setLastName(patientDTO.getLastName());
        patientEntity.setEmail(patientDTO.getEmail());
        patientEntity.setPhone(patientDTO.getPhone());
        patientEntity.setAddress(patientDTO.getAddress());
        patientEntity.setCity(patientDTO.getCity());
        patientEntity.setState(patientDTO.getState());
        patientEntity.setZip(patientDTO.getZip());
        patientEntity.setBloodGroup(patientDTO.getBloodGroup());
        patientEntity.setDateOfBirth(patientDTO.getDateOfBirth());

        // Update lab relationship (optional, if labs can be updated)
        patientEntity.getLabs().clear();
        patientEntity.getLabs().add(lab);

        // Save the updated patient
        patientRepository.save(patientEntity);
    }

    public void deletePatient(Long patientId, Long labId) {
        // Check if the lab exists
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));

        // Check if the patient exists and belongs to the lab
        PatientEntity patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().stream()
                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));

        // Delete the patient
        patientRepository.delete(patientEntity);
    }
}
