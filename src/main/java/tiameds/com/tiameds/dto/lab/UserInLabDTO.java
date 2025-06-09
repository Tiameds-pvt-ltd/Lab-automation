package tiameds.com.tiameds.dto.lab;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class UserInLabDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private String phone;
    private String city;
    private List<String> roles; // Add roles as a list of role names
}