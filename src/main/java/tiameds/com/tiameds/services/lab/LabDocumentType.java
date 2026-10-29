package tiameds.com.tiameds.services.lab;

import tiameds.com.tiameds.entity.Lab;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Lab files stored in S3 whose URL is kept on the labs table.
 * The multipart part name / delete query value is {@link #getParam()}.
 */
public enum LabDocumentType {

    LAB_LOGO("labLogo", "lab-logo", Lab::getLabLogo, Lab::setLabLogo),
    ORGANIZATION_GSTIN_CERTIFICATE("organizationGstinCertificate", "organization-gstin-certificate",
            Lab::getOrganizationGstinCertificate, Lab::setOrganizationGstinCertificate),
    GSTIN_CERTIFICATE("gstinCertificate", "gstin-certificate", Lab::getGstinCertificate, Lab::setGstinCertificate),
    ESTABLISHMENT_REGISTRATION_CERTIFICATE("establishmentRegistrationCertificate", "establishment-registration-certificate",
            Lab::getEstablishmentRegistrationCertificate, Lab::setEstablishmentRegistrationCertificate),
    BIOMEDICAL_WASTE_AUTHORIZATION_CERTIFICATE("biomedicalWasteAuthorizationCertificate", "biomedical-waste-authorization-certificate",
            Lab::getBiomedicalWasteAuthorizationCertificate, Lab::setBiomedicalWasteAuthorizationCertificate),
    NABL_ACCREDITATION_CERTIFICATE("nablAccreditationCertificate", "nabl-accreditation-certificate",
            Lab::getNablAccreditationCertificate, Lab::setNablAccreditationCertificate);

    private final String param;
    private final String folder;
    private final Function<Lab, String> getter;
    private final BiConsumer<Lab, String> setter;

    LabDocumentType(String param, String folder, Function<Lab, String> getter, BiConsumer<Lab, String> setter) {
        this.param = param;
        this.folder = folder;
        this.getter = getter;
        this.setter = setter;
    }

    public String getParam() {
        return param;
    }

    public String getFolder() {
        return folder;
    }

    public String getUrl(Lab lab) {
        return getter.apply(lab);
    }

    public void setUrl(Lab lab, String url) {
        setter.accept(lab, url);
    }

    public static LabDocumentType fromParam(String param) {
        return Arrays.stream(values())
                .filter(type -> type.param.equalsIgnoreCase(param))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown document type: " + param));
    }
}
