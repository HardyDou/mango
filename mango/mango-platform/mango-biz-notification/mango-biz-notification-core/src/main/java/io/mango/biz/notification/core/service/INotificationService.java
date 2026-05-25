package io.mango.biz.notification.core.service;

import io.mango.biz.notification.api.NotificationApi;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;

import java.util.List;
import java.util.Map;

public interface INotificationService extends NotificationApi {

    PageResult<SysNotificationVO> pageByUser(Long userId, int page, int size, Boolean unreadOnly);

    SysNotificationVO get(Long id);

    long unreadCount(Long userId);

    boolean markReadBatch(List<Long> ids);

    boolean markReadBatchForUser(List<Long> ids, Long userId);

    boolean delete(Long id);

    boolean deleteForUser(Long id, Long userId);

    R<Boolean> markReadForUser(Long messageId, Long userId);

    Map<String, Object> sendForFrontend(io.mango.biz.notification.api.po.SysNotificationPo po);
}
