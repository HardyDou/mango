package io.mango.integration.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.integration.api.enums.IntegrationCode;
import io.mango.integration.api.query.IntegrationConnectorPageQuery;
import io.mango.integration.api.vo.IntegrationConnectorVO;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import io.mango.integration.core.entity.IntegrationInstanceEntity;
import io.mango.integration.core.mapper.IntegrationConnectorMapper;
import io.mango.integration.core.mapper.IntegrationInstanceMapper;
import io.mango.integration.core.service.IIntegrationConnectorService;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 集成连接器定义服务实现。
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects managed mappers"))
public class IntegrationConnectorService implements IIntegrationConnectorService {

    private final IntegrationConnectorMapper connectorMapper;
    private final IntegrationInstanceMapper instanceMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<IntegrationConnectorVO> page(IntegrationConnectorPageQuery query) {
        IntegrationConnectorPageQuery validated = Require.nonNull(
                query, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器分页查询条件不能为空");
        LambdaQueryWrapper<IntegrationConnectorEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(validated.getCategory()),
                        IntegrationConnectorEntity::getCategory, validated.getCategory())
                .and(StringUtils.hasText(validated.getKeyword()), condition -> condition
                        .like(IntegrationConnectorEntity::getConnectorName, validated.getKeyword())
                        .or().like(IntegrationConnectorEntity::getConnectorCode, validated.getKeyword()))
                .orderByAsc(IntegrationConnectorEntity::getConnectorCode);
        Page<IntegrationConnectorEntity> page = connectorMapper.selectPage(
                Page.of(validated.getPage(), validated.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public IntegrationConnectorVO detail(String connectorCode) {
        return toVO(require(connectorCode));
    }

    @Override
    public boolean isEnabled(IntegrationConnectorEntity connector) {
        return connector.getStatus() != null && connector.getStatus() == 1;
    }

    @Override
    public IntegrationConnectorEntity require(String connectorCode) {
        Require.notBlank(connectorCode, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器编码不能为空");
        IntegrationConnectorEntity connector = connectorMapper.selectOne(
                new LambdaQueryWrapper<IntegrationConnectorEntity>()
                        .eq(IntegrationConnectorEntity::getConnectorCode, connectorCode.trim())
                        .last("LIMIT 1"));
        Require.notNull(connector, IntegrationCode.INTEGRATION_NOT_FOUND, "连接器定义不存在");
        return connector;
    }

    @Override
    public Boolean save(IntegrationConnectorCommand command) {
        Require.notNull(command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器保存命令不能为空");
        return save(command, false);
    }

    @Override
    public Boolean saveDeclaration(IntegrationConnectorCommand command) {
        Require.notNull(command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器保存命令不能为空");
        return save(command, true);
    }

    private Boolean save(IntegrationConnectorCommand command, boolean declared) {
        IntegrationConnectorCommand validated = Require.nonNull(
                command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器保存命令不能为空");
        Integer status = validated.getStatus() == null ? Integer.valueOf(1) : validated.getStatus();
        Require.isTrue(status == 0 || status == 1,
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器状态只能为0或1");
        validateJsonObject(validated.getConfigSchema(), "配置规范必须是JSON对象");
        validateJsonObject(validated.getAuthSchema(), "认证规范必须是JSON对象");
        validateJsonArray(validated.getCapabilitiesJson(), "能力清单必须是JSON数组");
        IntegrationConnectorEntity existing = findByCode(validated.getConnectorCode());
        boolean creating = existing == null;
        if (!declared && existing != null) {
            Require.isTrue(!"DECLARED".equals(existing.getSource()),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR,
                    "该连接器由资源声明维护，请在声明文件中修改");
        }
        IntegrationConnectorEntity entity = creating
                ? new IntegrationConnectorEntity() : existing;
        if (creating) {
            entity.setConnectorCode(validated.getConnectorCode().trim());
            String tenantId = MangoContextHolder.tenantId();
            entity.setTenantId(StringUtils.hasText(tenantId) ? tenantId : "default");
            entity.setSource(declared ? "DECLARED" : "MANUAL");
        }
        if (declared) {
            entity.setSource("DECLARED");
        }
        entity.setConnectorName(validated.getConnectorName().trim());
        entity.setCategory(emptyToNull(validated.getCategory()));
        entity.setProvider(validated.getProvider());
        entity.setStatus(status);
        entity.setConfigSchema(emptyToNull(validated.getConfigSchema()));
        entity.setAuthSchema(emptyToNull(validated.getAuthSchema()));
        entity.setCapabilitiesJson(emptyToNull(validated.getCapabilitiesJson()));
        entity.setTags(joinTags(validated.getTags()));
        entity.setDocsUrl(validated.getDocsUrl());
        if (creating) {
            Require.isTrue(connectorMapper.insert(entity) == 1,
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器保存失败");
        } else {
            int expected = entity.getRevision() == null ? 0 : entity.getRevision();
            entity.setRevision(expected + 1);
            Require.isTrue(connectorMapper.update(entity,
                            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<IntegrationConnectorEntity>()
                                    .eq(IntegrationConnectorEntity::getId, entity.getId())
                                    .eq(IntegrationConnectorEntity::getRevision, expected)) == 1,
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器已被他人修改，请刷新后重试");
        }
        return true;
    }

    private IntegrationConnectorEntity findByCode(String connectorCode) {
        return connectorMapper.selectOne(new LambdaQueryWrapper<IntegrationConnectorEntity>()
                .eq(IntegrationConnectorEntity::getConnectorCode, connectorCode.trim())
                .last("LIMIT 1"));
    }

    private void validateJsonObject(String json, String message) {
        if (!StringUtils.hasText(json)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            Require.isTrue(node != null && node.isObject(),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, message);
        } catch (JsonProcessingException exception) {
            Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, message);
        }
    }

    private void validateJsonArray(String json, String message) {
        if (!StringUtils.hasText(json)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            Require.isTrue(node != null && node.isArray(),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, message);
        } catch (JsonProcessingException exception) {
            Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, message);
        }
    }

    @Override
    public Long findIdByCode(String connectorCode) {
        return require(connectorCode).getId();
    }

    @Override
    public Boolean updateStatusByCode(String connectorCode, Integer status) {
        Require.isTrue(status != null && (status == 0 || status == 1),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器状态只能为0或1");
        IntegrationConnectorEntity entity = require(connectorCode);
        int expected = entity.getRevision() == null ? 0 : entity.getRevision();
        entity.setStatus(status);
        entity.setRevision(expected + 1);
        Require.isTrue(connectorMapper.update(entity,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<IntegrationConnectorEntity>()
                                .eq(IntegrationConnectorEntity::getId, entity.getId())
                                .eq(IntegrationConnectorEntity::getRevision, expected)) == 1,
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器已被他人修改，请刷新后重试");
        return true;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String joinTags(java.util.List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        String joined = tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return joined.isBlank() ? null : joined;
    }

    private java.util.List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private IntegrationConnectorVO toVO(IntegrationConnectorEntity entity) {
        IntegrationConnectorVO vo = new IntegrationConnectorVO();
        vo.setConnectorCode(entity.getConnectorCode());
        vo.setConnectorName(entity.getConnectorName());
        vo.setCategory(entity.getCategory());
        vo.setProvider(entity.getProvider());
        vo.setStatus(entity.getStatus());
        vo.setSource(entity.getSource());
        vo.setConfigSchema(entity.getConfigSchema());
        vo.setAuthSchema(entity.getAuthSchema());
        vo.setCapabilitiesJson(entity.getCapabilitiesJson());
        vo.setTags(splitTags(entity.getTags()));
        vo.setDocsUrl(entity.getDocsUrl());
        Long count = instanceMapper.selectCount(new LambdaQueryWrapper<IntegrationInstanceEntity>()
                .eq(IntegrationInstanceEntity::getConnectorCode, entity.getConnectorCode()));
        vo.setInstanceCount(count == null ? Long.valueOf(0L) : count);
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
