package tiameds.com.tiameds.whatsapp.entity;

public enum WhatsAppSendStatus {
    /** Row created, nothing sent yet. */
    PENDING,
    /** Accepted by WhatsApp (message id received). */
    SENT,
    /** Any failure before WhatsApp accepted the message. */
    FAILED,
    /** Reserved for delivery receipts (webhook). */
    DELIVERED,
    /** Reserved for read receipts (webhook). */
    READ
}
