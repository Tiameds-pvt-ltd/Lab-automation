package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.DoctorDTO;
import tiameds.com.tiameds.entity.Doctors;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.repository.DoctorRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.utils.UserAuthService;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final LabRepository labRepository;
    private final UserAuthService userAuthService;

    public DoctorService(DoctorRepository doctorRepository, LabRepository labRepository, UserAuthService userAuthService) {
        this.doctorRepository = doctorRepository;
        this.labRepository = labRepository;
        this.userAuthService = userAuthService;
    }

    // Add doctor to lab
    public void addDoctorToLab(Long labId, DoctorDTO doctorDTO, String username) {
        // Retrieve the lab and authenticate the user
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        // Verify if the doctor already exists in the lab
//        List<Doctors> existingDoctors = lab.getDoctors().stream()
//                .filter(doctor -> doctor.getEmail().equals(doctorDTO.getEmail()))
//                .collect(Collectors.toList());

        List<Doctors> existingDoctors  = lab.getDoctors().stream()
                .filter(doctors -> doctors.getName().equals(doctorDTO.getName()))
                .collect(Collectors.toList());

        if (!existingDoctors.isEmpty()) {
            throw new RuntimeException("Doctor already exists in this lab");
        }
        // Create a new doctor and add to lab
        Doctors doctor = new Doctors();
        doctor.setName(doctorDTO.getName());
        doctor.setEmail(doctorDTO.getEmail());
        doctor.setSpeciality(doctorDTO.getSpeciality());
        doctor.setQualification(doctorDTO.getQualification());
        doctor.setHospitalAffiliation(doctorDTO.getHospitalAffiliation());
        doctor.setLicenseNumber(doctorDTO.getLicenseNumber());
        doctor.setPhone(doctorDTO.getPhone());
        doctor.setAddress(doctorDTO.getAddress());
        doctor.setCity(doctorDTO.getCity());
        doctor.setState(doctorDTO.getState());
        doctor.setCountry(doctorDTO.getCountry());
        doctor.getLabs().add(lab);
        doctor.setCreatedBy(username);
        doctorRepository.save(doctor);
        // Add the doctor to the lab
        lab.getDoctors().add(doctor);
        labRepository.save(lab);
    }

    public void updateDoctor(Long labId, Long doctorId, DoctorDTO doctorDTO, String username) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        Doctors doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        if (!lab.getDoctors().contains(doctor)) {
            throw new RuntimeException("Doctor not found in this lab");
        }
        // Update the doctor details
        doctor.setName(doctorDTO.getName());
        doctor.setEmail(doctorDTO.getEmail());
        doctor.setSpeciality(doctorDTO.getSpeciality());
        doctor.setQualification(doctorDTO.getQualification());
        doctor.setHospitalAffiliation(doctorDTO.getHospitalAffiliation());
        doctor.setLicenseNumber(doctorDTO.getLicenseNumber());
        doctor.setPhone(doctorDTO.getPhone());
        doctor.setAddress(doctorDTO.getAddress());
        doctor.setCity(doctorDTO.getCity());
        doctor.setState(doctorDTO.getState());
        doctor.setCountry(doctorDTO.getCountry());
        doctor.setUpdatedBy(username);
        doctorRepository.save(doctor);
    }

    public void deleteDoctor(Long labId, Long doctorId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        Doctors doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        lab.getDoctors().remove(doctor);
        labRepository.save(lab);
        doctorRepository.delete(doctor);
    }

    public Object getAllDoctors(Long labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        return lab.getDoctors().stream()
                .map(doctor -> {
                    DoctorDTO doctorDTO = new DoctorDTO();
                    doctorDTO.setId(doctor.getId());
                    doctorDTO.setName(doctor.getName());
                    doctorDTO.setEmail(doctor.getEmail());
                    doctorDTO.setSpeciality(doctor.getSpeciality());
                    doctorDTO.setQualification(doctor.getQualification());
                    doctorDTO.setHospitalAffiliation(doctor.getHospitalAffiliation());
                    doctorDTO.setLicenseNumber(doctor.getLicenseNumber());
                    doctorDTO.setPhone(doctor.getPhone());
                    doctorDTO.setAddress(doctor.getAddress());
                    doctorDTO.setCity(doctor.getCity());
                    doctorDTO.setState(doctor.getState());
                    doctorDTO.setCountry(doctor.getCountry());
                    doctorDTO.setCreatedAt(doctor.getCreatedAt());
                    doctorDTO.setUpdatedAt(doctor.getUpdatedAt());
                    return doctorDTO;
                })
                .collect(Collectors.toList());
    }

    public Object getDoctorById(Long labId, Long doctorId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new RuntimeException("Lab not found"));
        Doctors doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        if (!lab.getDoctors().contains(doctor)) {
            throw new RuntimeException("Doctor not found in this lab");
        }
        DoctorDTO doctorDTO = new DoctorDTO();
        doctorDTO.setId(doctor.getId());
        doctorDTO.setName(doctor.getName());
        doctorDTO.setEmail(doctor.getEmail());
        doctorDTO.setSpeciality(doctor.getSpeciality());
        doctorDTO.setQualification(doctor.getQualification());
        doctorDTO.setHospitalAffiliation(doctor.getHospitalAffiliation());
        doctorDTO.setLicenseNumber(doctor.getLicenseNumber());
        doctorDTO.setPhone(doctor.getPhone());
        doctorDTO.setAddress(doctor.getAddress());
        doctorDTO.setCity(doctor.getCity());
        doctorDTO.setState(doctor.getState());
        doctorDTO.setCountry(doctor.getCountry());
        doctorDTO.setCreatedAt(doctor.getCreatedAt());
        doctorDTO.setUpdatedAt(doctor.getUpdatedAt());
        return doctorDTO;
    }
}
