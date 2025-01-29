package tiameds.com.tiameds.services.lab;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.*;
import tiameds.com.tiameds.utils.ApiResponseHelper;

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

    public VisitService(PatientRepository patientRepository,
                        LabRepository labRepository,
                        TestRepository testRepository,
                        HealthPackageRepository healthPackageRepository,
                        DoctorRepository doctorRepository,
                        InsuranceRepository insuranceRepository,
                        BillingRepository billingRepository,
                        VisitRepository visitRepository) {
        this.patientRepository = patientRepository;
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.doctorRepository = doctorRepository;
        this.insuranceRepository = insuranceRepository;
        this.billingRepository = billingRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public void addVisit(Long labId, Long patientId, VisitDTO visitDTO, Optional<User> currentUser) {

        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            ApiResponseHelper.successResponseWithDataAndMessage("Lab not found", HttpStatus.NOT_FOUND, null);
        }

        // Check if the user is a member of the lab
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

        // Create the visit entity
        VisitEntity visit = new VisitEntity();
        visit.setPatient(patientEntity.get());
        visit.setVisitDate(visitDTO.getVisitDate());
        visit.setVisitType(visitDTO.getVisitType());
        visit.setVisitStatus(visitDTO.getVisitStatus());
        visit.setVisitDescription(visitDTO.getVisitDescription());
        visit.setDoctor(doctorOptional.get());

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
        billingEntity.setTotalAmount(visitDTO.getBilling().getTotalAmount());
        billingEntity.setPaymentStatus(visitDTO.getBilling().getPaymentStatus());
        billingEntity.setPaymentMethod(visitDTO.getBilling().getPaymentMethod());
        billingEntity.setPaymentDate(visitDTO.getBilling().getPaymentDate());
        billingEntity.setDiscount(visitDTO.getBilling().getDiscount());
        billingEntity.setGstRate(visitDTO.getBilling().getGstRate());
        billingEntity.setGstAmount(visitDTO.getBilling().getGstAmount());
        billingEntity.setCgstAmount(visitDTO.getBilling().getCgstAmount());
        billingEntity.setSgstAmount(visitDTO.getBilling().getSgstAmount());
        billingEntity.setIgstAmount(visitDTO.getBilling().getIgstAmount());
        billingEntity.setNetAmount(visitDTO.getBilling().getNetAmount());

        billingRepository.save(billingEntity);
        visit.setBilling(billingEntity);

        // Save the visit
        visitRepository.save(visit);


    }

    public List<PatientDTO> getVisits(Long labId, Optional<User> currentUser) {
        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }

        // Check if the user is a member of the lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a member of this lab");
        }

        // Get the list of visits
        List<VisitEntity> visits = visitRepository.findAllByPatient_Labs(labOptional.get());

        // Map entities to DTOs
        return visits.stream()
                .map(this::mapVisitToPatientDTO)
                .collect(Collectors.toList());
    }


    private PatientDTO mapVisitToPatientDTO(VisitEntity visitEntity) {
        // Map patient details
        PatientDTO patientDTO = new PatientDTO();
        patientDTO.setId(visitEntity.getPatient().getPatientId());
        patientDTO.setFirstName(visitEntity.getPatient().getFirstName());
        patientDTO.setLastName(visitEntity.getPatient().getLastName());
        patientDTO.setEmail(visitEntity.getPatient().getEmail());
        patientDTO.setPhone(visitEntity.getPatient().getPhone());
        patientDTO.setAddress(visitEntity.getPatient().getAddress());
        patientDTO.setCity(visitEntity.getPatient().getCity());
        patientDTO.setState(visitEntity.getPatient().getState());
        patientDTO.setZip(visitEntity.getPatient().getZip());
        patientDTO.setBloodGroup(visitEntity.getPatient().getBloodGroup());
        patientDTO.setDateOfBirth(visitEntity.getPatient().getDateOfBirth());
        patientDTO.setGender(visitEntity.getPatient().getGender());

        // Map visit details
        VisitDTO visitDTO = new VisitDTO();
        visitDTO.setVisitId(visitEntity.getVisitId());
        visitDTO.setVisitDate(visitEntity.getVisitDate());
        visitDTO.setVisitType(visitEntity.getVisitType());
        visitDTO.setVisitStatus(visitEntity.getVisitStatus());
        visitDTO.setVisitDescription(visitEntity.getVisitDescription());
        visitDTO.setDoctorId(visitEntity.getDoctor().getId());
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
            billingDTO.setGstRate(visitEntity.getBilling().getGstRate());
            billingDTO.setGstAmount(visitEntity.getBilling().getGstAmount());
            billingDTO.setCgstAmount(visitEntity.getBilling().getCgstAmount());
            billingDTO.setSgstAmount(visitEntity.getBilling().getSgstAmount());
            billingDTO.setIgstAmount(visitEntity.getBilling().getIgstAmount());
            billingDTO.setNetAmount(visitEntity.getBilling().getNetAmount());
            visitDTO.setBilling(billingDTO);
        }

        // Attach visit to patient DTO
        patientDTO.setVisit(visitDTO);

        return patientDTO;
    }

    @Transactional
    public void updateVisit(Long labId, Long visitId, VisitDTO visitDTO, Optional<User> currentUser) {

        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the user is a member of the lab
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

        // Update the visit
        visit.setVisitDate(visitDTO.getVisitDate());
        visit.setVisitType(visitDTO.getVisitType());
        visit.setVisitStatus(visitDTO.getVisitStatus());
        visit.setVisitDescription(visitDTO.getVisitDescription());
        visit.setDoctor(doctorOptional.get());

        // Set tests
        Set<Test> tests = testRepository.findAllById(visitDTO.getTestIds()).stream().collect(Collectors.toSet());
        visit.setTests(tests);

        // Set health packages
        Set<HealthPackage> healthPackages = healthPackageRepository.findAllById(visitDTO.getPackageIds()).stream().collect(Collectors.toSet());
        visit.setPackages(healthPackages);

        // Set insurances
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
        billingEntity.setGstRate(visitDTO.getBilling().getGstRate());
        billingEntity.setGstAmount(visitDTO.getBilling().getGstAmount());

        billingEntity.setCgstAmount(visitDTO.getBilling().getCgstAmount());
        billingEntity.setSgstAmount(visitDTO.getBilling().getSgstAmount());
        billingEntity.setIgstAmount(visitDTO.getBilling().getIgstAmount());
        billingEntity.setNetAmount(visitDTO.getBilling().getNetAmount());

        billingRepository.save(billingEntity);
        visit.setBilling(billingEntity);
        // Save the visit
        visitRepository.save(visit);

    }

    // delete the visit
    public void deleteVisit(Long labId, Long visitId, Optional<User> currentUser) {
        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the user is a member of the lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Check if the visit exists
        Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }

        VisitEntity visit = visitRepository.findById(visitId)
                .filter(visitEntity -> visitEntity.getPatient().getLabs().contains(labOptional.get()))
                .orElseThrow(() -> new IllegalArgumentException("Visit not found or does not belong to the lab"));

        visitRepository.delete(visit);
    }

    // get the visit details
    public Object getVisit(Long labId, Long visitId, Optional<User> currentUser) {
        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the user is a member of the lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Check if the visit exists
        Optional<VisitEntity> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }

        VisitEntity visit = visitRepository.findById(visitId)
                .filter(visitEntity -> visitEntity.getPatient().getLabs().contains(labOptional.get()))
                .orElseThrow(() -> new IllegalArgumentException("Visit not found or does not belong to the lab"));


        // Map visit to PatientDTO
        PatientDTO patientDTO = mapVisitToPatientDTO(visit);

        return patientDTO;
    }

    public Object getVisitByPatient(Long labId, Long patientId, Optional<User> currentUser) {
        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the user is a member of the lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Check if the patient belongs to the lab
        Optional<PatientEntity> patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().contains(labOptional.get()));
        if (patientEntity.isEmpty()) {
            return ApiResponseHelper.errorResponse("Patient not belong to the lab", HttpStatus.BAD_REQUEST);
        }


        // Get the list of visits
        List<VisitEntity> visits = visitRepository.findAllByPatient(patientEntity.get());

        // Map visits to PatientDTO
        List<PatientDTO> patientDTOList = visits.stream()
                .map(this::mapVisitToPatientDTO)
                .collect(Collectors.toList());

        return patientDTOList;
    }
}

