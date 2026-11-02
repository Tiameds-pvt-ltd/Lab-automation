package tiameds.com.tiameds.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Self-service edit of the logged-in user's personal details.
 * A field that is missing / null is left unchanged; a field that is sent cannot be blank.
 *
 * Username, email, password, roles and enabled are not editable here. Unknown fields are rejected (400) instead of
 * being silently ignored, so a client that sends "email" is told it was not applied.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = false)
public class UserProfileUpdateDTO {

    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Phone number is invalid")
    private String phone;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    @Size(max = 20, message = "Zip must be at most 20 characters")
    private String zip;

    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;
}
