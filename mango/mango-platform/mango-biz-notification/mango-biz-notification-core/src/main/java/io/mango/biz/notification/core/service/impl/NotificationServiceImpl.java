package io.mango.biz.notification.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.realtime.api.RealtimePublisher;
import io.mango.biz.notification.api.NotificationApi;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import io.mango.biz.notification.core.entity.SysNotification;
import io.mango.biz.notification.core.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationApi {

    private final SysNotificationMapper messageMapper;
    private final RealtimePublisher messagePublisher;
    private final ObjectMapper objectMapper;

    @Override
    public R<Long> send(SysNotificationPo po) {
        SysNotification entity = new SysNotification();
        entity.setNotificationType(po.getNotificationType());
        entity.setTitle(po.getTitle());
        entity.setContent(po.getContent());
        entity.setPriority(po.getPriority());
        entity.setReadStatus(0);
        entity.setUserId(po.getUserId());
        messageMapper.insert(entity);

        String json = toJson(entity);
        messagePublisher.publishToUser(po.getUserId(), "message", json);

        return R.ok(entity.getId());
    }

    @Override
    public R<Long> broadcast(SysNotificationPo po) {
        SysNotification entity = new SysNotification();
        entity.setNotificationType(po.getNotificationType());
        entity.setTitle(po.getTitle());
        entity.setContent(po.getContent());
        entity.setPriority(po.getPriority());
        entity.setReadStatus(0);
        messageMapper.insert(entity);

        String json = toJson(entity);
        messagePublisher.broadcast("message", json);

        return R.ok(entity.getId());
    }

    @Override
    public R<List<SysNotificationVO>> listByUser(Long userId) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getUserId, userId)
               .orderByDesc(SysNotification::getCreateTime);
        List<SysNotification> list = messageMapper.selectList(wrapper);
        List<SysNotificationVO> vos = list.stream().map(this::convertToVO).collect(Collectors.toList());
        return R.ok(vos);
    }

    @Override
    public R<Boolean> markRead(Long messageId) {
        SysNotification message = messageMapper.selectById(messageId);
        if (message != null) {
            message.setReadStatus(1);
            message.setReadTime(java.time.LocalDateTime.now());
            messageMapper.updateById(message);
        }
        return R.ok(true);
    }

    private SysNotificationVO convertToVO(SysNotification entity) {
        SysNotificationVO vo = new SysNotificationVO();
        vo.setId(entity.getId());
        vo.setNotificationType(entity.getNotificationType());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setUserId(entity.getUserId());
        vo.setPriority(entity.getPriority());
        vo.setReadStatus(entity.getReadStatus());
        vo.setReadTime(entity.getReadTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String toJson(SysNotification entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize message entity: {}", entity.getId(), e);
            return "{}";
        }
    }
}
