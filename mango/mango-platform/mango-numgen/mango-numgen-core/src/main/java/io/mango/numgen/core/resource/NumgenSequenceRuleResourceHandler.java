package io.mango.numgen.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.numgen.core.entity.NumgenGenerator;
import io.mango.numgen.core.entity.NumgenRule;
import io.mango.numgen.core.entity.NumgenRuleSegment;
import io.mango.numgen.core.mapper.NumgenGeneratorMapper;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.mapper.NumgenRuleSegmentMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 编号规则资源处理器。
 */
@Component
@RequiredArgsConstructor
public class NumgenSequenceRuleResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "numgen_generator";
    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final int PUBLISHED = 1;
    private static final int UNPUBLISHED = 0;
    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final String DEFAULT_DOMAIN_CODE = "NUMGEN";
    private static final String VERSION_STATE_ACTIVE = "ACTIVE";
    private static final String VERSION_STATE_HISTORY = "HISTORY";
    private static final String VERSION_STATE_DRAFT = "DRAFT";

    private final NumgenGeneratorMapper generatorMapper;
    private final NumgenRuleMapper ruleMapper;
    private final NumgenRuleSegmentMapper segmentMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.SEQUENCE_RULE;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("genKey")
                .requiredField("genName")
                .requiredField("ruleName")
                .requiredField("segments")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("generatorId", "编号生成器稳定 ID，可选；不填使用资源 ID。")
                .fieldDescription("ruleId", "编号规则稳定 ID，可选；不填使用 generatorId + 10000。")
                .fieldDescription("genKey", "编号规则业务 Key，租户内唯一。")
                .fieldDescription("genName", "编号生成器名称。")
                .fieldDescription("domainCode", "业务域编码，默认 NUMGEN。")
                .fieldDescription("ruleName", "规则名称。")
                .fieldDescription("ruleVersion", "编号规则版本，默认资源 version。")
                .fieldDescription("status", "状态：1 启用，0 停用。")
                .fieldDescription("publishStatus", "发布状态：1 生效，0 未生效。")
                .fieldDescription("versionState", "版本状态：ACTIVE、DRAFT、HISTORY。")
                .fieldDescription("segments", "规则片段列表，每项支持 id、sortOrder、segmentType、segmentName、literalValue、variableKey、dateFormat、seqWidth、padChar、sequenceScope。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        SequenceRulePayload payload = SequenceRulePayload.from(resource);
        NumgenGenerator generator = upsertGenerator(payload);
        NumgenRule rule = upsertRule(payload);
        replaceSegments(payload, rule);
        if (payload.isActive()) {
            activateVersion(payload, rule, generator);
        } else {
            generator.setCurrentRuleVersion(null);
            generator.setCurrentPublishStatus(UNPUBLISHED);
            generatorMapper.updateById(generator);
        }
        return ResourceSyncResult.of(generator.getId(), TARGET_TABLE,
                "Sequence rule synced: " + payload.genKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        NumgenGenerator generator = resolveGenerator(resource);
        if (generator == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Sequence rule not found");
        }
        generator.setStatus(DISABLED);
        generator.setUpdateTime(LocalDateTime.now());
        generatorMapper.updateById(generator);

        NumgenRule rule = resolveRule(resource, generator);
        if (rule != null) {
            rule.setStatus(DISABLED);
            rule.setPublishStatus(UNPUBLISHED);
            rule.setVersionState(VERSION_STATE_HISTORY);
            rule.setUpdateTime(LocalDateTime.now());
            ruleMapper.updateById(rule);
        }
        return ResourceSyncResult.of(generator.getId(), TARGET_TABLE,
                "Sequence rule disabled: " + generator.getGenKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        NumgenGenerator generator = resolveGenerator(resource);
        if (generator == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Sequence rule not found");
        }
        List<NumgenRule> rules = ruleMapper.selectVersionsByGenKey(generator.getGenKey(), generator.getTenantId());
        for (NumgenRule rule : rules) {
            segmentMapper.physicalDeleteByRuleId(rule.getId());
            ruleMapper.physicalDeleteById(rule.getId());
        }
        generatorMapper.physicalDeleteById(generator.getId());
        return ResourceSyncResult.of(generator.getId(), TARGET_TABLE,
                "Sequence rule deleted: " + generator.getGenKey());
    }

    private NumgenGenerator upsertGenerator(SequenceRulePayload payload) {
        NumgenGenerator generator = generatorMapper.selectByTenantAndGenKeyIncludingDeleted(
                payload.tenantId(), payload.genKey());
        boolean exists = generator != null;
        if (generator == null) {
            generator = new NumgenGenerator();
            generator.setId(payload.generatorId());
            generator.setGenKey(payload.genKey());
            generator.setTenantId(payload.tenantId());
            generator.setCurrentPublishStatus(UNPUBLISHED);
            generator.setCreateTime(LocalDateTime.now());
        } else if (Integer.valueOf(DELETED).equals(generator.getDelFlag())) {
            generatorMapper.physicalDeleteById(generator.getId());
            generator.setId(payload.generatorId());
            exists = false;
        }
        generator.setGenName(payload.genName());
        generator.setDomainCode(payload.domainCode());
        generator.setStatus(payload.status());
        generator.setDelFlag(NOT_DELETED);
        generator.setUpdateTime(LocalDateTime.now());
        if (!exists) {
            generatorMapper.insert(generator);
        } else {
            generatorMapper.updateById(generator);
        }
        return generator;
    }

    private NumgenRule upsertRule(SequenceRulePayload payload) {
        NumgenRule rule = ruleMapper.selectVersionIncludingDeleted(
                payload.tenantId(), payload.genKey(), payload.ruleVersion());
        boolean exists = rule != null;
        if (rule == null) {
            rule = new NumgenRule();
            rule.setId(payload.ruleId());
            rule.setGenKey(payload.genKey());
            rule.setVersion(payload.ruleVersion());
            rule.setTenantId(payload.tenantId());
            rule.setCreateTime(LocalDateTime.now());
        } else if (Integer.valueOf(DELETED).equals(rule.getDelFlag())) {
            segmentMapper.physicalDeleteByRuleId(rule.getId());
            ruleMapper.physicalDeleteById(rule.getId());
            rule.setId(payload.ruleId());
            exists = false;
        }
        rule.setRuleName(payload.ruleName());
        rule.setStatus(payload.status());
        rule.setPublishStatus(payload.publishStatus());
        rule.setVersionState(payload.versionState());
        rule.setDelFlag(NOT_DELETED);
        rule.setUpdateTime(LocalDateTime.now());
        if (!exists) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        return rule;
    }

    private void replaceSegments(SequenceRulePayload payload, NumgenRule rule) {
        segmentMapper.physicalDeleteByRuleId(rule.getId());
        for (SegmentPayload segment : payload.segments()) {
            NumgenRuleSegment entity = new NumgenRuleSegment();
            entity.setId(segment.id());
            entity.setRuleId(rule.getId());
            entity.setSortOrder(segment.sortOrder());
            entity.setSegmentType(segment.segmentType());
            entity.setSegmentName(segment.segmentName());
            entity.setLiteralValue(segment.literalValue());
            entity.setVariableKey(segment.variableKey());
            entity.setDateFormat(segment.dateFormat());
            entity.setSeqWidth(segment.seqWidth());
            entity.setPadChar(segment.padChar());
            entity.setSequenceScope(segment.sequenceScope());
            entity.setTenantId(payload.tenantId());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            segmentMapper.insert(entity);
        }
    }

    private void activateVersion(SequenceRulePayload payload, NumgenRule activeRule, NumgenGenerator generator) {
        List<NumgenRule> versions = ruleMapper.selectVersionsByGenKey(payload.genKey(), payload.tenantId());
        for (NumgenRule version : versions) {
            if (!version.getId().equals(activeRule.getId())) {
                version.setPublishStatus(UNPUBLISHED);
                version.setVersionState(VERSION_STATE_HISTORY);
                version.setUpdateTime(LocalDateTime.now());
                ruleMapper.updateById(version);
            }
        }
        activeRule.setPublishStatus(PUBLISHED);
        activeRule.setVersionState(VERSION_STATE_ACTIVE);
        activeRule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(activeRule);

        generator.setCurrentRuleVersion(activeRule.getVersion());
        generator.setCurrentPublishStatus(PUBLISHED);
        generator.setUpdateTime(LocalDateTime.now());
        generatorMapper.updateById(generator);
    }

    private NumgenGenerator resolveGenerator(ResourceDeclaration resource) {
        Long tenantId = fieldLong(resource, "tenantId", false, DEFAULT_TENANT_ID);
        String genKey = fieldText(resource, "genKey", false);
        if (StringUtils.hasText(genKey)) {
            NumgenGenerator generator = generatorMapper.selectByTenantAndGenKeyIncludingDeleted(tenantId, genKey.trim());
            if (generator != null) {
                return generator;
            }
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        return targetId == null ? null : generatorMapper.selectByIdIncludingDeleted(targetId);
    }

    private NumgenRule resolveRule(ResourceDeclaration resource, NumgenGenerator generator) {
        Integer ruleVersion = fieldInt(resource, "ruleVersion", false, resource.getVersion());
        if (ruleVersion == null) {
            return null;
        }
        return ruleMapper.selectVersionIncludingDeleted(generator.getTenantId(), generator.getGenKey(), ruleVersion);
    }

    private record SequenceRulePayload(Long tenantId, Long generatorId, Long ruleId, String genKey, String genName,
                                       String domainCode, String ruleName, Integer ruleVersion, Integer status,
                                       Integer publishStatus, String versionState, List<SegmentPayload> segments) {

        private static SequenceRulePayload from(ResourceDeclaration resource) {
            Long generatorId = fieldLong(resource, "generatorId", false, Long.valueOf(resource.getId()));
            Integer ruleVersion = fieldInt(resource, "ruleVersion", false, resource.getVersion());
            String versionState = normalizeVersionState(fieldText(resource, "versionState", false));
            Integer publishStatus = fieldInt(resource, "publishStatus", false,
                    VERSION_STATE_ACTIVE.equals(versionState) ? PUBLISHED : UNPUBLISHED);
            return new SequenceRulePayload(
                    fieldLong(resource, "tenantId", false, DEFAULT_TENANT_ID),
                    generatorId,
                    fieldLong(resource, "ruleId", false, generatorId + 10000L),
                    requiredText(resource, "genKey").trim(),
                    requiredText(resource, "genName").trim(),
                    resolveDomainCode(resource),
                    requiredText(resource, "ruleName").trim(),
                    ruleVersion,
                    normalizeStatus(fieldInt(resource, "status", false, ENABLED)),
                    normalizePublishStatus(publishStatus),
                    versionState,
                    fieldList(resource, "segments").stream()
                            .map(SegmentPayload::from)
                            .sorted(Comparator.comparing(SegmentPayload::sortOrder))
                            .toList()
            );
        }

        private boolean isActive() {
            return publishStatus == PUBLISHED && VERSION_STATE_ACTIVE.equals(versionState);
        }
    }

    private static String resolveDomainCode(ResourceDeclaration resource) {
        return defaultText(defaultText(fieldText(resource, "domainCode", false), resource.getModuleCode()),
                DEFAULT_DOMAIN_CODE).toUpperCase();
    }

    private record SegmentPayload(Long id, Integer sortOrder, String segmentType, String segmentName,
                                  String literalValue, String variableKey, String dateFormat,
                                  Integer seqWidth, String padChar, Integer sequenceScope) {

        private static SegmentPayload from(Object value) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalStateException("SEQUENCE_RULE segments must be object list");
            }
            return new SegmentPayload(
                    toLong(map.get("id"), true, null),
                    toInt(map.get("sortOrder"), true, null),
                    requiredText(map.get("segmentType"), "SEQUENCE_RULE segmentType is required"),
                    requiredText(map.get("segmentName"), "SEQUENCE_RULE segmentName is required"),
                    toText(map.get("literalValue")),
                    toText(map.get("variableKey")),
                    toText(map.get("dateFormat")),
                    toInt(map.get("seqWidth"), false, null),
                    defaultText(toText(map.get("padChar")), "0"),
                    normalizeSequenceScope(toInt(map.get("sequenceScope"), false, 0))
            );
        }
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("SEQUENCE_RULE field is required: " + name);
        }
        return value;
    }

    private static String requiredText(ResourceDeclaration resource, String name) {
        return requiredText(fieldValue(resource, name, true), "SEQUENCE_RULE field is required: " + name);
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Integer fieldInt(ResourceDeclaration resource, String name, boolean required,
                                    Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    private static List<?> fieldList(ResourceDeclaration resource, String name) {
        Object value = fieldValue(resource, name, true);
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException("SEQUENCE_RULE field must be list: " + name);
        }
        return list;
    }

    private static String requiredText(Object value, String message) {
        String text = toText(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(message);
        }
        return text;
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("SEQUENCE_RULE long value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer toInt(Object value, boolean required, Integer defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("SEQUENCE_RULE int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static Integer normalizeStatus(Integer status) {
        if (status == null) {
            return ENABLED;
        }
        if (status != ENABLED && status != DISABLED) {
            throw new IllegalStateException("SEQUENCE_RULE status is invalid: " + status);
        }
        return status;
    }

    private static Integer normalizePublishStatus(Integer publishStatus) {
        if (publishStatus == null) {
            return UNPUBLISHED;
        }
        if (publishStatus != PUBLISHED && publishStatus != UNPUBLISHED) {
            throw new IllegalStateException("SEQUENCE_RULE publishStatus is invalid: " + publishStatus);
        }
        return publishStatus;
    }

    private static Integer normalizeSequenceScope(Integer sequenceScope) {
        if (sequenceScope == null) {
            return 0;
        }
        if (sequenceScope != 0 && sequenceScope != 1) {
            throw new IllegalStateException("SEQUENCE_RULE sequenceScope is invalid: " + sequenceScope);
        }
        return sequenceScope;
    }

    private static String normalizeVersionState(String versionState) {
        String resolved = defaultText(versionState, VERSION_STATE_ACTIVE).toUpperCase();
        if (!VERSION_STATE_ACTIVE.equals(resolved)
                && !VERSION_STATE_DRAFT.equals(resolved)
                && !VERSION_STATE_HISTORY.equals(resolved)) {
            throw new IllegalStateException("SEQUENCE_RULE versionState is invalid: " + versionState);
        }
        return resolved;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
