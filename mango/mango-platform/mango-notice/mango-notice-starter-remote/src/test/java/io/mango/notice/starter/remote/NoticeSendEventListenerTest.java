package io.mango.notice.starter.remote;

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
    void eventTenant_isPropagatedToRemoteCall_andPreviousContextIsRestored() {
        NoticeApi noticeApi = mock(NoticeApi.class);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setTenantId("remote-tenant");
        event.setAppCode("remote-app");
        event.setRealm("REMOTE_REALM");
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
        assertThat(contextDuringSend.get().tenantId()).isEqualTo("remote-tenant");
        assertThat(contextDuringSend.get().appCode()).isEqualTo("remote-app");
        assertThat(contextDuringSend.get().realm()).isEqualTo("REMOTE_REALM");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("caller-tenant");
        assertThat(MangoContextHolder.appCode()).isEqualTo("caller-app");
        assertThat(MangoContextHolder.get().realm()).isEqualTo("CALLER_REALM");
    }
}
