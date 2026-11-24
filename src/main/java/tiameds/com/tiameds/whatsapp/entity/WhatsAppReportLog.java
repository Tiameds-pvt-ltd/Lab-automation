package tiameds.com.tiameds.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One WhatsApp send attempt of a visit's lab report. Table is created by Flyway V70.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lab_report_whatsapp_log")
public class WhatsAppReportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "s3_key", length = 512)
    private String s3Key;

    @Column(name = "media_id", length = 100)
    private String mediaId;

    @Column(name = "whatsapp_message_id", length = 255)
    private String whatsappMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WhatsAppSendStatus status = WhatsAppSendStatus.PENDING;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "sent_by_user_id")
    private Long sentByUserId;

    @Column(name = "sent_by_username", length = 100)
    private String sentByUsername;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
