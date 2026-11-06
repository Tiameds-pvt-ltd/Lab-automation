package tiameds.com.tiameds.notification.service;

import org.springframework.http.HttpStatus;

/** Business-rule failure of the notification module, carrying the HTTP status to answer with. */
public class NotificationException extends RuntimeException {

    private final HttpStatus status;

    public NotificationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
