package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.infra.context.api.MangoContextHeaders;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.web.util.InternalCallSignature;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * JobCenter 与 Worker 动态地址内部调用客户端。
 */
public class MangoJobDynamicHttpClient {

    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";
    private static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";
    private static final String NONCE_HEADER = "X-Internal-Nonce";
    private static final String SIGNATURE_HEADER = "X-Internal-Signature";
    private static final String SECRET_VERSION_HEADER = "X-Internal-Secret-Version";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final ParameterizedTypeReference<R<Long>> LONG_RESULT_TYPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<R<MangoJobWorkerExecuteResultVO>> EXECUTE_RESULT_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final String sharedSecret;
    private final int secretVersion;
    private final LongSupplier timestampSupplier;
    private final Supplier<String> nonceSupplier;

    public MangoJobDynamicHttpClient(RestClient restClient, String sharedSecret, int secretVersion) {
        this(restClient, sharedSecret, secretVersion, System::currentTimeMillis,
                () -> UUID.randomUUID().toString());
    }

    MangoJobDynamicHttpClient(RestClient restClient, String sharedSecret, int secretVersion,
                              LongSupplier timestampSupplier, Supplier<String> nonceSupplier) {
        this.restClient = restClient;
        this.sharedSecret = sharedSecret;
        this.secretVersion = secretVersion;
        this.timestampSupplier = timestampSupplier;
        this.nonceSupplier = nonceSupplier;
    }

    public R<Long> registerWorker(URI jobCenterUri, RegisterMangoJobWorkerCommand command) {
        return post(jobCenterUri, "/job/internal/workers/register", command, LONG_RESULT_TYPE);
    }

    public R<MangoJobWorkerExecuteResultVO> executeWorker(URI workerUri,
                                                           MangoJobWorkerExecuteCommand command) {
        return post(workerUri, "/_job/workers/execute", command, EXECUTE_RESULT_TYPE);
    }

    private <T> R<T> post(URI baseUri, String endpointPath, Object body,
                          ParameterizedTypeReference<R<T>> responseType) {
        URI requestUri = resolveRequestUri(baseUri, endpointPath);
        return restClient.post()
                .uri(requestUri)
                .headers(headers -> addHeaders(headers, requestUri))
                .body(body)
                .retrieve()
                .body(responseType);
    }

    private URI resolveRequestUri(URI baseUri, String endpointPath) {
        String basePath = normalizePath(baseUri.getRawPath());
        String endpoint = normalizePath(endpointPath);
        String path;
        if (basePath.endsWith("/job") && endpoint.startsWith("/_job/")) {
            path = basePath.substring(0, basePath.length() - 4) + endpoint;
        } else if (basePath.endsWith("/job") && endpoint.startsWith("/job/")) {
            path = basePath + endpoint.substring(4);
        } else {
            path = basePath + endpoint;
        }
        StringBuilder authority = new StringBuilder(baseUri.getScheme())
                .append("://")
                .append(baseUri.getHost());
        if (baseUri.getPort() >= 0) {
            authority.append(':').append(baseUri.getPort());
        }
        return URI.create(authority + (path.isEmpty() ? "/" : path));
    }

    private void addHeaders(HttpHeaders headers, URI requestUri) {
        String token = MangoContextHolder.token();
        if (StringUtils.hasText(token)) {
            headers.set(AUTHORIZATION_HEADER, token);
        }
        MangoContextSnapshot context = MangoContextHolder.get();
        put(headers, MangoContextHeaders.REQUEST_ID, context.requestId());
        put(headers, MangoContextHeaders.TRACE_ID, context.traceId());
        put(headers, MangoContextHeaders.TENANT_ID, context.tenantId());
        put(headers, MangoContextHeaders.USER_ID, context.userId());
        put(headers, MangoContextHeaders.MEMBER_ID, context.memberId());
        put(headers, MangoContextHeaders.PRINCIPAL_NAME, context.principalName());
        put(headers, MangoContextHeaders.REALM, context.realm());
        put(headers, MangoContextHeaders.ACTOR_TYPE, context.actorType());
        put(headers, MangoContextHeaders.PARTY_TYPE, context.partyType());
        put(headers, MangoContextHeaders.PARTY_ID, context.partyId());
        put(headers, MangoContextHeaders.APP_CODE, context.appCode());
        put(headers, MangoContextHeaders.CLIENT_IP, context.clientIp());
        addInternalCallHeaders(headers, requestUri);
    }

    private void addInternalCallHeaders(HttpHeaders headers, URI requestUri) {
        if (!StringUtils.hasText(sharedSecret)) {
            return;
        }
        String timestamp = Long.toString(timestampSupplier.getAsLong());
        String nonce = nonceSupplier.get();
        String signature = InternalCallSignature.sign(
                timestamp, nonce, "POST", requestUri.getRawPath(), "", sharedSecret);
        headers.set(INTERNAL_CALL_HEADER, "true");
        headers.set(TIMESTAMP_HEADER, timestamp);
        headers.set(NONCE_HEADER, nonce);
        headers.set(SECRET_VERSION_HEADER, Integer.toString(secretVersion));
        headers.set(SIGNATURE_HEADER, signature);
    }

    private void put(HttpHeaders headers, String name, Object value) {
        if (value != null && StringUtils.hasText(value.toString())) {
            headers.set(name, value.toString());
        }
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path.trim())) {
            return "";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
