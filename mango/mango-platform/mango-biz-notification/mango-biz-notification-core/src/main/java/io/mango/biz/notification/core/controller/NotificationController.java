package io.mango.biz.notification.core.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.biz.notification.api.command.MarkNotificationReadBatchCommand;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.query.NotificationPageQuery;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import io.mango.biz.notification.core.service.INotificationService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Tag(name = "消息通知", description = "消息发送、广播、查询与已读管理接口")
public class NotificationController {

    private final INotificationService notificationService;
    private final ISecurityContextProvider securityContextProvider;

    @PostMapping("/send")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:send")
    @Operation(summary = "发送消息通知", description = "权限接口。向指定用户发送消息通知")
    public R<Map<String, Object>> send(@RequestBody @Valid SysNotificationPo po) {
        return R.ok(notificationService.sendForFrontend(po));
    }

    @PostMapping("/broadcast")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:broadcast")
    @Operation(summary = "广播消息通知", description = "权限接口。向用户广播消息通知")
    public R<Map<String, Object>> broadcast(@RequestBody @Valid SysNotificationPo po) {
        Long id = notificationService.broadcast(po).getData();
        return R.ok(Map.of(
                "messageId", id,
                "successCount", id == null ? 0 : 1,
                "failCount", id == null ? 1 : 0));
    }

    @GetMapping("/user")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    @Operation(summary = "获取用户消息通知", description = "权限接口。按用户ID查询消息通知列表")
    public R<List<SysNotificationVO>> listByUser(
            @Parameter(description = "用户ID")
            @RequestParam Long userId) {
        return notificationService.listByUser(userId);
    }

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    @Operation(summary = "分页查询我的消息通知", description = "权限接口。分页查询当前用户的消息通知")
    public R<PageResult<SysNotificationVO>> page(@ParameterObject NotificationPageQuery query) {
        return R.ok(notificationService.pageByUser(
                currentUserId(),
                query.getPageNum(),
                query.getPageSize(),
                query.getUnreadOnly()));
    }

    @GetMapping("/{id}")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    @Operation(summary = "获取消息通知详情", description = "权限接口。按消息ID查询消息通知详情")
    public R<SysNotificationVO> get(
            @Parameter(description = "消息ID")
            @PathVariable Long id) {
        SysNotificationVO message = notificationService.get(id);
        return message == null ? R.fail(404, "消息不存在") : R.ok(message);
    }

    @GetMapping("/unread/count")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    @Operation(summary = "获取未读消息数量", description = "权限接口。查询当前用户未读消息数量")
    public R<Map<String, Long>> unreadCount() {
        return R.ok(Map.of("count", notificationService.unreadCount(currentUserId())));
    }

    @PutMapping("/read")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:edit")
    @Operation(summary = "标记消息已读", description = "权限接口。按消息ID标记消息为已读")
    public R<Boolean> markRead(
            @Parameter(description = "消息ID")
            @RequestParam Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/{id}/read")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:edit")
    @Operation(summary = "标记路径消息已读", description = "权限接口。按路径中的消息ID标记消息为已读")
    public R<Boolean> markReadByPath(
            @Parameter(description = "消息ID")
            @PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/read/batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:edit")
    @Operation(summary = "批量标记消息已读", description = "权限接口。按消息ID集合批量标记消息为已读")
    public R<Boolean> markReadBatch(@RequestBody @Valid MarkNotificationReadBatchCommand command) {
        return R.ok(notificationService.markReadBatch(command.getIds()));
    }

    @PostMapping("/{id}/delete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:delete")
    @Operation(summary = "删除消息通知", description = "权限接口。按消息ID删除消息通知")
    public R<Boolean> delete(
            @Parameter(description = "消息ID")
            @PathVariable Long id) {
        return R.ok(notificationService.delete(id));
    }

    private Long currentUserId() {
        return securityContextProvider.currentContext().userId();
    }
}
