package io.mango.integration.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.integration.api.command.IntegrationCredentialCommand;
import io.mango.integration.api.command.IntegrationCredentialRotateCommand;
import io.mango.integration.api.enums.IntegrationCode;
import io.mango.integration.api.enums.IntegrationCredentialStatus;
import io.mango.integration.api.vo.IntegrationCredentialVO;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import io.mango.integration.core.entity.IntegrationCredentialEntity;
import io.mango.integration.core.entity.IntegrationCredentialAccessLogEntity;
import io.mango.integration.core.entity.IntegrationCredentialRotationEntity;
import io.mango.integration.core.mapper.IntegrationCredentialAccessLogMapper;
import io.mango.integration.core.mapper.IntegrationCredentialMapper;
import io.mango.integration.core.mapper.IntegrationCredentialRotationMapper;
import io.mango.integration.core.service.IIntegrationConnectorService;
import io.mango.integration.core.service.IIntegrationCredentialService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.crypto.impl.ICryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 集成凭证服务实现。
 *
 * <p>敏感字段按连接器认证规范加密保存，版本按字段递增，读取一律返回掩码。</p>
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects managed mapper, crypto and object mapper"))
public class IntegrationCredentialService implements IIntegrationCredentialService {

    private static final String ENCRYPTED_PREFIX = "enc:";
    private static final String MASK = "******";
    private static final String TYPE_SECRET = "SECRET";

    private final IntegrationCredentialMapper credentialMapper;
    private final IntegrationCredentialRotationMapper rotationMapper;
    private final IntegrationCredentialAccessLogMapper accessLogMapper;
    private final IIntegrationConnectorService connectorService;
    private final ICryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Override
    public List<IntegrationCredentialVO> list(String connectorCode) {
        LambdaQueryWrapper<IntegrationCredentialEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IntegrationCredentialEntity::getTenantId, currentTenantId())
                .eq(StringUtils.hasText(connectorCode),
                        IntegrationCredentialEntity::getConnectorCode, connectorCode)
                .orderByDesc(IntegrationCredentialEntity::getUpdatedAt);
        return credentialMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean save(IntegrationCredentialCommand command) {
        IntegrationCredentialCommand validated = Require.nonNull(
                command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证保存命令不能为空");
        IntegrationConnectorEntity connector =
                connectorService.require(validated.getConnectorCode());
        Map<String, FieldSpec> specs = authFieldSpecs(connector);
        Map<String, String> incoming = parseFields(validated.getFieldsJson());
        validateFields(specs, incoming, true);
        IntegrationCredentialEntity existing = findCredential(validated.getCredentialCode());
        boolean creating = existing == null;
        IntegrationCredentialEntity entity = creating
                ? new IntegrationCredentialEntity() : existing;
        if (creating) {
            entity.setTenantId(currentTenantId());
            entity.setCredentialCode(validated.getCredentialCode().trim());
        }
        entity.setConnectorCode(connector.getConnectorCode());
        Map<String, String> stored = creating ? new LinkedHashMap<>() : parseStoredFields(entity.getFieldsJson());
        Map<String, Integer> versions = creating
                ? new LinkedHashMap<>() : parseVersions(entity.getFieldVersionsJson());
        for (Map.Entry<String, String> entry : incoming.entrySet()) {
            FieldSpec spec = specs.get(entry.getKey());
            stored.put(entry.getKey(), encryptIfNeeded(spec, entry.getValue()));
            versions.put(entry.getKey(), nextVersion(versions.get(entry.getKey())));
        }
        entity.setFieldsJson(writeJson(stored));
        entity.setFieldVersionsJson(writeJson(versions));
        entity.setRotatedAt(LocalDateTime.now());
        entity.setExpiresAt(validated.getExpiresAt());
        entity.setStatus(IntegrationCredentialStatus.ACTIVE.name());
        entity.setRemark(validated.getRemark());
        if (creating) {
            Require.isTrue(credentialMapper.insert(entity) == 1,
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证保存失败");
            recordRotation(entity, incoming.keySet());
        } else {
            Require.isTrue(updateWithRevision(entity),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证已被他人修改，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rotate(IntegrationCredentialRotateCommand command) {
        IntegrationCredentialRotateCommand validated = Require.nonNull(
                command, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证轮换命令不能为空");
        IntegrationCredentialEntity entity = findCredential(validated.getCredentialCode());
        Require.notNull(entity, IntegrationCode.INTEGRATION_NOT_FOUND, "凭证不存在");
        IntegrationConnectorEntity connector =
                connectorService.require(entity.getConnectorCode());
        Map<String, FieldSpec> specs = authFieldSpecs(connector);
        Map<String, String> incoming = parseFields(validated.getFieldsJson());
        validateFields(specs, incoming, false);
        Map<String, String> stored = parseStoredFields(entity.getFieldsJson());
        Map<String, Integer> versions = parseVersions(entity.getFieldVersionsJson());
        for (Map.Entry<String, String> entry : incoming.entrySet()) {
            FieldSpec spec = specs.get(entry.getKey());
            stored.put(entry.getKey(), encryptIfNeeded(spec, entry.getValue()));
            versions.put(entry.getKey(), nextVersion(versions.get(entry.getKey())));
        }
        entity.setFieldsJson(writeJson(stored));
        entity.setFieldVersionsJson(writeJson(versions));
        entity.setRotatedAt(LocalDateTime.now());
        entity.setStatus(IntegrationCredentialStatus.ACTIVE.name());
        Require.isTrue(updateWithRevision(entity),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证已被他人修改，请刷新后重试");
        recordRotation(entity, incoming.keySet());
        return true;
    }

    @Override
    public Map<String, String> resolve(String credentialCode) {
        IntegrationCredentialEntity entity = findCredential(credentialCode);
        Require.notNull(entity, IntegrationCode.INTEGRATION_NOT_FOUND, "凭证不存在");
        Require.isTrue(IntegrationCredentialStatus.ACTIVE.name().equals(entity.getStatus()),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证不可用");
        Require.isTrue(entity.getExpiresAt() == null || entity.getExpiresAt().isAfter(LocalDateTime.now()),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证已过期");
        Map<String, String> stored = parseStoredFields(entity.getFieldsJson());
        Map<String, String> resolved = new LinkedHashMap<>();
        stored.forEach((key, value) -> resolved.put(key, decryptIfNeeded(value)));
        recordAccess(entity, stored.keySet());
        return resolved;
    }

    private boolean updateWithRevision(IntegrationCredentialEntity entity) {
        int expected = entity.getRevision() == null ? 0 : entity.getRevision();
        entity.setRevision(expected + 1);
        return credentialMapper.update(entity,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<IntegrationCredentialEntity>()
                        .eq(IntegrationCredentialEntity::getId, entity.getId())
                        .eq(IntegrationCredentialEntity::getRevision, expected)) == 1;
    }

    private IntegrationCredentialEntity findCredential(String credentialCode) {
        if (!StringUtils.hasText(credentialCode)) {
            return null;
        }
        return credentialMapper.selectOne(new LambdaQueryWrapper<IntegrationCredentialEntity>()
                .eq(IntegrationCredentialEntity::getTenantId, currentTenantId())
                .eq(IntegrationCredentialEntity::getCredentialCode, credentialCode.trim())
                .last("LIMIT 1"));
    }

    private Map<String, FieldSpec> authFieldSpecs(IntegrationConnectorEntity connector) {
        Map<String, FieldSpec> specs = new LinkedHashMap<>();
        JsonNode root = readJson(connector.getAuthSchema());
        Require.notNull(root, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器缺少认证规范");
        JsonNode fields = root.get("fields");
        Require.isTrue(fields != null && fields.isArray(),
                IntegrationCode.INTEGRATION_BUSINESS_ERROR, "连接器认证规范缺少字段声明");
        for (JsonNode field : Objects.requireNonNull(fields)) {
            String code = field.path("fieldCode").asText(null);
            if (code == null || code.isBlank()) {
                continue;
            }
            specs.put(code, new FieldSpec(code, field.path("type").asText("STRING"),
                    field.path("required").asBoolean(false)));
        }
        return specs;
    }

    private void validateFields(Map<String, FieldSpec> specs, Map<String, String> values, boolean requireAll) {
        for (String key : values.keySet()) {
            Require.isTrue(specs.containsKey(key),
                    IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证包含未声明的字段：" + key);
        }
        if (!requireAll) {
            return;
        }
        for (FieldSpec spec : specs.values()) {
            if (spec.required()) {
                Require.isTrue(StringUtils.hasText(values.get(spec.code())),
                        IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证缺少必填字段：" + spec.code());
            }
        }
    }

    private String encryptIfNeeded(FieldSpec spec, String value) {
        if (value == null) {
            return null;
        }
        if (spec == null || !TYPE_SECRET.equalsIgnoreCase(spec.type())) {
            return value;
        }
        return ENCRYPTED_PREFIX + cryptoService.encrypt(value);
    }

    private String decryptIfNeeded(String value) {
        if (value == null) {
            return null;
        }
        return value.startsWith(ENCRYPTED_PREFIX)
                ? cryptoService.decrypt(value.substring(ENCRYPTED_PREFIX.length())) : value;
    }

    private Map<String, String> parseFields(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(json)) {
            return result;
        }
        JsonNode root = readJson(json);
        Require.notNull(root, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证字段必须是合法JSON对象");
        Require.isTrue(root.isObject(), IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证字段必须是JSON对象");
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            result.put(entry.getKey(), entry.getValue().asText());
        }
        return result;
    }

    private Map<String, String> parseStoredFields(String json) {
        return parseFields(json);
    }

    private Map<String, Integer> parseVersions(String json) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(json)) {
            return result;
        }
        JsonNode root = readJson(json);
        if (root == null || !root.isObject()) {
            return result;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            result.put(entry.getKey(), entry.getValue().asInt(1));
        }
        return result;
    }

    private int nextVersion(Integer current) {
        return current == null ? 1 : current + 1;
    }

    private String writeJson(Map<String, ?> map) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            map.forEach((key, value) -> node.set(key, objectMapper.valueToTree(value)));
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            Require.isTrue(false, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证字段序列化失败");
            return null;
        }
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

    private void recordRotation(IntegrationCredentialEntity entity, java.util.Collection<String> fieldCodes) {
        IntegrationCredentialRotationEntity record = new IntegrationCredentialRotationEntity();
        record.setTenantId(currentTenantId());
        record.setCredentialCode(entity.getCredentialCode());
        record.setVersion(entity.getFieldVersionsJson() == null ? 1 : maxVersion(entity.getFieldVersionsJson()));
        record.setFieldCodes(String.join(",", fieldCodes));
        record.setOperatorId(MangoContextHolder.userId());
        rotationMapper.insert(record);
    }

    private void recordAccess(IntegrationCredentialEntity entity, java.util.Collection<String> fieldCodes) {
        IntegrationCredentialAccessLogEntity log = new IntegrationCredentialAccessLogEntity();
        log.setTenantId(currentTenantId());
        log.setCredentialCode(entity.getCredentialCode());
        log.setAction("READ");
        log.setFieldCodes(String.join(",", fieldCodes));
        log.setOperatorId(MangoContextHolder.userId());
        log.setSource("IntegrationDeclarationProvider");
        accessLogMapper.insert(log);
    }

    private int maxVersion(String fieldVersionsJson) {
        Map<String, Integer> versions = parseVersions(fieldVersionsJson);
        return versions.values().stream().max(Integer::compareTo).orElse(1);
    }

    @Override
    public java.util.List<io.mango.integration.api.vo.IntegrationCredentialRotationVO> rotations(String credentialCode) {
        Require.notBlank(credentialCode, IntegrationCode.INTEGRATION_BUSINESS_ERROR, "凭证编码不能为空");
        return rotationMapper.selectList(new LambdaQueryWrapper<IntegrationCredentialRotationEntity>()
                        .eq(IntegrationCredentialRotationEntity::getTenantId, currentTenantId())
                        .eq(IntegrationCredentialRotationEntity::getCredentialCode, credentialCode.trim())
                        .orderByDesc(IntegrationCredentialRotationEntity::getCreatedAt))
                .stream()
                .map(record -> {
                    io.mango.integration.api.vo.IntegrationCredentialRotationVO vo =
                            new io.mango.integration.api.vo.IntegrationCredentialRotationVO();
                    vo.setCredentialCode(record.getCredentialCode());
                    vo.setVersion(record.getVersion());
                    vo.setFieldCodes(record.getFieldCodes());
                    vo.setOperatorId(record.getOperatorId());
                    vo.setRemark(record.getRemark());
                    vo.setRotatedAt(record.getCreatedAt());
                    return vo;
                })
                .toList();
    }

    private IntegrationCredentialVO toVO(IntegrationCredentialEntity entity) {
        IntegrationCredentialVO vo = new IntegrationCredentialVO();
        vo.setCredentialCode(entity.getCredentialCode());
        vo.setConnectorCode(entity.getConnectorCode());
        vo.setMaskedFieldsJson(maskFields(entity));
        vo.setFieldVersionsJson(entity.getFieldVersionsJson());
        vo.setRotatedAt(entity.getRotatedAt());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setStatus(effectiveStatus(entity));
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private IntegrationCredentialStatus effectiveStatus(IntegrationCredentialEntity entity) {
        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            return IntegrationCredentialStatus.EXPIRED;
        }
        return entity.getStatus() == null
                ? IntegrationCredentialStatus.ACTIVE : IntegrationCredentialStatus.valueOf(entity.getStatus());
    }

    private String maskFields(IntegrationCredentialEntity entity) {
        Map<String, String> stored = parseStoredFields(entity.getFieldsJson());
        Set<String> maskedTypes = maskedFieldTypes(entity.getConnectorCode());
        Set<String> masked = new LinkedHashSet<>();
        stored.forEach((key, value) -> {
            boolean sensitive = value != null && value.startsWith(ENCRYPTED_PREFIX);
            boolean maskedType = maskedTypes.contains(key);
            masked.add(sensitive || maskedType ? key + "=" + MASK : key + "=" + value);
        });
        return String.join(",", masked);
    }

    private Set<String> maskedFieldTypes(String connectorCode) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(connectorCode)) {
            return result;
        }
        IntegrationConnectorEntity connector =
                connectorService.require(connectorCode);
        JsonNode root = readJson(connector.getAuthSchema());
        JsonNode fields = root == null ? null : root.get("fields");
        if (fields == null || !fields.isArray()) {
            return result;
        }
        for (JsonNode field : fields) {
            String type = field.path("type").asText("STRING");
            if (TYPE_SECRET.equalsIgnoreCase(type) || "CERT".equalsIgnoreCase(type)
                    || "FILE".equalsIgnoreCase(type) || "PRIVATE_KEY".equalsIgnoreCase(type)) {
                result.add(field.path("fieldCode").asText());
            }
        }
        return result;
    }

    private List<String> fieldCodes(String fieldsJson) {
        return new ArrayList<>(parseFields(fieldsJson).keySet());
    }

    private record FieldSpec(String code, String type, boolean required) {
    }
}
