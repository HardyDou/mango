package io.mango.biz.notification.core.controller;

import io.mango.infra.security.api.Perm;
import io.mango.common.result.R;
import io.mango.biz.notification.api.NotificationApi;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApi messageApi;

    @PostMapping("/send")
    @Perm("message:send")
    public R<Long> send(@RequestBody @Valid SysNotificationPo po) {
        return messageApi.send(po);
    }

    @PostMapping("/broadcast")
    @Perm("message:broadcast")
    public R<Long> broadcast(@RequestBody @Valid SysNotificationPo po) {
        return messageApi.broadcast(po);
    }

    @GetMapping("/user/{userId}")
    @Perm("message:query")
    public R<List<SysNotificationVO>> listByUser(@PathVariable Long userId) {
        return messageApi.listByUser(userId);
    }

    @PutMapping("/read/{id}")
    @Perm("message:edit")
    public R<Boolean> markRead(@PathVariable Long id) {
        return messageApi.markRead(id);
    }
}
