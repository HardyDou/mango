package io.mango.resource.support.execution;

import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;

/**
 * 当前应用内资源目标执行端口。
 *
 * <p>该端口只负责把资源目标命令分派给当前 JVM 中注册的资源处理器，不访问数据库、
 * 不暴露 HTTP，也不依赖部署拓扑。</p>
 */
public interface ResourceTargetExecutor {

    ResourceBatchResultVO upsertBatch(ExecuteResourceTargetCommand command);

    ResourceSyncResultVO disable(ExecuteResourceTargetCommand command);

    ResourceSyncResultVO delete(ExecuteResourceTargetCommand command);
}
