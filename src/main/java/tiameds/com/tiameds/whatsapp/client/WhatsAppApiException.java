package tiameds.com.tiameds.whatsapp.client;

/** A call to AWS (S3 / End User Messaging Social) failed. The message never contains patient data. */
public class WhatsAppApiException extends RuntimeException {

    private final int httpStatus;

    public WhatsAppApiException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public WhatsAppApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
