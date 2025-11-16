package tiameds.com.tiameds.services.auth;

import org.springframework.http.HttpStatus;

public class RefreshTokenException extends RuntimeException {

    private final HttpStatus status;

    public RefreshTokenException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

























