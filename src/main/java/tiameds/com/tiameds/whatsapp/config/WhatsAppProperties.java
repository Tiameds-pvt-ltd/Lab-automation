package tiameds.com.tiameds.whatsapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Settings for sending lab reports over WhatsApp through AWS End User Messaging Social.
 * Everything comes from environment variables (nothing sensitive in code); the module is OFF unless
 * WHATSAPP_ENABLED=true. Reuses the existing AWS_REGION / AWS_S3_BUCKET / AWS_ACCESS_KEY / AWS_SECRET_KEY
 * values unless a WHATSAPP_* override is given.
 */
@Component
public class WhatsAppProperties {

    private final boolean enabled;
    private final String phoneNumberId;
    private final String region;
    private final String s3Bucket;
    private final String s3Prefix;
    private final String metaApiVersion;
    private final String templateName;
    private final String templateLanguage;
    private final String defaultCountryCode;
    private final String accessKey;
    private final String secretKey;
    private final long maxPdfBytes;

    public WhatsAppProperties(
            @Value("${whatsapp.enabled:${WHATSAPP_ENABLED:false}}") boolean enabled,
            @Value("${whatsapp.phone-number-id:${WHATSAPP_PHONE_NUMBER_ID:}}") String phoneNumberId,
            // must be the same Region as the WABA in AWS End User Messaging Social
            @Value("${whatsapp.region:${WHATSAPP_AWS_REGION:${AWS_REGION:}}}") String region,
            // must be in the same AWS account AND Region as the WABA
            @Value("${whatsapp.s3-bucket:${WHATSAPP_S3_BUCKET:${AWS_S3_BUCKET:}}}") String s3Bucket,
            @Value("${whatsapp.s3-prefix:${WHATSAPP_S3_PREFIX:whatsapp-reports/}}") String s3Prefix,
            // check the supported versions in the AWS General Reference (End User Messaging Social)
            @Value("${whatsapp.meta-api-version:${WHATSAPP_META_API_VERSION:v20.0}}") String metaApiVersion,
            @Value("${whatsapp.template-name:${WHATSAPP_TEMPLATE_NAME:lab_report_ready}}") String templateName,
            // must match the template language in Meta exactly, e.g. en or en_US
            @Value("${whatsapp.template-language:${WHATSAPP_TEMPLATE_LANGUAGE:en}}") String templateLanguage,
            @Value("${whatsapp.default-country-code:${WHATSAPP_DEFAULT_COUNTRY_CODE:91}}") String defaultCountryCode,
            @Value("${whatsapp.access-key:${AWS_ACCESS_KEY:}}") String accessKey,
            @Value("${whatsapp.secret-key:${AWS_SECRET_KEY:}}") String secretKey,
            @Value("${whatsapp.max-pdf-bytes:${WHATSAPP_MAX_PDF_BYTES:10485760}}") long maxPdfBytes
    ) {
        this.enabled = enabled;
        this.phoneNumberId = phoneNumberId == null ? "" : phoneNumberId.trim();
        this.region = region == null ? "" : region.trim();
        this.s3Bucket = s3Bucket == null ? "" : s3Bucket.trim();
        String prefix = s3Prefix == null ? "" : s3Prefix.trim();
        this.s3Prefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
        this.metaApiVersion = metaApiVersion.trim();
        this.templateName = templateName.trim();
        this.templateLanguage = templateLanguage.trim();
        this.defaultCountryCode = defaultCountryCode.trim();
        this.accessKey = accessKey == null ? "" : accessKey.trim();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.maxPdfBytes = maxPdfBytes;
    }

    /** True only when the feature is switched on AND every required setting is present. */
    public boolean isUsable() {
        return enabled && !phoneNumberId.isEmpty() && !region.isEmpty() && !s3Bucket.isEmpty();
    }

    public boolean isEnabled() { return enabled; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public String getRegion() { return region; }
    public String getS3Bucket() { return s3Bucket; }
    public String getS3Prefix() { return s3Prefix; }
    public String getMetaApiVersion() { return metaApiVersion; }
    public String getTemplateName() { return templateName; }
    public String getTemplateLanguage() { return templateLanguage; }
    public String getDefaultCountryCode() { return defaultCountryCode; }
    public String getAccessKey() { return accessKey; }
    public String getSecretKey() { return secretKey; }
    public long getMaxPdfBytes() { return maxPdfBytes; }
}
