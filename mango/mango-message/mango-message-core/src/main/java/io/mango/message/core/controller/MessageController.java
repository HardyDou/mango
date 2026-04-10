package io.mango.message.core.controller;

import io.mango.infra.security.api.Perm;
import io.mango.common.result.R;
import io.mango.message.api.MessageApi;
import io.mango.message.api.po.SysMessagePo;
import io.mango.message.api.vo.SysMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageApi messageApi;

    @PostMapping("/send")
    @Perm("message:send")
    public R<Long> send(@RequestBody @Valid SysMessagePo po) {
        return messageApi.send(po);
    }

    @PostMapping("/broadcast")
    @Perm("message:broadcast")
    public R<Long> broadcast(@RequestBody @Valid SysMessagePo po) {
        return messageApi.broadcast(po);
    }

    @GetMapping("/user/{userId}")
    @Perm("message:query")
    public R<List<SysMessageVO>> listByUser(@PathVariable Long userId) {
        return messageApi.listByUser(userId);
    }

    @PutMapping("/read/{id}")
    @Perm("message:edit")
    public R<Boolean> markRead(@PathVariable Long id) {
        return messageApi.markRead(id);
    }
}
