package io.mango.integration.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.integration.api.command.IntegrationInstanceCommand;
import io.mango.integration.api.command.IntegrationInstanceStatusCommand;
import io.mango.integration.api.enums.IntegrationCode;
import io.mango.integration.api.enums.IntegrationCredentialStatus;
import io.mango.integration.api.enums.IntegrationInstanceStatus;
import io.mango.integration.api.query.IntegrationInstancePageQuery;
import io.mango.integration.api.vo.IntegrationInstanceVO;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import io.mango.integration.core.entity.IntegrationCredentialEntity;
import io.mango.integration.core.entity.IntegrationInstanceEntity;
import io.mango.integration.core.mapper.IntegrationCredentialMapper;
import io.mango.integration.core.entity.IntegrationCallLogEntity;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import io.mango.integration.core.mapper.IntegrationCallLogMapper;
import io.mango.integration.core.mapper.IntegrationConnectorMapper;
import io.mango.integration.core.mapper.IntegrationInstanceMapper;
import io.mango.integration.core.service.IIntegrationConnectorService;
import io.mango.integration.core.service.IIntegrationInstanceService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 集成连接实例服务实现。
 *
 * <p>实例配置按连接器的配置规范校验，能力必须来自连接器声明的能力清单。</p>
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects managed mappers and object mapper"))
public class IntegrationInstanceService implements IIntegrationInstanceService {

    private static final String CALLBACK_PATH_PREFIX = "/integration/callback/";
    private static final String PROD = "PROD";
    private static final String TEST = "TEST";

    private final IntegrationInstanceMapper instanceMapper;
    private final IntegrationCallLogMapper callLogMapper;
    private final IntegrationConnectorMapper connectorMapper;
    private final io.mango.integration.api.spi.IntegrationDeclarationProvider declarationProvider;
    private final io.mango.integration.support.client.IntegrationCallRecorder callRecorder;
    private final io.mango.integration.core.support.IntegrationTestProperties testProperties;
    private final org.springframework.beans.factory.ObjectProvider<io.mango.system.api.DictApi> dictApiProvider;
    private final IntegrationCredentialMapper credentialMapper;
    private final IIntegrationConnectorService connectorService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<IntegrationInstanceVO> page(IntegrationInstancePageQuery query) {
        IntegrationInstancePageQuery validated = Require.nonNull(
                query, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例分页查询条件不能为空");
        LambdaQueryWrapper<IntegrationInstanceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IntegrationInstanceEntity::getTenantId, currentTenantId())
                .eq(validated.getStatus() != null,
                        IntegrationInstanceEntity::getStatus, validated.getStatus())
                .eq(StringUtils.hasText(validated.getConnectorCode()),
                        IntegrationInstanceEntity::getConnectorCode, validated.getConnectorCode())
                .and(StringUtils.hasText(validated.getKeyword()), condition -> condition
                        .like(IntegrationInstanceEntity::getInstanceName, validated.getKeyword())
                        .or().like(IntegrationInstanceEntity::getInstanceCode, validated.getKeyword()))
                .orderByDesc(IntegrationInstanceEntity::getUpdatedAt);
        Page<IntegrationInstanceEntity> page = instanceMapper.selectPage(
                Page.of(validated.getPage(), validated.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public IntegrationInstanceVO detail(String instanceCode) {
        return toVO(requireInstance(instanceCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean save(IntegrationInstanceCommand command) {
        IntegrationInstanceCommand validated = Require.nonNull(
                command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例保存命令不能为空");
        IntegrationConnectorEntity connector =
                connectorService.require(validated.getConnectorCode());
        Require.isTrue(connectorService.isEnabled(connector),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器已停用，不能创建或启用实例");
        validateEnvironment(validated.getEnvironment());
        validateConfigValues(connector, validated.getConfigValuesJson());
        String capabilities = normalizeCapabilities(connector, validated.getCapabilitiesJson());
        IntegrationCredentialEntity credential = null;
        if (StringUtils.hasText(validated.getCredentialCode())) {
            credential = findCredential(validated.getCredentialCode());
            Require.notNull(credential, IntegrationCode.INTEGRATION_NOT_FOUND, "凭证不存在");
            Require.isTrue(connector.getConnectorCode().equals(credential.getConnectorCode()),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证与连接器不匹配");
            Require.isTrue(IntegrationCredentialStatus.ACTIVE.name().equals(credential.getStatus()),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证不可用，请先完成配置或轮换");
            Require.isTrue(credential.getExpiresAt() == null
                            || credential.getExpiresAt().isAfter(java.time.LocalDateTime.now()),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证已过期，请先轮换");
        }
        if (validated.getStatus() != null && validated.getStatus() == 1 && requiresCredential(connector)) {
            Require.notNull(credential, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "启用实例前必须绑定凭证");
        }
        IntegrationInstanceEntity existing = findInstance(validated.getInstanceCode());
        boolean creating = existing == null;
        IntegrationInstanceEntity entity = creating
                ? new IntegrationInstanceEntity() : existing;
        if (creating) {
            entity.setTenantId(currentTenantId());
            entity.setInstanceCode(validated.getInstanceCode().trim());
        }
        entity.setInstanceName(validated.getInstanceName().trim());
        entity.setConnectorCode(connector.getConnectorCode());
        entity.setEnvironment(StringUtils.hasText(validated.getEnvironment())
                ? validated.getEnvironment().trim().toUpperCase() : PROD);
        entity.setConfigValuesJson(normalizeJsonObject(validated.getConfigValuesJson(), "配置值必须是JSON对象"));
        entity.setCredentialCode(StringUtils.hasText(validated.getCredentialCode())
                ? validated.getCredentialCode().trim() : null);
        entity.setCapabilitiesJson(capabilities);
        entity.setStatus(validated.getStatus());
        entity.setOwner(validated.getOwner());
        entity.setRemark(validated.getRemark());
        if (creating) {
            Require.isTrue(instanceMapper.insert(entity) == 1,
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例保存失败");
        } else {
            Require.isTrue(updateWithRevision(entity),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例已被他人修改，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changeStatus(IntegrationInstanceStatusCommand command) {
        IntegrationInstanceStatusCommand validated = Require.nonNull(
                command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例状态命令不能为空");
        IntegrationInstanceEntity entity = requireInstance(validated.getInstanceCode());
        if (validated.getStatus() == 1) {
            IntegrationConnectorEntity connector =
                    connectorService.require(entity.getConnectorCode());
            Require.isTrue(connectorService.isEnabled(connector),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器已停用，不能启用实例");
            if (requiresCredential(connector)) {
                Require.isTrue(StringUtils.hasText(entity.getCredentialCode()),
                        IntegrationCode.INTEGRATION_BUSINESS_ERROR, "启用实例前必须绑定凭证");
            }
        }
        entity.setStatus(validated.getStatus());
        Require.isTrue(updateWithRevision(entity),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例已被他人修改，请刷新后重试");
        return true;
    }

    @Override
    public io.mango.integration.api.vo.IntegrationConnectionTestVO testConnection(
            io.mango.integration.api.command.IntegrationConnectionTestCommand command) {
        io.mango.integration.api.command.IntegrationConnectionTestCommand validated = Require.nonNull(
                command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接测试命令不能为空");
        IntegrationInstanceEntity entity = requireInstance(validated.getInstanceCode());
        Require.isTrue(entity.getStatus() != null && entity.getStatus() == 1,
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例未启用，不能连接测试");
        IntegrationConnectorEntity connector = connectorService.require(entity.getConnectorCode());
        if (requiresCredential(connector)) {
            Require.isTrue(StringUtils.hasText(entity.getCredentialCode()),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例未绑定凭证");
        }
        validateTestTarget(entity.getConfigValuesJson());
        io.mango.integration.support.client.IntegrationClient client =
                io.mango.integration.support.client.IntegrationClient.of(
                        declarationProvider, entity.getInstanceCode(), new io.mango.integration.support.auth.IntegrationTokenCache(),
                        callRecorder);
        String path = StringUtils.hasText(validated.getTestPath()) ? validated.getTestPath() : "/";
        io.mango.integration.support.client.IntegrationCallResult result = client.get(path);
        io.mango.integration.api.vo.IntegrationConnectionTestVO vo =
                new io.mango.integration.api.vo.IntegrationConnectionTestVO();
        vo.setSuccess(result.isSuccess());
        vo.setStatusCode(result.getStatusCode());
        vo.setElapsedMs(result.getElapsedMs());
        vo.setMessage(result.getMessage());
        return vo;
    }

    @Override
    public java.util.List<io.mango.integration.api.vo.IntegrationCallLogVO> callLogs(String instanceCode, Integer limit) {
        IntegrationInstanceEntity instance = requireInstance(instanceCode);
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return callLogMapper.selectList(new LambdaQueryWrapper<IntegrationCallLogEntity>()
                        .eq(IntegrationCallLogEntity::getTenantId, currentTenantId())
                        .eq(IntegrationCallLogEntity::getInstanceCode, instance.getInstanceCode())
                        .orderByDesc(IntegrationCallLogEntity::getCreatedAt)
                        .last("LIMIT " + size))
                .stream()
                .map(entity -> {
                    io.mango.integration.api.vo.IntegrationCallLogVO vo =
                            new io.mango.integration.api.vo.IntegrationCallLogVO();
                    vo.setInstanceCode(entity.getInstanceCode());
                    vo.setConnectorCode(entity.getConnectorCode());
                    vo.setHttpMethod(entity.getHttpMethod());
                    vo.setPath(entity.getPath());
                    vo.setStatusCode(entity.getStatusCode());
                    vo.setElapsedMs(entity.getElapsedMs());
                    vo.setSuccess(entity.getSuccess() != null && entity.getSuccess() == 1);
                    vo.setMessage(entity.getMessage());
                    vo.setCreatedAt(entity.getCreatedAt());
                    return vo;
                })
                .toList();
    }

    private void validateTestTarget(String configValuesJson) {
        JsonNode config = readJson(configValuesJson);
        String baseUrl = config == null ? null : config.path("apiBaseUrl").asText(null);
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = config == null ? null : config.path("baseUrl").asText(null);
        }
        Require.isTrue(StringUtils.hasText(baseUrl),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例未配置接口地址");
        java.net.URI uri;
        try {
            uri = java.net.URI.create(baseUrl);
        } catch (IllegalArgumentException exception) {
            Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "接口地址不合法");
            return;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
        Require.isTrue("http".equals(scheme) || "https".equals(scheme),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "接口地址必须是http或https");
        if (testProperties.isAllowPrivateNetwork()) {
            return;
        }
        String host = uri.getHost();
        Require.isTrue(StringUtils.hasText(host),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "接口地址缺少主机");
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(host);
            boolean privateTarget = address.isLoopbackAddress() || address.isAnyLocalAddress()
                    || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isMulticastAddress();
            Require.isTrue(!privateTarget, IntegrationCode.INTEGRATION_BUSINESS_ERROR,
                    "连接测试目标不在允许范围内");
        } catch (java.net.UnknownHostException exception) {
            Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接测试目标无法解析");
        }
    }

    private void validateDictValue(String dictCode, String value, String fieldName) {
        Require.isTrue(dictAllows(dictCode, value), IntegrationCode.INTEGRATION_BUSINESS_ERROR,
                "配置取值不在字典范围内：" + fieldName);
    }

    private boolean dictAllows(String dictCode, String value) {
        io.mango.system.api.DictApi dictApi = dictApiProvider.getIfAvailable();
        if (dictApi == null) {
            return true;
        }
        try {
            io.mango.common.result.R<java.util.List<io.mango.system.api.vo.DictOptionVO>> response =
                    dictApi.getOptions(dictCode);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                return true;
            }
            return response.getData().stream().anyMatch(option -> value.equals(option.getValue()));
        } catch (Exception exception) {
            return true;
        }
    }

    private boolean updateWithRevision(IntegrationInstanceEntity entity) {
        int expected = entity.getRevision() == null ? 0 : entity.getRevision();
        entity.setRevision(expected + 1);
        return instanceMapper.update(entity,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<IntegrationInstanceEntity>()
                        .eq(IntegrationInstanceEntity::getId, entity.getId())
                        .eq(IntegrationInstanceEntity::getRevision, expected)) == 1;
    }

    private boolean requiresCredential(IntegrationConnectorEntity connector) {
        JsonNode schema = readJson(connector.getAuthSchema());
        String authMode = schema == null ? "SECRET" : schema.path("authMode").asText("SECRET");
        return !"NONE".equalsIgnoreCase(authMode);
    }

    private void validateEnvironment(String environment) {
        if (!StringUtils.hasText(environment)) {
            return;
        }
        String normalized = environment.trim().toUpperCase();
        Require.isTrue(PROD.equals(normalized) || TEST.equals(normalized),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "环境只能是PROD或TEST");
    }

    private void validateConfigValues(IntegrationConnectorEntity connector, String configValuesJson) {
        JsonNode schema = readJson(connector.getConfigSchema());
        Require.notNull(schema, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器缺少配置规范");
        JsonNode config = readJson(normalizeJsonObject(configValuesJson, "配置值必须是JSON对象"));
        Require.notNull(config, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "配置值必须是JSON对象");
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode field : required) {
                String code = field.asText();
                Require.isTrue(config.hasNonNull(code) && !config.get(code).asText().isBlank(),
                        IntegrationCode.INTEGRATION_BUSINESS_ERROR, "缺少必填配置：" + code);
            }
        }
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            config.fieldNames().forEachRemaining(name ->
                    Require.isTrue(properties.has(name),
                            IntegrationCode.INTEGRATION_BUSINESS_ERROR, "配置包含未声明的字段：" + name));
            validateFieldRules(properties, config);
        }
    }

    private void validateFieldRules(JsonNode properties, JsonNode config) {
        properties.fieldNames().forEachRemaining(name -> {
            JsonNode value = config.get(name);
            if (value == null || value.isNull()) {
                return;
            }
            JsonNode rule = properties.get(name);
            String text = value.asText();
            String dictCode = rule.path("dictCode").asText(null);
            if (StringUtils.hasText(dictCode)) {
                validateDictValue(dictCode, text, name);
            }
            JsonNode enumNode = rule.get("enum");
            if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
                boolean matched = false;
                for (JsonNode option : enumNode) {
                    if (option.asText().equals(text)) {
                        matched = true;
                        break;
                    }
                }
                Require.isTrue(matched, IntegrationCode.INTEGRATION_BUSINESS_ERROR,
                        "配置取值不在允许范围内：" + name);
            }
            String pattern = rule.path("pattern").asText(null);
            if (pattern != null && !pattern.isBlank()) {
                Require.isTrue(text.matches(pattern), IntegrationCode.INTEGRATION_BUSINESS_ERROR,
                        "配置格式不符合要求：" + name);
            }
            int minLength = rule.path("minLength").asInt(-1);
            if (minLength >= 0) {
                Require.isTrue(text.length() >= minLength, IntegrationCode.INTEGRATION_BUSINESS_ERROR,
                        "配置长度小于最小值：" + name);
            }
            int maxLength = rule.path("maxLength").asInt(-1);
            if (maxLength >= 0) {
                Require.isTrue(text.length() <= maxLength, IntegrationCode.INTEGRATION_BUSINESS_ERROR,
                        "配置长度超过最大值：" + name);
            }
            if (value.isNumber()) {
                double number = value.asDouble();
                JsonNode minimum = rule.get("minimum");
                if (minimum != null && number < minimum.asDouble()) {
                    Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "配置数值小于最小值：" + name);
                }
                JsonNode maximum = rule.get("maximum");
                if (maximum != null && number > maximum.asDouble()) {
                    Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "配置数值超过最大值：" + name);
                }
            }
        });
    }

    private String normalizeCapabilities(IntegrationConnectorEntity connector, String capabilitiesJson) {
        Set<String> declared = new LinkedHashSet<>();
        JsonNode declaredNode = readJson(connector.getCapabilitiesJson());
        if (declaredNode != null && declaredNode.isArray()) {
            declaredNode.forEach(node -> {
                String code = node.isObject() ? node.path("code").asText(null) : node.asText(null);
                if (code != null && !code.isBlank()) {
                    declared.add(code);
                }
            });
        }
        if (!StringUtils.hasText(capabilitiesJson)) {
            return null;
        }
        JsonNode node = readJson(capabilitiesJson);
        Require.notNull(node, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "能力清单必须是合法JSON数组");
        Require.isTrue(node.isArray(), IntegrationCode.INTEGRATION_BUSINESS_ERROR, "能力清单必须是JSON数组");
        List<String> selected = new ArrayList<>();
        node.forEach(item -> {
            String value = item.asText();
            Require.isTrue(declared.contains(value),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器未声明能力：" + value);
            selected.add(value);
        });
        try {
            return objectMapper.writeValueAsString(selected);
        } catch (Exception exception) {
            Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "能力清单序列化失败");
            return null;
        }
    }

    private String normalizeJsonObject(String json, String message) {
        if (!StringUtils.hasText(json)) {
            return "{}";
        }
        JsonNode node = readJson(json);
        Require.notNull(node, IntegrationCode.INTEGRATION_BUSINESS_ERROR, message);
        Require.isTrue(node.isObject(), IntegrationCode.INTEGRATION_BUSINESS_ERROR, message);
        return json.trim();
    }

    private IntegrationInstanceEntity requireInstance(String instanceCode) {
        Require.notBlank(instanceCode, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "实例编码不能为空");
        IntegrationInstanceEntity entity = findInstance(instanceCode);
        Require.notNull(entity, IntegrationCode.INTEGRATION_NOT_FOUND, "连接实例不存在");
        return entity;
    }

    private IntegrationInstanceEntity findInstance(String instanceCode) {
        if (!StringUtils.hasText(instanceCode)) {
            return null;
        }
        return instanceMapper.selectOne(new LambdaQueryWrapper<IntegrationInstanceEntity>()
                .eq(IntegrationInstanceEntity::getTenantId, currentTenantId())
                .eq(IntegrationInstanceEntity::getInstanceCode, instanceCode.trim())
                .last("LIMIT 1"));
    }

    private IntegrationCredentialEntity findCredential(String credentialCode) {
        return credentialMapper.selectOne(new LambdaQueryWrapper<IntegrationCredentialEntity>()
                .eq(IntegrationCredentialEntity::getTenantId, currentTenantId())
                .eq(IntegrationCredentialEntity::getCredentialCode, credentialCode.trim())
                .last("LIMIT 1"));
    }

    private JsonNode readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return null;
        }
    }

    private String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.isTrue(StringUtils.hasText(tenantId),
                IntegrationCode.INTEGRATION_UNAUTHORIZED, "缺少当前租户上下文");
        return tenantId;
    }

    private IntegrationInstanceVO toVO(IntegrationInstanceEntity entity) {
        IntegrationInstanceVO vo = new IntegrationInstanceVO();
        vo.setInstanceCode(entity.getInstanceCode());
        vo.setInstanceName(entity.getInstanceName());
        vo.setConnectorCode(entity.getConnectorCode());
        vo.setEnvironment(entity.getEnvironment());
        vo.setConfigValuesJson(entity.getConfigValuesJson());
        vo.setCredentialCode(entity.getCredentialCode());
        vo.setCapabilitiesJson(entity.getCapabilitiesJson());
        vo.setCallbackPath(CALLBACK_PATH_PREFIX + entity.getConnectorCode() + "/" + entity.getInstanceCode());
        vo.setStatus(entity.getStatus() == null ? null : toStatus(entity.getStatus()));
        vo.setConnectorEnabled(isConnectorEnabled(entity.getConnectorCode()));
        vo.setOwner(entity.getOwner());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private boolean isConnectorEnabled(String connectorCode) {
        if (!StringUtils.hasText(connectorCode)) {
            return false;
        }
        IntegrationConnectorEntity connector = connectorMapper.selectOne(
                new LambdaQueryWrapper<IntegrationConnectorEntity>()
                        .eq(IntegrationConnectorEntity::getConnectorCode, connectorCode)
                        .last("LIMIT 1"));
        return connector != null && connector.getStatus() != null && connector.getStatus() == 1;
    }

    private IntegrationInstanceStatus toStatus(Integer status) {
        return status != null && status == 1
                ? IntegrationInstanceStatus.ENABLED : IntegrationInstanceStatus.DISABLED;
    }
}
