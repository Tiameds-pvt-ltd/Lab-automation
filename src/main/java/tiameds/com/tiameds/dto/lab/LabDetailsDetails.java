package tiameds.com.tiameds.dto.lab;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabDetailsDetails {
    private Long id;
    private String name;
    private String logo;
    private String address;
    private String city;
    private String state;
    private Boolean isActive;
    private String description;
    private String labZip;
    private String labCountry;
    private String labPhone;
    private String labEmail;
    private String licenseNumber;
    private String labType;
    private String createdByName;
    private String createdAt;
    private String updatedAt;
    private String directorName;
    private String directorEmail;
    private String directorPhone;
    private String certificationBody;
    private String labCertificate;
    private String directorGovtId;
    private String labBusinessRegistration;
    private String labLicense;
    private String taxId;
    private String labAccreditation;
    private Boolean dataPrivacyAgreement;
}
