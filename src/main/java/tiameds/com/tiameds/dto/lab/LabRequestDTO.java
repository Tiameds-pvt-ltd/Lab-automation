package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabRequestDTO {
    private String name;
    private String address;
    private String city;
    private String state;
    private Boolean isActive;
    private String description;

//======================new feilds=========================
private String labLogo;
    private String licenseNumber;
    private String labType;
    private String labZip;
    private String labCountry;
    private String labPhone;
    private String labEmail;
    private String directorName;
    private String directorEmail;
    private String directorPhone;
    private String certificationBody;
    private String labCertificate ;  // Changed to match entity field name
    private String directorGovtId;
    private String labBusinessRegistration;
    private String labLicense;
    private String taxId;
    private String labAccreditation;
    private Boolean dataPrivacyAgreement;

}
