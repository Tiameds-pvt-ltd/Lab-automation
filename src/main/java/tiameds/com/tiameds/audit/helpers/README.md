# Audit Helper Classes

This package contains reusable helper classes for audit functionality. These classes are designed to be extensible for future entity types beyond Patient.

## Architecture

### Current Design (Patient-Specific)
- **AuditAspect**: Main AOP aspect that handles patient-specific audit logging
- **Helper Classes**: Reusable utilities that can be extended for other entities

### Future Extension Pattern
When adding a new entity (e.g., Visit, Billing):
1. Add entity-specific methods to helper classes (or create new helpers)
2. Extend AuditAspect or create a new aspect for the entity
3. Reuse the helper classes for common operations

## Helper Classes

### AuditDataExtractor
**Purpose**: Extracts DTOs from method arguments and responses.

**Current Methods**:
- `extractPatientDTOFromArgs()` - Extracts PatientDTO from method arguments
- `extractPatientDTOFromResponse()` - Extracts PatientDTO from ResponseEntity/ApiResponse/Map responses
- `extractDTOFromResponse()` - Generic method for any DTO type (for future use)

**Usage Example**:
```java
PatientDTO patientDTO = auditDataExtractor.extractPatientDTOFromArgs(pjp.getArgs());
PatientDTO responseDTO = auditDataExtractor.extractPatientDTOFromResponse(result);
```

**Future Extension**:
```java
VisitDTO visitDTO = auditDataExtractor.extractDTOFromResponse(result, VisitDTO.class);
```

### FieldChangeTracker
**Purpose**: Compares entities/DTOs and tracks field changes.

**Current Methods**:
- `comparePatientFields()` - Compares PatientEntity and PatientDTO
- `fieldChangesToJson()` - Converts field changes map to JSON
- `objectToJson()` - Serializes any object to JSON

**Usage Example**:
```java
Map<String, Object> changes = fieldChangeTracker.comparePatientFields(oldEntity, newDTO);
String json = fieldChangeTracker.fieldChangesToJson(changes);
```

**Future Extension**:
```java
Map<String, Object> changes = fieldChangeTracker.compareVisitFields(oldVisit, newVisitDTO);
```

## Benefits of This Design

1. **Separation of Concerns**: Business logic separated from audit logic
2. **Reusability**: Helper classes can be used for any entity type
3. **Testability**: Helper classes can be unit tested independently
4. **Maintainability**: Changes to audit logic are centralized
5. **Extensibility**: Easy to add support for new entities

## Adding Support for a New Entity

### Step 1: Add Helper Methods
In `FieldChangeTracker.java`:
```java
public Map<String, Object> compareVisitFields(VisitEntity oldVisit, VisitDTO newVisit) {
    // Similar to comparePatientFields
}
```

### Step 2: Add Extraction Methods (if needed)
In `AuditDataExtractor.java`:
```java
public VisitDTO extractVisitDTOFromArgs(Object[] args) {
    // Similar to extractPatientDTOFromArgs
}
```

### Step 3: Use in Aspect
In `AuditAspect.java` or new `VisitAuditAspect.java`:
```java
VisitDTO visitDTO = auditDataExtractor.extractVisitDTOFromArgs(pjp.getArgs());
Map<String, Object> changes = fieldChangeTracker.compareVisitFields(oldVisit, newVisitDTO);
```

## Notes

- Helper classes are Spring `@Component` beans, so they're automatically injected
- All JSON serialization uses `ObjectMapper` (auto-configured by Spring Boot)
- Error handling is done with try-catch and logging in the aspect layer


