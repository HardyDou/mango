package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * 资源目标端动态地址 HTTP 客户端。
 */
@RequiredArgsConstructor
public class ResourceTargetHttpClient implements ResourceTargetClient {

    private static final String TARGET_PATH = "/resource/targets";
    private static final ParameterizedTypeReference<R<ResourceBatchResultVO>> BATCH_RESULT_TYPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<R<ResourceSyncResultVO>> RESULT_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

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
        return restClient.post()
                .uri(targetUri.resolve(TARGET_PATH + action))
                .body(command)
                .retrieve()
                .body(responseType);
    }
}
