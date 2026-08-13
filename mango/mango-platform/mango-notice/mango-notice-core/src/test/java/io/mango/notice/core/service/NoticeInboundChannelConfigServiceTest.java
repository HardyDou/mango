package io.mango.notice.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mango.notice.api.enums.NoticeChannelCapabilityMode;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import org.junit.jupiter.api.Test;

class NoticeInboundChannelConfigServiceTest {
    private final NoticeChannelConfigMapper mapper = mock(NoticeChannelConfigMapper.class);
    private final NoticeChannelSecretMaterializer materializer = mock(NoticeChannelSecretMaterializer.class);
    private final NoticeInboundChannelConfigService service =
            new NoticeInboundChannelConfigService(mapper, materializer);

    @Test
    void sendOnlyChannelCannotBeResolvedByAnonymousInboundEndpoint() {
        NoticeChannelConfigEntity entity = entity(NoticeChannelCapabilityMode.SEND);
        when(mapper.selectInboundConfigById(765L)).thenReturn(entity);

        assertThatThrownBy(() -> service.resolve(765L, NoticeChannelType.WECOM))
                .hasMessageContaining("渠道用途不允许接收消息");
    }

    @Test
    void receiveChannelCanBeResolved() {
        NoticeChannelConfigEntity entity = entity(NoticeChannelCapabilityMode.RECEIVE);
        when(mapper.selectInboundConfigById(765L)).thenReturn(entity);
        when(materializer.materialize(entity)).thenReturn("{\"callbackToken\":\"referenced\"}");

        NoticeInboundChannelConfigService.ResolvedInboundChannelConfig resolved =
                service.resolve(765L, NoticeChannelType.WECOM);

        assertThat(resolved.id()).isEqualTo(765L);
        assertThat(resolved.tenantId()).isEqualTo("tenant-765");
    }

    private NoticeChannelConfigEntity entity(NoticeChannelCapabilityMode mode) {
        NoticeChannelConfigEntity entity = new NoticeChannelConfigEntity();
        entity.setId(765L);
        entity.setTenantId("tenant-765");
        entity.setConfigCode("IT_765_WECOM");
        entity.setChannelType(NoticeChannelType.WECOM);
        entity.setProviderCode("WECOM");
        entity.setCapabilityMode(mode);
        entity.setEnabled(true);
        return entity;
    }
}
