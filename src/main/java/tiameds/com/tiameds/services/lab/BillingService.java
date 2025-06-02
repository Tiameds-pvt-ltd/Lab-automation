package tiameds.com.tiameds.services.lab;

import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.BillingRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.repository.VisitRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private final BillingRepository billingRepository;
    private final LabRepository labRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;

    public BillingService(BillingRepository billingRepository, LabRepository labRepository, PatientRepository patientRepository, VisitRepository visitRepository) {
        this.billingRepository = billingRepository;
        this.labRepository = labRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
    }

    public List<BillingDTO> getBillingList(Long labId, Optional<User> currentUser, BillingDTO filterCriteria) {

        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new IllegalArgumentException("Lab not found");
        }

        Lab lab = labOptional.get();

        // Check if the user is authorized for the lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(lab)) {
            throw new SecurityException("User is not a member of this lab");
        }

        // Fetch the billing list
        List<BillingEntity> billingList = billingRepository.findAll();


        // Transform entities to DTOs
        return billingList.stream()
                .map(billingEntity -> {
                    BillingDTO dto = new BillingDTO();
                    dto.setBillingId(billingEntity.getId());
                    dto.setTotalAmount(billingEntity.getTotalAmount());
                    dto.setPaymentStatus(billingEntity.getPaymentStatus());
                    dto.setPaymentMethod(billingEntity.getPaymentMethod());
                    dto.setPaymentDate(billingEntity.getPaymentDate().toString()); // Ensure date is in string format
                    dto.setDiscount(billingEntity.getDiscount());
                    dto.setGstRate(billingEntity.getGstRate());
                    dto.setGstAmount(billingEntity.getGstAmount());
                    dto.setCgstAmount(billingEntity.getCgstAmount());
                    dto.setSgstAmount(billingEntity.getSgstAmount());
                    dto.setIgstAmount(billingEntity.getIgstAmount());
                    dto.setNetAmount(billingEntity.getNetAmount());
                    return dto;
                })
                .collect(Collectors.toList());
    }



    public List<BillingDTO> getBillingDetailsByPatientId(Long labId, Optional<User> currentUser, Long patientId) {

        // Check if the lab exists
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            throw new RuntimeException("Lab not found");
        }

        // Check if the user is authorized for the lab
        if (currentUser.isEmpty() || !currentUser.get().getLabs().contains(labOptional.get())) {
            throw new RuntimeException("User is not authorized for this lab");
        }

        // Check if the patient exists
        Optional<PatientEntity> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isEmpty()) {
            throw new RuntimeException("Patient not found");
        }

        // Ensure the patient belongs to the lab
        PatientEntity patientEntity = patientRepository.findById(patientId)
                .filter(patient -> patient.getLabs().stream()
                        .anyMatch(existingLab -> Objects.equals(existingLab.getId(), labId)))
                .orElseThrow(() -> new RuntimeException("Patient not found for the specified lab"));

        // Fetch visits associated with the patient
        List<VisitEntity> visits = visitRepository.findByPatientId(patientId);

        // Extract and map billing details from visits
        // Include only visits with billing information

        return visits.stream()
                .map(VisitEntity::getBilling)
                .filter(Objects::nonNull) // Include only visits with billing information
                .map(billing -> new BillingDTO(
                        billing.getId(),
                        billing.getTotalAmount(),
                        billing.getPaymentStatus(),
                        billing.getPaymentMethod(),
                        billing.getPaymentDate() != null ? billing.getPaymentDate().toString() : null,
                        billing.getDiscount(),
                        billing.getGstRate(),
                        billing.getGstAmount(),
                        billing.getCgstAmount(),
                        billing.getSgstAmount(),
                        billing.getIgstAmount(),
                        billing.getNetAmount(),
                        billing.getDiscountReason()
                ))
                .collect(Collectors.toList());
    }
}


