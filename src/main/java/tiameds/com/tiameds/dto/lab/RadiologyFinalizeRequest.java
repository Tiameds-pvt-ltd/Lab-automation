package tiameds.com.tiameds.dto.lab;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RadiologyFinalizeRequest {
    // Version the client last read; a mismatch means the report changed meanwhile (409).
    private Long version;
}
