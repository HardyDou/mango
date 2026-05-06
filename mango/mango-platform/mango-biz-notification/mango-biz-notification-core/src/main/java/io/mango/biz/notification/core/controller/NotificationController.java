package io.mango.biz.notification.core.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import io.mango.biz.notification.core.service.INotificationService;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @PostMapping("/send")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:send")
    public R<Long> send(@RequestBody @Valid SysNotificationPo po) {
        return notificationService.send(po);
    }

    @PostMapping("/broadcast")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:broadcast")
    public R<Long> broadcast(@RequestBody @Valid SysNotificationPo po) {
        return notificationService.broadcast(po);
    }

    @GetMapping("/user")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:query")
    public R<List<SysNotificationVO>> listByUser(@RequestParam Long userId) {
        return notificationService.listByUser(userId);
    }

    @PutMapping("/read")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "message:edit")
    public R<Boolean> markRead(@RequestParam Long id) {
        return notificationService.markRead(id);
    }
}
