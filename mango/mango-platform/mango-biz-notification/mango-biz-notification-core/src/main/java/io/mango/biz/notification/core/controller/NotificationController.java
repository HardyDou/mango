package io.mango.biz.notification.core.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import io.mango.biz.notification.core.service.INotificationService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public R<Map<String, Object>> send(@RequestBody @Valid SysNotificationPo po) {
        return R.ok(notificationService.sendForFrontend(po));
    }

    @PostMapping("/broadcast")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:broadcast")
    public R<Map<String, Object>> broadcast(@RequestBody @Valid SysNotificationPo po) {
        Long id = notificationService.broadcast(po).getData();
        return R.ok(Map.of(
                "messageId", id,
                "successCount", id == null ? 0 : 1,
                "failCount", id == null ? 1 : 0));
    }

    @GetMapping("/user")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    public R<List<SysNotificationVO>> listByUser(@RequestParam Long userId) {
        return notificationService.listByUser(userId);
    }

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    public R<PageResult<SysNotificationVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Boolean unreadOnly) {
        return R.ok(notificationService.pageByUser(currentUserId(), pageNum, pageSize, unreadOnly));
    }

    @GetMapping("/{id}")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    public R<SysNotificationVO> get(@PathVariable Long id) {
        SysNotificationVO message = notificationService.get(id);
        return message == null ? R.fail(404, "消息不存在") : R.ok(message);
    }

    @GetMapping("/unread/count")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    public R<Map<String, Long>> unreadCount() {
        return R.ok(Map.of("count", notificationService.unreadCount(currentUserId())));
    }

    @PutMapping("/read")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:edit")
    public R<Boolean> markRead(@RequestParam Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/{id}/read")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:edit")
    public R<Boolean> markReadByPath(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/read/batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:edit")
    public R<Boolean> markReadBatch(@RequestBody Map<String, List<Long>> payload) {
        return R.ok(notificationService.markReadBatch(payload.get("ids")));
    }

    @PostMapping("/{id}/delete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:delete")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(notificationService.delete(id));
    }

    private Long currentUserId() {
        return securityContextProvider.currentContext().userId();
    }
}
