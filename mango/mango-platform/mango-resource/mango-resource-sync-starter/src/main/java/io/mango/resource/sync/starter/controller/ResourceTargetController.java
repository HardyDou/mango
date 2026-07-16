package io.mango.resource.sync.starter.controller;

import io.mango.common.result.R;
import io.mango.resource.api.ResourceTargetApi;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import io.mango.resource.support.execution.ResourceTargetExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源目标模块内部执行入口。
 */
@Validated
@RestController
@RequestMapping("/resource/targets")
@RequiredArgsConstructor
@Tag(name = "资源目标端内部接口", description = "供资源注册中心在目标模块内执行资源同步、禁用和删除")
public class ResourceTargetController implements ResourceTargetApi {

    private final ResourceTargetExecutor resourceTargetExecutor;

    @Override
    @PostMapping("/upsert-batch")
    @Operation(summary = "批量创建或更新目标资源", description = "按资源类型调用本模块处理器执行批量资源同步")
    public R<ResourceBatchResultVO> upsertBatch(@RequestBody ExecuteResourceTargetCommand command) {
        return R.ok(resourceTargetExecutor.upsertBatch(command));
    }

    @Override
    @PostMapping("/disable")
    @Operation(summary = "禁用目标资源", description = "调用本模块资源处理器逻辑禁用指定资源")
    public R<ResourceSyncResultVO> disable(@RequestBody ExecuteResourceTargetCommand command) {
        return R.ok(resourceTargetExecutor.disable(command));
    }

    @Override
    @PostMapping("/delete")
    @Operation(summary = "删除目标资源", description = "调用本模块资源处理器删除指定资源")
    public R<ResourceSyncResultVO> delete(@RequestBody ExecuteResourceTargetCommand command) {
        return R.ok(resourceTargetExecutor.delete(command));
    }
}
