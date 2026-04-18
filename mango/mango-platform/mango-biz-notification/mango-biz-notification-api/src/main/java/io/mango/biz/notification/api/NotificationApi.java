package io.mango.biz.notification.api;

import io.mango.common.result.R;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.vo.SysNotificationVO;

import java.util.List;

public interface NotificationApi {

    R<Long> send(SysNotificationPo po);

    R<Long> broadcast(SysNotificationPo po);

    R<List<SysNotificationVO>> listByUser(Long userId);

    R<Boolean> markRead(Long messageId);
}
