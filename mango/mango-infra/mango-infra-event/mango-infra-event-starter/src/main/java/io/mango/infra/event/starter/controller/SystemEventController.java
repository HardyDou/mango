package io.mango.infra.event.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.event.api.SystemEventApi;
import io.mango.infra.event.api.command.ReconsumeSystemEventCommand;
import io.mango.infra.event.api.query.SystemEventPageQuery;
import io.mango.infra.event.api.vo.SystemEventVO;
import io.mango.infra.event.core.system.SystemEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统事件运维接口。
 */
@RestController
@RequestMapping("/system/events")
@RequiredArgsConstructor
@Validated
@Tag(name = "系统事件", description = "查询异常领域事件并发起重新投递")
public class SystemEventController implements SystemEventApi {

    private final SystemEventService systemEventService;

    @Override
    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:event:list", desc = "分页查询系统事件")
    @Operation(summary = "分页查询系统事件", description = "分页查询 Outbox 中失败、重试中或处理中事件")
    public R<PageResult<SystemEventVO>> page(@Valid @ParameterObject SystemEventPageQuery query) {
        return R.ok(systemEventService.page(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:event:detail", desc = "查询系统事件详情")
    @Operation(summary = "查询系统事件详情", description = "按消息 ID 查询领域事件投递详情和错误信息")
    public R<SystemEventVO> detail(
            @Parameter(description = "消息 ID", required = true)
            @NotBlank(message = "消息 ID 不能为空")
            @RequestParam String messageId) {
        return R.ok(systemEventService.detail(messageId));
    }

    @Override
    @PostMapping("/reconsume")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:event:reconsume", desc = "重新投递系统事件")
    @Operation(summary = "重新投递系统事件", description = "将失败或等待重试的事件重新放回待投递队列")
    public R<Boolean> reconsume(@Valid @RequestBody ReconsumeSystemEventCommand command) {
        return R.ok(systemEventService.reconsume(command));
    }
}
