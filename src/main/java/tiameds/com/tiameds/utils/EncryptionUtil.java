package tiameds.com.tiameds.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.config.JwtProperties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    public static final String ENCRYPTED_PREFIX = "ENC(";
    private static final String ENCRYPTED_SUFFIX = ")";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public EncryptionUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void init() {
        String configuredKey = jwtProperties.getSubjectEncryptionKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("Subject encryption key must be configured");
        }
        byte[] keyBytes = Base64.getDecoder().decode(configuredKey);
        int length = keyBytes.length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException("Subject encryption key must be 16, 24, or 32 bytes after Base64 decoding");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encryptSubject(String subject) {
        if (subject == null) {
            return null;
        }
        return ENCRYPTED_PREFIX + encrypt(subject) + ENCRYPTED_SUFFIX;
    }

    public String decryptSubject(String subject) {
        if (subject == null) {
            return null;
        }
        if (!isEncrypted(subject)) {
            return subject;
        }
        String payload = subject.substring(ENCRYPTED_PREFIX.length(), subject.length() - ENCRYPTED_SUFFIX.length());
        return decrypt(payload);
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = ByteBuffer.allocate(iv.length + cipherBytes.length)
                    .put(iv)
                    .put(cipherBytes)
                    .array();
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt subject", ex);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            if (decoded.length < IV_LENGTH_BYTES) {
                throw new IllegalStateException("Invalid ciphertext for subject");
            }
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt subject", ex);
        }
    }

    private boolean isEncrypted(String value) {
        return value.startsWith(ENCRYPTED_PREFIX) && value.endsWith(ENCRYPTED_SUFFIX);
    }
}

