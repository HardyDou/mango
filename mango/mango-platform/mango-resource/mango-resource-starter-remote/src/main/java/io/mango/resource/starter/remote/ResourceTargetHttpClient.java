package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.infra.web.util.InternalCallSignature;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 资源目标端动态地址 HTTP 客户端。
 */
public class ResourceTargetHttpClient implements ResourceTargetClient {

    private static final String TARGET_PATH = "/resource/targets";
    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";
    private static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";
    private static final String NONCE_HEADER = "X-Internal-Nonce";
    private static final String SIGNATURE_HEADER = "X-Internal-Signature";
    private static final String SECRET_VERSION_HEADER = "X-Internal-Secret-Version";
    private static final ParameterizedTypeReference<R<ResourceBatchResultVO>> BATCH_RESULT_TYPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<R<ResourceSyncResultVO>> RESULT_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;
    private final String sharedSecret;
    private final int secretVersion;
    private final LongSupplier timestampSupplier;
    private final Supplier<String> nonceSupplier;

    public ResourceTargetHttpClient(RestClient restClient, LoadBalancerClient loadBalancerClient,
                                    String sharedSecret, int secretVersion) {
        this(restClient, loadBalancerClient, sharedSecret, secretVersion,
                System::currentTimeMillis, () -> UUID.randomUUID().toString());
    }

    ResourceTargetHttpClient(RestClient restClient, LoadBalancerClient loadBalancerClient,
                             String sharedSecret, int secretVersion,
                             LongSupplier timestampSupplier, Supplier<String> nonceSupplier) {
        this.restClient = restClient;
        this.loadBalancerClient = loadBalancerClient;
        this.sharedSecret = sharedSecret;
        this.secretVersion = secretVersion;
        this.timestampSupplier = timestampSupplier;
        this.nonceSupplier = nonceSupplier;
    }

    @Override
    public R<ResourceBatchResultVO> upsertBatch(URI targetUri, ExecuteResourceTargetCommand command) {
        return post(targetUri, "/upsert-batch", command, BATCH_RESULT_TYPE);
    }

    @Override
    public R<ResourceSyncResultVO> disable(URI targetUri, ExecuteResourceTargetCommand command) {
        return post(targetUri, "/disable", command, RESULT_TYPE);
    }

    @Override
    public R<ResourceSyncResultVO> delete(URI targetUri, ExecuteResourceTargetCommand command) {
        return post(targetUri, "/delete", command, RESULT_TYPE);
    }

    private <T> R<T> post(URI targetUri, String action, ExecuteResourceTargetCommand command,
                          ParameterizedTypeReference<R<T>> responseType) {
        URI requestUri = resolveRequestUri(targetUri, TARGET_PATH + action);
        return restClient.post()
                .uri(requestUri)
                .headers(headers -> addInternalCallHeaders(headers, requestUri))
                .body(command)
                .retrieve()
                .body(responseType);
    }

    private URI resolveRequestUri(URI targetUri, String endpointPath) {
        URI instanceUri = resolveInstanceUri(targetUri);
        String contextPath = normalizePath(targetUri.getRawPath());
        String instancePath = instanceUri.equals(targetUri)
                ? ""
                : normalizePath(instanceUri.getRawPath());
        String path = joinPath(instancePath, contextPath, endpointPath);
        return URI.create(authority(instanceUri) + path);
    }

    private URI resolveInstanceUri(URI targetUri) {
        if (loadBalancerClient == null || targetUri.getHost() == null || targetUri.getPort() >= 0) {
            return targetUri;
        }
        ServiceInstance instance = loadBalancerClient.choose(targetUri.getHost());
        return instance == null ? targetUri : instance.getUri();
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

    private String authority(URI uri) {
        StringBuilder authority = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() >= 0) {
            authority.append(':').append(uri.getPort());
        }
        return authority.toString();
    }

    private String joinPath(String... parts) {
        StringBuilder path = new StringBuilder();
        for (String part : parts) {
            String normalized = normalizePath(part);
            if (!normalized.isEmpty()) {
                path.append(normalized);
            }
        }
        return path.isEmpty() ? "/" : path.toString();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
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
