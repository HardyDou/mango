package io.mango.integration.starter.resource;

import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.integration.core.service.IIntegrationConnectorService;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.PortableResourceIds;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 连接器资源声明的确定性主键测试。
 *
 * <p>历史缺陷（issue #1317）：声明落库走雪花主键，导致冷基线回放两次得到不同的
 * {@code integration_connector.id}。这里锁定「同一连接器编码重复声明得到同一主键」。</p>
 */
class IntegrationConnectorResourceHandlerTest {

    private final IIntegrationConnectorService connectorService = mock(IIntegrationConnectorService.class);

    private final List<IntegrationConnectorCommand> saved = new ArrayList<>();

    private final IntegrationConnectorResourceHandler handler =
            new IntegrationConnectorResourceHandler(connectorService);

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            saved.add(invocation.getArgument(0));
            return Boolean.TRUE;
        }).when(connectorService).saveDeclaration(org.mockito.ArgumentMatchers.any());
        when(connectorService.findIdByCode(anyString())).thenAnswer(invocation -> lastSavedId());
    }

    @Test
    void sameConnectorCodeDeclaredTwiceKeepsStableId() {
        handler.upsert(declaration("wecom-message"));
        long firstId = lastSavedId();
        ResourceSyncResult second = handler.upsert(declaration("wecom-message"));

        assertThat(saved).hasSize(2);
        assertThat(firstId).isEqualTo(PortableResourceIds.stable("integration_connector", "wecom-message"));
        assertThat(lastSavedId()).isEqualTo(firstId);
        assertThat(second.getTargetId()).isEqualTo(firstId);
    }

    @Test
    void differentConnectorCodesProduceDifferentIds() {
        handler.upsert(declaration("wecom-message"));
        long wecomId = lastSavedId();
        handler.upsert(declaration("alipay-trade"));
        long alipayId = lastSavedId();

        assertThat(wecomId).isNotEqualTo(alipayId);
        assertThat(alipayId).isEqualTo(PortableResourceIds.stable("integration_connector", "alipay-trade"));
    }

    @Test
    void connectorCodeIsTrimmedBeforeDerivingId() {
        handler.upsert(declaration("  wecom-message  "));

        assertThat(lastSavedId()).isEqualTo(PortableResourceIds.stable("integration_connector", "wecom-message"));
    }

    private long lastSavedId() {
        return saved.get(saved.size() - 1).getId();
    }

    private ResourceDeclaration declaration(String connectorCode) {
        String code = connectorCode.trim();
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("it-integration-connector-" + code);
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.INTEGRATION_CONNECTOR);
        declaration.setModuleCode("integration");
        declaration.setBizKey("integration.connector." + code);
        declaration.setName(code);
        declaration.setTargetModule("integration");
        declaration.setFields(new LinkedHashMap<>());
        put(declaration, "connectorCode", ResourceFieldType.STRING, connectorCode);
        put(declaration, "connectorName", ResourceFieldType.STRING, "企业微信应用消息");
        put(declaration, "status", ResourceFieldType.INT, 1);
        return declaration;
    }

    private void put(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }
}
