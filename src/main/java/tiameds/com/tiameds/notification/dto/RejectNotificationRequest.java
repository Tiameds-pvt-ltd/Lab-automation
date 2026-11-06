package tiameds.com.tiameds.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RejectNotificationRequest {
    /** Why the request is rejected; shown to the user who raised it. */
    private String reason;
}
