package tiameds.com.tiameds.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What a held test / test reference change contains. Stored as JSON in notification.payload_json and applied only
 * when the change is approved.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogChangePayload {
    /** ADD, UPDATE, DELETE or DELETE_ALL */
    private String operation;
    /** id of the test / test reference being changed (null for ADD, DELETE_ALL or when only a code was sent) */
    private Long entityId;
    /** the request body: a TestDTO or a TestReferenceDTO (null for DELETE and DELETE_ALL) */
    private JsonNode body;
}
