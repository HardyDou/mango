package io.mango.notice.starter.remote;

import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.NoticeSendEventCommand;
import io.mango.notice.support.context.NoticeEventContextExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends notice events through the remote notice API after the owning business
 * transaction commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeSendEventListener {

    private final NoticeApi noticeApi;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNoticeSendEvent(NoticeSendEventCommand event) {
        try {
            NoticeEventContextExecutor.run(event, () -> noticeApi.send(event));
        } catch (RuntimeException ex) {
            log.warn("Send remote notice event failed. bizType={}, bizId={}",
                    event.getBizType(), event.getBizId(), ex);
        }
    }

}
