package tiameds.com.tiameds.whatsapp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tiameds.com.tiameds.whatsapp.config.WhatsAppProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

/**
 * Thin client for AWS End User Messaging Social (WhatsApp) + the S3 upload of the report PDF.
 *
 * The project's AWS SDK is 2.20.0, which predates the "socialmessaging" module, so instead of bumping the SDK
 * for the whole application this calls the two REST operations directly, signed with SigV4 (signing name
 * "social-messaging") using the signer that already ships with the SDK:
 *   POST /v1/whatsapp/media  (PostWhatsAppMessageMedia)  -> mediaId
 *   POST /v1/whatsapp/send   (SendWhatsAppMessage)       -> messageId
 */
@Component
public class AwsWhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(AwsWhatsAppClient.class);
    private static final String SIGNING_NAME = "social-messaging";
    // headers java.net.http sets itself and refuses to take from the caller
    private static final Set<String> RESTRICTED_HEADERS = Set.of("host", "content-length", "connection", "expect", "upgrade");

    private final WhatsAppProperties props;
    private final ObjectMapper objectMapper;
    private final AwsCredentialsProvider credentialsProvider;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private volatile S3Client s3Client;

    public AwsWhatsAppClient(WhatsAppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        // Same rule as S3StorageService: static keys only if both are set, otherwise the default chain
        // (ECS task role on Fargate).
        this.credentialsProvider = (!props.getAccessKey().isEmpty() && !props.getSecretKey().isEmpty())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()))
                : DefaultCredentialsProvider.create();
    }

    /** Uploads the report PDF to the (private) WhatsApp bucket. Returns nothing; the caller keeps the key. */
    public void uploadPdf(String key, byte[] pdf) {
        try {
            s3().putObject(PutObjectRequest.builder()
                            .bucket(props.getS3Bucket())
                            .key(key)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(pdf));
        } catch (RuntimeException e) {
            throw new WhatsAppApiException("S3 upload failed: " + e.getClass().getSimpleName(), e);
        }
    }

    /** Hands the S3 object to WhatsApp and returns the media id to use in the template header. */
    public String postMediaFromS3(String key) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("originationPhoneNumberId", props.getPhoneNumberId());
        ObjectNode s3File = body.putObject("sourceS3File");
        s3File.put("bucketName", props.getS3Bucket());
        s3File.put("key", key);

        JsonNode response = signedPost("/v1/whatsapp/media", body.toString());
        String mediaId = response.path("mediaId").asText("");
        if (mediaId.isEmpty()) {
            throw new WhatsAppApiException("WhatsApp media upload returned no mediaId", 502);
        }
        return mediaId;
    }

    /**
     * Sends the approved document-header template. Template layout (must match the template approved in Meta):
     * header = document, body = {{1}} patient name, {{2}} visit/report reference, {{3}} lab name.
     *
     * @param toE164 recipient in E.164 format, e.g. +919876543210
     * @return WhatsApp message id
     */
    public String sendReportTemplate(String toE164, String patientName, String reportRef, String labName,
                                     String mediaId, String fileName) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("messaging_product", "whatsapp");
        message.put("recipient_type", "individual");
        message.put("to", toE164);
        message.put("type", "template");

        ObjectNode template = message.putObject("template");
        template.put("name", props.getTemplateName());
        template.putObject("language").put("code", props.getTemplateLanguage());
        ArrayNode components = template.putArray("components");

        ObjectNode header = components.addObject();
        header.put("type", "header");
        ObjectNode document = header.putArray("parameters").addObject();
        document.put("type", "document");
        ObjectNode doc = document.putObject("document");
        doc.put("id", mediaId);
        doc.put("filename", fileName);

        ObjectNode bodyComponent = components.addObject();
        bodyComponent.put("type", "body");
        ArrayNode bodyParams = bodyComponent.putArray("parameters");
        for (String value : new String[]{patientName, reportRef, labName}) {
            bodyParams.addObject().put("type", "text").put("text", templateParam(value));
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.put("message", Base64.getEncoder().encodeToString(message.toString().getBytes(StandardCharsets.UTF_8)));
        request.put("metaApiVersion", props.getMetaApiVersion());
        request.put("originationPhoneNumberId", props.getPhoneNumberId());

        JsonNode response = signedPost("/v1/whatsapp/send", request.toString());
        String messageId = response.path("messageId").asText("");
        if (messageId.isEmpty()) {
            throw new WhatsAppApiException("WhatsApp send returned no messageId", 502);
        }
        return messageId;
    }

    // WhatsApp rejects template parameters with newlines, tabs or 4+ consecutive spaces.
    private static String templateParam(String value) {
        String v = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" {2,}", " ").trim();
        if (v.isEmpty()) {
            v = "-";
        }
        return v.length() > 100 ? v.substring(0, 100) : v;
    }

    private JsonNode signedPost(String path, String jsonBody) {
        String region = props.getRegion();
        URI uri = URI.create("https://social-messaging." + region + ".amazonaws.com" + path);
        byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
        try {
            SdkHttpFullRequest unsigned = SdkHttpFullRequest.builder()
                    .method(SdkHttpMethod.POST)
                    .uri(uri)
                    .putHeader("Content-Type", "application/json")
                    .contentStreamProvider(() -> new ByteArrayInputStream(payload))
                    .build();
            Aws4SignerParams params = Aws4SignerParams.builder()
                    .awsCredentials(credentialsProvider.resolveCredentials())
                    .signingName(SIGNING_NAME)
                    .signingRegion(Region.of(region))
                    .build();
            SdkHttpFullRequest signed = Aws4Signer.create().sign(unsigned, params);

            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
            signed.headers().forEach((name, values) -> {
                if (!RESTRICTED_HEADERS.contains(name.toLowerCase())) {
                    values.forEach(v -> request.header(name, v));
                }
            });

            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status / 100 != 2) {
                // AWS error bodies describe the problem; they do not echo the message payload.
                log.warn("WhatsApp API {} failed: HTTP {} {}", path, status, abbreviate(response.body()));
                throw new WhatsAppApiException("WhatsApp API " + path + " failed with HTTP " + status
                        + ": " + abbreviate(response.body()), status);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WhatsAppApiException("WhatsApp API call interrupted", e);
        } catch (IOException | RuntimeException e) {
            if (e instanceof WhatsAppApiException wae) {
                throw wae;
            }
            throw new WhatsAppApiException("WhatsApp API call failed: " + e.getClass().getSimpleName(), e);
        }
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private S3Client s3() {
        S3Client client = s3Client;
        if (client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    s3Client = S3Client.builder()
                            .region(Region.of(props.getRegion()))
                            .credentialsProvider(credentialsProvider)
                            .build();
                }
                client = s3Client;
            }
        }
        return client;
    }

    @PreDestroy
    void close() {
        S3Client client = s3Client;
        if (client != null) {
            client.close();
        }
    }
}
