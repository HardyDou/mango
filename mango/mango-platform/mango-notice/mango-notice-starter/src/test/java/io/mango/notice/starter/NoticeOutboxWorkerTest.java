package io.mango.notice.starter;

import io.mango.infra.kv.api.IOutboxDispatcher;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class NoticeOutboxWorkerTest {

    @Test
    void shouldStartDispatchLoopOnlyAfterApplicationReady() {
        IOutboxDispatcher dispatcher = mock(IOutboxDispatcher.class);

        try (NoticeOutboxWorker worker = new NoticeOutboxWorker(dispatcher, "test", 0L, 1000L)) {
            verify(dispatcher, after(100).never()).dispatchOnce();

            worker.startOnReady();

            verify(dispatcher, timeout(1000)).dispatchOnce();
        }
    }
}
