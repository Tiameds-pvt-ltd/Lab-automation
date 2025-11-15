package tiameds.com.tiameds.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Location of the RSA private key (PEM encoded) used to sign JWTs.
     * Example: file:/etc/tiameds/jwt/private.pem
     */
    @NotNull
    private Resource privateKeyLocation;

    /**
     * Location of the RSA public key (PEM encoded) used to verify JWTs.
     * Example: file:/etc/tiameds/jwt/public.pem
     */
    @NotNull
    private Resource publicKeyLocation;

    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofHours(24);

    @NotBlank
    private String issuer = "tiameds-lab-automation";

    @NotBlank
    private String audience = "tiameds-clients";

    private String accessCookieName = "accessToken";
    private String refreshCookieName = "refreshToken";
    private String cookieDomain;
    private boolean cookieSecure = true;
    private String sameSite = "Strict";
    private String cookiePath = "/";
}


















