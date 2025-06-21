package tiameds.com.tiameds.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;


@Data
public class RegisterRequest {

    @NotBlank(message = "Username is mandatory")
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is mandatory")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    private String lastName;

    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Phone number is invalid")
    private String phone;

    @NotBlank(message = "Address is mandatory")
    private String address;

    @NotBlank(message = "City is mandatory")
    private String city;

    @NotBlank(message = "State is mandatory")
    private String state;

    @NotBlank(message = "ZIP code is mandatory")
    @Pattern(regexp = "^[0-9]{5}(?:-[0-9]{4})?$", message = "ZIP code is invalid")
    private String zip;

    @NotBlank(message = "Country is mandatory")
    private String country;

    @NotNull(message = "Enabled cannot be null")
    @JsonProperty("verified")
    private boolean isVerified;

//    private List<Long> modules;


}
