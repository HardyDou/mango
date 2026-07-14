package io.mango.notice.starter.event;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.NoticeSendEventCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeSendEventListenerTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void eventTenant_isActiveDuringDelivery_andPreviousContextIsRestored() {
        NoticeApi noticeApi = mock(NoticeApi.class);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setTenantId("event-tenant");
        event.setBizType("notice.test");
        AtomicReference<String> tenantDuringSend = new AtomicReference<>();
        when(noticeApi.send(event)).thenAnswer(invocation -> {
            tenantDuringSend.set(MangoContextHolder.tenantId());
            return null;
        });
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("caller-tenant"));

        new NoticeSendEventListener(noticeApi).onNoticeSendEvent(event);

        verify(noticeApi).send(event);
        assertThat(tenantDuringSend).hasValue("event-tenant");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("caller-tenant");
    }

    @Test
    void missingEventTenant_usesCurrentTenantForBackwardCompatibility() {
        NoticeApi noticeApi = mock(NoticeApi.class);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setBizType("notice.test");
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("current-tenant"));

        new NoticeSendEventListener(noticeApi).onNoticeSendEvent(event);

        verify(noticeApi).send(event);
        assertThat(event.getTenantId()).isEqualTo("current-tenant");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("current-tenant");
    }
}
