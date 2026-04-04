package io.mango.infra.feign.starter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feign interceptor for internal call authentication.
 *
 * <p>Adds HMAC-SHA256 signed headers for internal API verification:
 * <ul>
 *   <li>X-Internal-Call: fixed value "true"</li>
 *   <li>X-Internal-Timestamp: current timestamp in milliseconds</li>
 *   <li>X-Internal-Nonce: UUID for replay protection</li>
 *   <li>X-Internal-Secret-Version: secret version for key rotation</li>
 *   <li>X-Internal-Signature: HMAC-SHA256(timestamp:nonce:method:path:query, secret)</li>
 * </ul>
 *
 * @author Mango
 */
@Component
public class InternalCallFeignInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(InternalCallFeignInterceptor.class);

    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";
    private static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";
    private static final String NONCE_HEADER = "X-Internal-Nonce";
    private static final String SIGNATURE_HEADER = "X-Internal-Signature";
    private static final String SECRET_VERSION_HEADER = "X-Internal-Secret-Version";

    /**
     * Shared secret for HMAC signature (loaded from config center)
     */
    @Value("${mango.internal-call.secret:}")
    private String sharedSecret;

    /**
     * Secret version for key rotation
     */
    @Value("${mango.internal-call.secret-version:1}")
    private int secretVersion;

    @Override
    public void apply(RequestTemplate template) {
        // Skip if no secret configured (dev mode)
        if (sharedSecret == null || sharedSecret.isEmpty()) {
            log.debug("No internal call secret configured, skipping internal call headers");
            return;
        }

        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();

        // Build query string from queries map
        String queryString = buildQueryString(template.queries());
        String method = template.method();
        String path = template.url();

        // Build signature payload: timestamp:nonce:method:path:query
        String payload = timestamp + ":" + nonce + ":" + method + ":" + path + ":" + queryString;

        // Calculate HMAC-SHA256 signature
        String signature = hmacSha256(payload, sharedSecret);

        // Add all required headers
        template.header(INTERNAL_CALL_HEADER, "true");
        template.header(TIMESTAMP_HEADER, String.valueOf(timestamp));
        template.header(NONCE_HEADER, nonce);
        template.header(SECRET_VERSION_HEADER, String.valueOf(secretVersion));
        template.header(SIGNATURE_HEADER, signature);

        log.debug("Added internal call headers: timestamp={}, nonce={}, signature={}",
                timestamp, nonce, signature);
    }

    /**
     * Build query string from Feign's queries map.
     * Results are sorted alphabetically for consistent signature calculation.
     */
    private String buildQueryString(Map<String, Collection<String>> queries) {
        if (queries == null || queries.isEmpty()) {
            return "";
        }
        return queries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(e -> e.getValue().stream()
                        .map(v -> e.getKey() + "=" + v))
                .collect(Collectors.joining("&"));
    }

    /**
     * Calculate HMAC-SHA256 signature.
     */
    private String hmacSha256(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to calculate HMAC-SHA256", e);
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
