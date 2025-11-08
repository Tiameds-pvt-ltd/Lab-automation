package tiameds.com.tiameds.audit.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.utils.ApiResponse;

import java.util.Map;

/**
 * Helper class for extracting DTOs from method arguments and responses.
 * Designed to be extensible for other entity types in the future.
 */
@Slf4j
@Component
public class AuditDataExtractor {

    /**
     * Extracts PatientDTO from method arguments.
     * Can be extended to support other DTO types.
     */
    public PatientDTO extractPatientDTOFromArgs(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof PatientDTO) {
                return (PatientDTO) arg;
            }
        }
        return null;
    }

    /**
     * Extracts PatientDTO from method result/response.
     * Handles various response wrapper types (ResponseEntity, ApiResponse, Map).
     * Can be extended to support other DTO types.
     */
    public PatientDTO extractPatientDTOFromResponse(Object result) {
        if (result == null) return null;
        
        // Handle ResponseEntity<?> wrapper
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            Object body = responseEntity.getBody();
            
            if (body == null) return null;
            
            // Handle Map response from ApiResponseHelper.successResponseWithDataAndMessage
            if (body instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) body;
                Object data = responseMap.get("data");
                if (data instanceof PatientDTO) {
                    return (PatientDTO) data;
                }
            }
            // Handle ApiResponse<T> wrapper
            else if (body instanceof ApiResponse) {
                @SuppressWarnings("unchecked")
                ApiResponse<Object> apiResponse = (ApiResponse<Object>) body;
                Object data = apiResponse.getData();
                if (data instanceof PatientDTO) {
                    return (PatientDTO) data;
                }
            }
            // Direct PatientDTO
            else if (body instanceof PatientDTO) {
                return (PatientDTO) body;
            }
        } else if (result instanceof PatientDTO) {
            return (PatientDTO) result;
        }
        return null;
    }

    /**
     * Generic method to extract any DTO from response.
     * Can be extended for other entity types.
     * 
     * @param result The method result
     * @param dtoClass The DTO class to extract
     * @return The extracted DTO or null
     */
    @SuppressWarnings("unchecked")
    public <T> T extractDTOFromResponse(Object result, Class<T> dtoClass) {
        if (result == null || dtoClass == null) return null;
        
        // Handle ResponseEntity<?> wrapper
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            Object body = responseEntity.getBody();
            
            if (body == null) return null;
            
            // Handle Map response
            if (body instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) body;
                Object data = responseMap.get("data");
                if (dtoClass.isInstance(data)) {
                    return (T) data;
                }
            }
            // Handle ApiResponse<T> wrapper
            else if (body instanceof ApiResponse) {
                ApiResponse<Object> apiResponse = (ApiResponse<Object>) body;
                Object data = apiResponse.getData();
                if (dtoClass.isInstance(data)) {
                    return (T) data;
                }
            }
            // Direct DTO
            else if (dtoClass.isInstance(body)) {
                return (T) body;
            }
        } else if (dtoClass.isInstance(result)) {
            return (T) result;
        }
        return null;
    }
}


