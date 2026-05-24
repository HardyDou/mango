package io.mango.biz.notification.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import io.mango.biz.notification.core.entity.SysNotification;
import io.mango.biz.notification.core.mapper.SysNotificationMapper;
import io.mango.biz.notification.core.service.INotificationService;
import io.mango.common.vo.PageResult;
import io.mango.infra.realtime.api.RealtimeApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final SysNotificationMapper messageMapper;
    private final RealtimeApi realtimeApi;
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
        realtimeApi.publishToUser(po.getUserId(), "message", json);

        return R.ok(entity.getId());
    }

    @Override
    public Map<String, Object> sendForFrontend(SysNotificationPo po) {
        Long id = send(po).getData();
        return Map.of(
                "messageId", id,
                "successCount", id == null ? 0 : 1,
                "failCount", id == null ? 1 : 0);
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
        realtimeApi.broadcast("message", json);

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

    @Override
    public R<Boolean> markReadForUser(Long messageId, Long userId) {
        SysNotification entity = new SysNotification();
        entity.setReadStatus(1);
        entity.setReadTime(java.time.LocalDateTime.now());
        return R.ok(messageMapper.update(entity,
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getId, messageId)
                        .eq(SysNotification::getUserId, userId)) > 0);
    }

    @Override
    public PageResult<SysNotificationVO> pageByUser(Long userId, int page, int size, Boolean unreadOnly) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.and(query -> query.eq(SysNotification::getUserId, userId).or().isNull(SysNotification::getUserId));
        }
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(SysNotification::getReadStatus, 0);
        }
        wrapper.orderByDesc(SysNotification::getCreateTime);
        Page<SysNotification> result = messageMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::convertToVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public SysNotificationVO get(Long id) {
        SysNotification entity = messageMapper.selectById(id);
        return entity == null ? null : convertToVO(entity);
    }

    @Override
    public long unreadCount(Long userId) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.and(query -> query.eq(SysNotification::getUserId, userId).or().isNull(SysNotification::getUserId));
        }
        wrapper.eq(SysNotification::getReadStatus, 0);
        return messageMapper.selectCount(wrapper);
    }

    @Override
    public boolean markReadBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        SysNotification entity = new SysNotification();
        entity.setReadStatus(1);
        entity.setReadTime(java.time.LocalDateTime.now());
        return messageMapper.update(entity,
                new LambdaQueryWrapper<SysNotification>().in(SysNotification::getId, ids)) >= 0;
    }

    @Override
    public boolean markReadBatchForUser(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        SysNotification entity = new SysNotification();
        entity.setReadStatus(1);
        entity.setReadTime(java.time.LocalDateTime.now());
        return messageMapper.update(entity,
                new LambdaQueryWrapper<SysNotification>()
                        .in(SysNotification::getId, ids)
                        .eq(SysNotification::getUserId, userId)) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return messageMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteForUser(Long id, Long userId) {
        return messageMapper.delete(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getId, id)
                .eq(SysNotification::getUserId, userId)) > 0;
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
