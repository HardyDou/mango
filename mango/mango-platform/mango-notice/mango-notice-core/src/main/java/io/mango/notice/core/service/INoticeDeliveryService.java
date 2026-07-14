package io.mango.notice.core.service;

import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.vo.NoticeSendResultVO;

/** Owns notice task creation, routing and channel delivery execution. */
public interface INoticeDeliveryService {

    NoticeSendResultVO send(SendNoticeCommand command);

    String findTaskTenantId(Long taskId);

    int executeTask(Long taskId);

    boolean hasRetryWaitingRecords(Long taskId);

    void finalizeRetryWaitingRecords(Long taskId, String failReason);
}
