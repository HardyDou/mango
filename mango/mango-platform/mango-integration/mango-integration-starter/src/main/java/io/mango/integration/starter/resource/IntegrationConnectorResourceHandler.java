package io.mango.integration.starter.resource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.integration.core.service.IIntegrationConnectorService;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 集成连接器定义的资源声明处理器。
 *
 * <p>连接器定义由部署侧声明并同步，保存即生效；声明缺失时停用连接器，不删除数据。</p>
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects the managed connector service; copying it is not valid"))
public class IntegrationConnectorResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "integration_connector";

    private final IIntegrationConnectorService connectorService;

    @Override
    public String resourceType() {
        return ResourceTypes.INTEGRATION_CONNECTOR;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("connectorCode")
                .requiredField("connectorName")
                .fieldDescription("connectorCode", "连接器编码，例如 wecom-message。")
                .fieldDescription("connectorName", "连接器名称。")
                .fieldDescription("category", "用途分类，取值来自字典 integration_connector_category。")
                .fieldDescription("provider", "厂商标识，例如 WECOM。")
                .fieldDescription("status", "状态：0-停用，1-启用，默认 1。")
                .fieldDescription("configSchema", "配置规范 JSON Schema 文本。")
                .fieldDescription("authSchema", "认证规范 JSON 文本。")
                .fieldDescription("capabilitiesJson", "能力清单 JSON 数组文本，元素含 code/name/description。")
                .fieldDescription("tags", "标签列表。")
                .fieldDescription("docsUrl", "文档地址。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        IntegrationConnectorCommand command = new IntegrationConnectorCommand();
        command.setConnectorCode(requiredString(resource, "connectorCode"));
        command.setConnectorName(requiredString(resource, "connectorName"));
        command.setCategory(stringField(resource, "category"));
        command.setProvider(stringField(resource, "provider"));
        command.setStatus(integerField(resource, "status", 1));
        command.setConfigSchema(stringField(resource, "configSchema"));
        command.setAuthSchema(stringField(resource, "authSchema"));
        command.setCapabilitiesJson(stringField(resource, "capabilitiesJson"));
        command.setTags(stringListField(resource, "tags"));
        command.setDocsUrl(stringField(resource, "docsUrl"));
        connectorService.saveDeclaration(command);
        Long targetId = connectorService.findIdByCode(command.getConnectorCode());
        return ResourceSyncResult.of(targetId, TARGET_TABLE,
                "Integration connector synced: " + command.getConnectorCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String connectorCode = stringField(resource, "connectorCode");
        if (!StringUtils.hasText(connectorCode)) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Integration connector disabled: skipped");
        }
        Boolean changed = connectorService.updateStatusByCode(connectorCode.trim(), 0);
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Integration connector disabled: " + connectorCode + ", changed=" + Boolean.TRUE.equals(changed));
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        return disable(resource);
    }

    private String requiredString(ResourceDeclaration resource, String fieldName) {
        String value = stringField(resource, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(resourceType() + " field is required: " + fieldName);
        }
        return value.trim();
    }

    private String stringField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerField(ResourceDeclaration resource, String fieldName, int defaultValue) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private List<String> stringListField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            list.forEach(item -> {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            });
            return result;
        }
        return List.of(String.valueOf(value));
    }

    private Object fieldValue(ResourceDeclaration resource, String fieldName) {
        var field = resource.getFields().get(fieldName);
        return field == null ? null : field.getValue();
    }
}
