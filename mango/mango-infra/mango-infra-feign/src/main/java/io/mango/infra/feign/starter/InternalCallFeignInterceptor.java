package io.mango.infra.feign.starter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.mango.infra.web.util.InternalCallSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

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
public class InternalCallFeignInterceptor implements RequestInterceptor, Ordered {

    public static final int ORDER = ModuleTargetFeignInterceptor.ORDER + 100;

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalCallFeignInterceptor.class);

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
        if (!StringUtils.hasText(sharedSecret)) {
            LOGGER.debug("No internal call secret configured, skipping internal call headers");
            return;
        }

        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();

        // Build query string from queries map
        String queryString = buildQueryString(template.queries());
        String method = template.method();
        String path = canonicalPath(template.url());

        // Build signature payload: timestamp:nonce:method:path:query
        String signature = InternalCallSignature.sign(
                String.valueOf(timestamp), nonce, method, path, queryString, sharedSecret);

        // Add all required headers
        template.header(INTERNAL_CALL_HEADER, "true");
        template.header(TIMESTAMP_HEADER, String.valueOf(timestamp));
        template.header(NONCE_HEADER, nonce);
        template.header(SECRET_VERSION_HEADER, String.valueOf(secretVersion));
        template.header(SIGNATURE_HEADER, signature);

        LOGGER.debug("Added internal call signature headers: method={}, path={}", method, path);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /**
     * Build query string from Feign's queries map.
     * Results are sorted alphabetically for consistent signature calculation.
     */
    private String buildQueryString(Map<String, Collection<String>> queries) {
        return InternalCallSignature.canonicalizeQueries(queries);
    }

    /**
     * Sign the request path only. Feign may expose an absolute URL after dynamic target rewriting,
     * while the server validates against HttpServletRequest#getRequestURI().
     */
    private String canonicalPath(String url) {
        if (url == null || url.isEmpty()) {
            return "/";
        }
        try {
            URI uri = new URI(url);
            String path = uri.getRawPath();
            if (path != null && !path.isEmpty()) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            // Fallback below keeps invalid but usable path strings deterministic.
        }
        int queryIndex = url.indexOf('?');
        String path = url;
        if (queryIndex >= 0) {
            path = url.substring(0, queryIndex);
        }
        if (path.isEmpty()) {
            return "/";
        }
        return path;
    }

}
