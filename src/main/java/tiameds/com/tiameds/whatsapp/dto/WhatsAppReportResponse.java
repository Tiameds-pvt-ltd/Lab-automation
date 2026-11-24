package tiameds.com.tiameds.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tiameds.com.tiameds.whatsapp.entity.WhatsAppReportLog;

import java.time.LocalDateTime;

/** What the API returns to the frontend. Deliberately omits the S3 key and the full phone number. */
@Getter
@AllArgsConstructor
public class WhatsAppReportResponse {

    private final Long id;
    private final Long visitId;
    private final String status;
    private final String phoneNumber;      // masked, e.g. +91******3210
    private final String whatsappMessageId;
    private final String errorMessage;
    private final String sentBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime sentAt;
    private final LocalDateTime deliveredAt;

    public static WhatsAppReportResponse from(WhatsAppReportLog l) {
        return new WhatsAppReportResponse(l.getId(), l.getVisitId(), l.getStatus().name(), mask(l.getPhoneNumber()),
                l.getWhatsappMessageId(), l.getErrorMessage(), l.getSentByUsername(),
                l.getCreatedAt(), l.getSentAt(), l.getDeliveredAt());
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "*".repeat(phone.length() - 7) + phone.substring(phone.length() - 4);
    }
}
