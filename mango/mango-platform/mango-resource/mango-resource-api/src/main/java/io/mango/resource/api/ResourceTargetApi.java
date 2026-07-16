package io.mango.resource.api;

import io.mango.common.result.R;
import io.mango.infra.web.api.Inner;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/**
 * 资源目标模块内部执行 API。
 */
@Validated
public interface ResourceTargetApi {

    /**
     * 在目标模块本地批量执行资源创建或更新。
     *
     * @param command 目标执行命令。
     * @return 按资源 ID 返回的同步结果。
     */
    @Inner
    R<ResourceBatchResultVO> upsertBatch(@Valid ExecuteResourceTargetCommand command);

    /**
     * 在目标模块本地执行资源逻辑禁用。
     *
     * @param command 目标执行命令。
     * @return 同步结果。
     */
    @Inner
    R<ResourceSyncResultVO> disable(@Valid ExecuteResourceTargetCommand command);

    /**
     * 在目标模块本地执行资源删除。
     *
     * @param command 目标执行命令。
     * @return 同步结果。
     */
    @Inner
    R<ResourceSyncResultVO> delete(@Valid ExecuteResourceTargetCommand command);
}
