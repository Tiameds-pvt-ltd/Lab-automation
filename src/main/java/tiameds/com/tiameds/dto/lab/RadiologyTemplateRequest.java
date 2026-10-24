package tiameds.com.tiameds.dto.lab;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RadiologyTemplateRequest {
    private Long testId;
    private String name;
    // Form definition: sections, fields, options and the default "normal" text.
    private JsonNode schemaJson;
    // true = template for every lab (SUPERADMIN only); false/null = this lab only.
    private Boolean global;
}
