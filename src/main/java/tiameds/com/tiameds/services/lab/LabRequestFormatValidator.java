package tiameds.com.tiameds.services.lab;

import tiameds.com.tiameds.dto.lab.LabRequestDTO;

import java.util.regex.Pattern;

/**
 * Length / format checks for the onboarding fields of {@link LabRequestDTO}, shared by add-lab and update-lab.
 * Blank values are skipped (a blank clears an optional field). A violation throws IllegalArgumentException,
 * which the controllers return as 400.
 *
 * Plain code on purpose: the project does not run bean validation, so annotations on the DTO would not be enforced.
 */
final class LabRequestFormatValidator {

    private static final Pattern PIN_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private LabRequestFormatValidator() {
    }

    static void validate(LabRequestDTO dto) {
        LabRequestDTO.OrganizationInfo org = dto.getOrganization();
        if (org != null) {
            checkMax("Organization name", org.getName(), 100);
            checkMax("Organization type", org.getType(), 50);
            checkMax("Organization phone", org.getPhone(), 20);
            checkMax("Organization address line 1", org.getAddressLine1(), 100);
            checkMax("Organization address line 2", org.getAddressLine2(), 100);
            checkMax("Organization country", org.getCountry(), 100);
            checkMax("Organization state", org.getState(), 100);
            checkMax("Organization district", org.getDistrict(), 100);
            checkMax("Organization city", org.getCity(), 50);
            checkPattern("Organization email must be valid", EMAIL_PATTERN, org.getEmail());
            checkPattern("Organization PIN code must be a valid 6-digit PIN code", PIN_PATTERN, org.getPinCode());
            checkPattern("Organization PAN must be a valid PAN (e.g. ABCDE1234F)", PAN_PATTERN, org.getPan());
            checkPattern("Organization GSTIN must be a valid 15-character GSTIN", GSTIN_PATTERN, org.getGstin());
        }

        checkMax("Lab address line 2", dto.getLabAddressLine2(), 100);
        checkMax("Lab district", dto.getLabDistrict(), 100);
        checkMax("Establishment registration number", dto.getEstablishmentRegistrationNumber(), 50);
        checkMax("Issuing authority", dto.getIssuingAuthority(), 25);
        checkMax("Biomedical waste authorization number", dto.getBiomedicalWasteAuthorizationNumber(), 25);
        checkMax("Terms version", dto.getTermsVersion(), 20);
        checkPattern("Lab PAN must be a valid PAN (e.g. ABCDE1234F)", PAN_PATTERN, dto.getLabPan());
        checkPattern("Lab GSTIN must be a valid 15-character GSTIN", GSTIN_PATTERN, dto.getLabGstin());
    }

    private static void checkMax(String field, String value, int max) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
    }

    private static void checkPattern(String message, Pattern pattern, String value) {
        if (value != null && !value.isBlank() && !pattern.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(message);
        }
    }
}
