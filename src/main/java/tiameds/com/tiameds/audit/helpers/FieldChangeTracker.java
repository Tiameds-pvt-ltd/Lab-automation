package tiameds.com.tiameds.audit.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.DoctorDTO;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.SampleDto;
import tiameds.com.tiameds.dto.lab.TestDTO;
import tiameds.com.tiameds.dto.visits.BillDTO;
import tiameds.com.tiameds.entity.PatientEntity;

import java.math.BigDecimal;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for tracking field changes between old and new entity states.
 * Designed to be extensible for other entity types in the future.
 */
@Slf4j
@Component
public class FieldChangeTracker {

    private final ObjectMapper objectMapper;

    public FieldChangeTracker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Compares PatientEntity and PatientDTO to identify changed fields.
     * Returns a map of changed fields with old and new values.
     * 
     * @param oldPatient The old patient entity
     * @param newPatient The new patient DTO
     * @return Map of changed fields with old/new values, empty if no changes
     */
    public Map<String, Object> comparePatientFields(PatientEntity oldPatient, PatientDTO newPatient) {
        PatientDTO oldSnapshot = oldPatient != null ? new PatientDTO(oldPatient) : null;
        return comparePatientFields(oldSnapshot, newPatient);
    }

    /**
     * Compares two PatientDTO instances to identify changed fields.
     * Useful when the old state is captured as a DTO snapshot.
     */
    public Map<String, Object> comparePatientFields(PatientDTO oldPatient, PatientDTO newPatient) {
        Map<String, Object> changes = new HashMap<>();

        if (oldPatient == null || newPatient == null) {
            return changes;
        }

        // Compare each field
        compareField(changes, "firstName", oldPatient.getFirstName(), newPatient.getFirstName());
        compareField(changes, "lastName", oldPatient.getLastName(), newPatient.getLastName());
        compareField(changes, "email", oldPatient.getEmail(), newPatient.getEmail());
        compareField(changes, "phone", oldPatient.getPhone(), newPatient.getPhone());
        compareField(changes, "address", oldPatient.getAddress(), newPatient.getAddress());
        compareField(changes, "city", oldPatient.getCity(), newPatient.getCity());
        compareField(changes, "state", oldPatient.getState(), newPatient.getState());
        compareField(changes, "zip", oldPatient.getZip(), newPatient.getZip());
        compareField(changes, "bloodGroup", oldPatient.getBloodGroup(), newPatient.getBloodGroup());
        compareField(changes, "dateOfBirth", oldPatient.getDateOfBirth(), newPatient.getDateOfBirth());
        compareField(changes, "age", oldPatient.getAge(), newPatient.getAge());
        compareField(changes, "gender", oldPatient.getGender(), newPatient.getGender());

        return changes;
    }

    public Map<String, Object> compareDoctorFields(DoctorDTO oldDoctor, DoctorDTO newDoctor) {
        Map<String, Object> changes = new HashMap<>();

        if (oldDoctor == null || newDoctor == null) {
            return changes;
        }

        compareField(changes, "name", oldDoctor.getName(), newDoctor.getName());
        compareField(changes, "email", oldDoctor.getEmail(), newDoctor.getEmail());
        compareField(changes, "speciality", oldDoctor.getSpeciality(), newDoctor.getSpeciality());
        compareField(changes, "qualification", oldDoctor.getQualification(), newDoctor.getQualification());
        compareField(changes, "hospitalAffiliation", oldDoctor.getHospitalAffiliation(), newDoctor.getHospitalAffiliation());
        compareField(changes, "licenseNumber", oldDoctor.getLicenseNumber(), newDoctor.getLicenseNumber());
        compareField(changes, "phone", oldDoctor.getPhone(), newDoctor.getPhone());
        compareField(changes, "address", oldDoctor.getAddress(), newDoctor.getAddress());
        compareField(changes, "city", oldDoctor.getCity(), newDoctor.getCity());
        compareField(changes, "state", oldDoctor.getState(), newDoctor.getState());
        compareField(changes, "country", oldDoctor.getCountry(), newDoctor.getCountry());

        return changes;
    }

    public Map<String, Object> compareTestFields(TestDTO oldTest, TestDTO newTest) {
        Map<String, Object> changes = new HashMap<>();

        if (oldTest == null || newTest == null) {
            return changes;
        }

        compareField(changes, "category", oldTest.getCategory(), newTest.getCategory());
        compareField(changes, "name", oldTest.getName(), newTest.getName());
        compareBigDecimalField(changes, "price", oldTest.getPrice(), newTest.getPrice());

        return changes;
    }

    public Map<String, Object> compareSampleFields(SampleDto oldSample, SampleDto newSample) {
        Map<String, Object> changes = new HashMap<>();
        if (oldSample == null || newSample == null) {
            return changes;
        }
        compareField(changes, "name", oldSample.getName(), newSample.getName());
        return changes;
    }

    public Map<String, Object> compareMaps(Map<String, Object> oldValues, Map<String, Object> newValues) {
        Map<String, Object> changes = new HashMap<>();
        if (oldValues == null || newValues == null) {
            return changes;
        }

        Set<String> keys = new java.util.HashSet<>();
        keys.addAll(oldValues.keySet());
        keys.addAll(newValues.keySet());

        for (String key : keys) {
            Object oldValue = oldValues.get(key);
            Object newValue = newValues.get(key);
            if (!valuesEqual(oldValue, newValue)) {
                changes.put(key, Map.of(
                        "old", safeString(oldValue),
                        "new", safeString(newValue)
                ));
            }
        }

        return changes;
    }

    /**
     * Compares two BillDTO instances to identify changed billing fields.
     */
    public Map<String, Object> compareBillFields(BillDTO oldBill, BillDTO newBill) {
        Map<String, Object> changes = new HashMap<>();

        if (oldBill == null || newBill == null) {
            return changes;
        }

        compareBigDecimalField(changes, "totalAmount", oldBill.getTotalAmount(), newBill.getTotalAmount());
        compareField(changes, "paymentStatus", oldBill.getPaymentStatus(), newBill.getPaymentStatus());
        compareField(changes, "paymentMethod", oldBill.getPaymentMethod(), newBill.getPaymentMethod());
        compareField(changes, "paymentDate", oldBill.getPaymentDate(), newBill.getPaymentDate());
        compareBigDecimalField(changes, "discount", oldBill.getDiscount(), newBill.getDiscount());
        compareField(changes, "discountReason", oldBill.getDiscountReason(), newBill.getDiscountReason());
        compareBigDecimalField(changes, "netAmount", oldBill.getNetAmount(), newBill.getNetAmount());
        compareBigDecimalField(changes, "receivedAmount", oldBill.getReceivedAmount(), newBill.getReceivedAmount());
        compareBigDecimalField(changes, "actualReceivedAmount", oldBill.getActualReceivedAmount(), newBill.getActualReceivedAmount());
        compareBigDecimalField(changes, "dueAmount", oldBill.getDueAmount(), newBill.getDueAmount());
        compareBigDecimalField(changes, "refundAmount", oldBill.getRefundAmount(), newBill.getRefundAmount());

        return changes;
    }

    /**
     * Helper method to compare a single field and add to changes map if different.
     */
    private void compareField(Map<String, Object> changes, String fieldName, Object oldValue, Object newValue) {
        if (!equals(oldValue, newValue)) {
            changes.put(fieldName, Map.of(
                "old", safeString(oldValue),
                "new", safeString(newValue)
            ));
        }
    }

    private void compareBigDecimalField(Map<String, Object> changes, String fieldName, BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null && newValue == null) {
            return;
        }
        boolean differs;
        if (oldValue == null || newValue == null) {
            differs = true;
        } else {
            differs = oldValue.compareTo(newValue) != 0;
        }
        if (differs) {
            changes.put(fieldName, Map.of(
                    "old", safeString(oldValue),
                    "new", safeString(newValue)
            ));
        }
    }

    /**
     * Converts field changes map to JSON string.
     */
    public String fieldChangesToJson(Map<String, Object> fieldChanges) {
        if (fieldChanges == null || fieldChanges.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fieldChanges);
        } catch (Exception e) {
            log.warn("Failed to serialize field changes to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts an object to JSON string.
     * Used for serializing old/new values.
     */
    public String objectToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Compares two objects for equality, handling null values.
     */
    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof BigDecimal oldDecimal && b instanceof BigDecimal newDecimal) {
            return oldDecimal.compareTo(newDecimal) == 0;
        }
        if (a instanceof Collection<?> oldCollection && b instanceof Collection<?> newCollection) {
            return new java.util.HashSet<>(oldCollection).equals(new java.util.HashSet<>(newCollection));
        }
        if (a.getClass().isArray() && b.getClass().isArray()) {
            int lengthA = Array.getLength(a);
            int lengthB = Array.getLength(b);
            if (lengthA != lengthB) {
                return false;
            }
            for (int i = 0; i < lengthA; i++) {
                Object elementA = Array.get(a, i);
                Object elementB = Array.get(b, i);
                if (!valuesEqual(elementA, elementB)) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }

    /**
     * Safely converts an object to string, handling null values.
     */
    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

