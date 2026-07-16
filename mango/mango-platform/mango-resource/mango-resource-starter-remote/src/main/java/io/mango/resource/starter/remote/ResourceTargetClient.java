package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;

import java.net.URI;

/**
 * 动态地址资源目标端客户端。
 */
public interface ResourceTargetClient {

    R<ResourceBatchResultVO> upsertBatch(URI targetUri, ExecuteResourceTargetCommand command);

    R<ResourceSyncResultVO> disable(URI targetUri, ExecuteResourceTargetCommand command);

    R<ResourceSyncResultVO> delete(URI targetUri, ExecuteResourceTargetCommand command);
}
