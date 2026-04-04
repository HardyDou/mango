package io.mango.message.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.common.util.JacksonUtils;
import io.mango.message.api.MessageApi;
import io.mango.message.api.po.SysMessagePo;
import io.mango.message.api.vo.SysMessageVO;
import io.mango.message.core.channel.MessageChannel;
import io.mango.message.core.channel.impl.SseChannel;
import io.mango.message.core.channel.impl.WebSocketChannel;
import io.mango.message.core.entity.SysMessage;
import io.mango.message.core.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageApi {

    private final SysMessageMapper messageMapper;
    private final WebSocketChannel wsChannel;
    private final SseChannel sseChannel;

    @Override
    public R<Long> send(SysMessagePo po) {
        SysMessage entity = new SysMessage();
        entity.setMessageType(po.getMessageType());
        entity.setTitle(po.getTitle());
        entity.setContent(po.getContent());
        entity.setPriority(po.getPriority());
        entity.setReadStatus(0);
        entity.setUserId(po.getUserId());
        messageMapper.insert(entity);

        String json = JacksonUtils.toJsonStr(entity);
        wsChannel.sendToUser(po.getUserId(), json);
        sseChannel.sendToUser(po.getUserId(), json);

        return R.ok(entity.getId());
    }

    @Override
    public R<Long> broadcast(SysMessagePo po) {
        SysMessage entity = new SysMessage();
        entity.setMessageType(po.getMessageType());
        entity.setTitle(po.getTitle());
        entity.setContent(po.getContent());
        entity.setPriority(po.getPriority());
        entity.setReadStatus(0);
        messageMapper.insert(entity);

        String json = JacksonUtils.toJsonStr(entity);
        wsChannel.broadcast(json);
        sseChannel.broadcast(json);

        return R.ok(entity.getId());
    }

    @Override
    public R<List<SysMessageVO>> listByUser(Long userId) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId)
               .orderByDesc(SysMessage::getCreateTime);
        List<SysMessage> list = messageMapper.selectList(wrapper);
        List<SysMessageVO> vos = list.stream().map(this::convertToVO).collect(Collectors.toList());
        return R.ok(vos);
    }

    @Override
    public R<Boolean> markRead(Long messageId) {
        SysMessage message = messageMapper.selectById(messageId);
        if (message != null) {
            message.setReadStatus(1);
            message.setReadTime(java.time.LocalDateTime.now());
            messageMapper.updateById(message);
        }
        return R.ok(true);
    }

    private SysMessageVO convertToVO(SysMessage entity) {
        SysMessageVO vo = new SysMessageVO();
        vo.setId(entity.getId());
        vo.setMessageType(entity.getMessageType());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setUserId(entity.getUserId());
        vo.setPriority(entity.getPriority());
        vo.setReadStatus(entity.getReadStatus());
        vo.setReadTime(entity.getReadTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
