package tiameds.com.tiameds.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApproveNotificationRequest {
    /** Why the request is approved; shown to the user who raised it. ("remark" is accepted as an alias.) */
    @JsonAlias("remark")
    private String reason;
}
