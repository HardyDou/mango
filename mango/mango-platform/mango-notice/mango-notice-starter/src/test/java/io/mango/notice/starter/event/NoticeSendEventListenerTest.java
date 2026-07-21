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
        event.setAppCode("event-app");
        event.setRealm("EVENT_REALM");
        event.setBizType("notice.test");
        AtomicReference<MangoContextSnapshot> contextDuringSend = new AtomicReference<>();
        when(noticeApi.send(event)).thenAnswer(invocation -> {
            contextDuringSend.set(MangoContextHolder.get());
            return null;
        });
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                10L, "caller-tenant", "caller", "CALLER_REALM", "USER", "ORG", 20L, "caller-app"));

        new NoticeSendEventListener(noticeApi).onNoticeSendEvent(event);

        verify(noticeApi).send(event);
        assertThat(contextDuringSend.get().tenantId()).isEqualTo("event-tenant");
        assertThat(contextDuringSend.get().appCode()).isEqualTo("event-app");
        assertThat(contextDuringSend.get().realm()).isEqualTo("EVENT_REALM");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("caller-tenant");
        assertThat(MangoContextHolder.appCode()).isEqualTo("caller-app");
        assertThat(MangoContextHolder.get().realm()).isEqualTo("CALLER_REALM");
    }

    @Test
    void missingEventTenant_usesCurrentTenantForBackwardCompatibility() {
        NoticeApi noticeApi = mock(NoticeApi.class);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setBizType("notice.test");
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                null, "current-tenant", null, "CURRENT_REALM", null, null, null, "current-app"));

        new NoticeSendEventListener(noticeApi).onNoticeSendEvent(event);

        verify(noticeApi).send(event);
        assertThat(event.getTenantId()).isEqualTo("current-tenant");
        assertThat(event.getAppCode()).isEqualTo("current-app");
        assertThat(event.getRealm()).isEqualTo("CURRENT_REALM");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("current-tenant");
    }
}
