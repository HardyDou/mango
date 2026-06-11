package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCapabilityCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.api.vo.PaymentChannelCertificateRotationRecordVO;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentChannelContractCapabilityVO;
import io.mango.payment.api.vo.PaymentChannelContractVO;
import io.mango.payment.core.entity.PaymentChannelCertificateRotationRecordEntity;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentChannelCapability;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentChannelContractCapability;
import io.mango.payment.core.entity.PaymentChannelContractValueEntity;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.mapper.PaymentChannelCertificateRotationRecordMapper;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelContractValueMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.model.Money;
import io.mango.payment.core.service.IPaymentChannelContractService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentChannelContractServiceImpl implements IPaymentChannelContractService {

    private static final TypeReference<List<Map<String, Object>>> FIELD_TEMPLATE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> CONFIG_VALUES_TYPE = new TypeReference<>() {
    };
    private static final String SECRET_MASK = "******";
    private static final String ENCRYPTED_PREFIX = "enc:";
    private static final Set<String> COMPONENTS = Set.of(
            "input", "textarea", "password", "fileId", "select", "number", "switch", "url", "datetime", "json");
    private static final Set<String> DATA_TYPES = Set.of(
            "string", "number", "boolean", "url", "datetime", "json", "fileId");
    private static final int FEE_RATE_SCALE = 10;
    private static final int DEFAULT_CERTIFICATE_WARNING_DAYS = 30;
    private static final int MAX_CERTIFICATE_WARNING_DAYS = 365;
    private static final String OFFLINE_ACCOUNT_NAME_FIELD = "accountName";
    private static final String OFFLINE_ACCOUNT_NO_FIELD = "accountNo";
    private static final String OFFLINE_BANK_NAME_FIELD = "bankName";

    private final PaymentChannelContractMapper contractMapper;
    private final PaymentEnterpriseSubjectMapper subjectMapper;
    private final PaymentChannelMapper channelMapper;
    private final PaymentChannelCapabilityMapper channelCapabilityMapper;
    private final PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private final PaymentChannelContractValueMapper contractValueMapper;
    private final PaymentChannelCertificateRotationRecordMapper certificateRotationRecordMapper;
    private final PaymentOperationAuditService auditService;
    private final ObjectMapper objectMapper;
    private final ICryptoService cryptoService;

    @Override
    public R<PageResult<PaymentChannelContractVO>> pageChannelContracts(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        IPage<PaymentChannelContract> page = contractMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<PaymentChannelContractVO> records = page.getRecords().stream().map(this::toVO).toList();
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<PaymentChannelContractVO> detailChannelContract(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createChannelContract(SavePaymentChannelContractCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID);
        ValidatedContract validated = validate(command, null);
        PaymentChannelContract entity = new PaymentChannelContract();
        copy(command, entity, validated.channel(), validated.storedConfigValuesJson());
        entity.setTenantId(PaymentContextSupport.currentTenantId());
        contractMapper.insert(entity);
        syncContractValues(entity, validated.fields());
        syncCapabilities(entity, validated.channel(), command.getCapabilities());
        auditService.record(
                PaymentOperationAuditService.ACTION_CREATE_CHANNEL_CONTRACT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateChannelContract(SavePaymentChannelContractCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "签约配置 ID 不能为空");
        PaymentChannelContract entity = selectRequired(command.getId());
        ValidatedContract validated = validate(command, entity);
        copy(command, entity, validated.channel(), validated.storedConfigValuesJson());
        boolean updated = contractMapper.updateById(entity) > 0;
        syncContractValues(entity, validated.fields());
        syncCapabilities(entity, validated.channel(), command.getCapabilities());
        auditService.record(
                PaymentOperationAuditService.ACTION_UPDATE_CHANNEL_CONTRACT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteChannelContract(Long id) {
        PaymentChannelContract entity = selectRequired(id);
        long relationCount = countDeleteRelations(entity);
        if (relationCount > 0) {
            auditService.record(
                    PaymentOperationAuditService.ACTION_DELETE_CHANNEL_CONTRACT,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                    String.valueOf(entity.getId()),
                    PaymentOperationAuditService.RESULT_REJECTED);
        }
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_CHANNEL_CONTRACT_DELETE_HAS_RELATIONS);
        deleteContractValuesPhysically(entity.getId(), entity.getTenantId());
        deleteCapabilitiesPhysically(entity.getId(), entity.getTenantId());
        boolean deleted = contractMapper.deleteById(id) > 0;
        Require.isTrue(deleted, PaymentCode.PAYMENT_CHANNEL_CONTRACT_DELETE_FAILED);
        auditService.record(
                PaymentOperationAuditService.ACTION_DELETE_CHANNEL_CONTRACT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(true);
    }

    @Override
    public R<List<PaymentChannelCertificateExpiryVO>> listExpiringCertificates(Integer warningDays) {
        int days = warningDays == null ? DEFAULT_CERTIFICATE_WARNING_DAYS : warningDays;
        Require.isTrue(days >= 0 && days <= MAX_CERTIFICATE_WARNING_DAYS,
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "证书提醒天数必须在 0 到 365 之间");
        LocalDateTime now = LocalDateTime.now();
        return R.ok(contractCapabilityMapper.selectExpiringCertificates(
                PaymentContextSupport.currentTenantId(), now.plusDays(days), now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<PaymentChannelCertificateRotationRecordVO> rotateCertificate(RotatePaymentChannelContractCertificateCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID);
        Require.notNull(command.getContractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "签约配置 ID 不能为空");
        Require.notNull(command.getContractCapabilityId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力 ID 不能为空");
        Require.notBlank(command.getCertificateFieldCode(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), "证书字段编码不能为空");
        Require.notNull(command.getNewCertificateFileId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), "新证书文件 ID 不能为空");
        Require.isTrue(command.getNewCertificateFileId() > 0, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), "新证书文件只能保存文件 ID");
        Require.notNull(command.getNewCertificateExpireTime(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "证书有效期不能为空");
        Require.isTrue(command.getNewCertificateExpireTime().isAfter(LocalDateTime.now()),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "证书有效期必须晚于当前时间");
        Require.notBlank(command.getRotateReason(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "轮换原因不能为空");
        PaymentChannelContract contract = selectRequired(command.getContractId());
        PaymentChannelContractCapability capability = selectContractCapability(
                command.getContractCapabilityId(), contract.getId(), contract.getTenantId());
        PaymentChannel channel = channelMapper.selectById(contract.getChannelId());
        Require.notNull(channel, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道不存在");
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(channel.getTenantId()),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道不存在");
        FieldDefinition certificateField = selectCertificateFileField(channel, command.getCertificateFieldCode());
        Long oldFileId = findCurrentFileId(contract, certificateField);
        LocalDateTime oldExpireTime = capability.getCertificateExpireTime();
        updateCertificateFileValue(contract, certificateField, command.getNewCertificateFileId());
        capability.setCertificateExpireTime(command.getNewCertificateExpireTime());
        contractCapabilityMapper.updateById(capability);
        PaymentChannelCertificateRotationRecordEntity record = createRotationRecord(command, contract,
                oldFileId, oldExpireTime);
        certificateRotationRecordMapper.insert(record);
        auditService.record(
                PaymentOperationAuditService.ACTION_ROTATE_CHANNEL_CERTIFICATE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_CONTRACT,
                String.valueOf(contract.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(toRotationRecordVO(record));
    }

    private LambdaQueryWrapper<PaymentChannelContract> wrapper(PaymentConfigPageQuery query) {
        String keyword = PaymentContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<PaymentChannelContract>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(PaymentChannelContract::getContractCode, keyword)
                        .or()
                        .like(PaymentChannelContract::getContractName, keyword)
                        .or()
                        .like(PaymentChannelContract::getMerchantNo, keyword))
                .eq(query.getStatus() != null, PaymentChannelContract::getStatus, query.getStatus())
                .eq(PaymentChannelContract::getTenantId, PaymentContextSupport.currentTenantId())
                .orderByDesc(PaymentChannelContract::getUpdatedAt);
    }

    private PaymentChannelContract selectRequired(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "签约配置 ID 不能为空");
        PaymentChannelContract entity = contractMapper.selectById(id);
        Require.notNull(entity, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(entity.getTenantId()), PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        return entity;
    }

    private ValidatedContract validate(SavePaymentChannelContractCommand command, PaymentChannelContract current) {
        Require.notBlank(command.getContractName(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "签约名称不能为空");
        Require.notNull(command.getSubjectId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "企业主体不能为空");
        Require.notNull(command.getChannelId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道不能为空");
        Require.notBlank(command.getMerchantNo(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "商户号不能为空");
        Require.notNull(command.getStatus(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "状态不能为空");
        PaymentEnterpriseSubject subject = subjectMapper.selectById(command.getSubjectId());
        Require.notNull(subject, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(subject.getTenantId()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        PaymentChannel channel = channelMapper.selectById(command.getChannelId());
        Require.notNull(channel, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道不存在");
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(channel.getTenantId()), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道不存在");
        Require.notBlank(channel.getEnvironment(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道路由域不能为空");
        List<FieldDefinition> fields = parseTemplate(channel.getFieldTemplateJson());
        String configValuesJson = configValuesWithDefaults(command.getConfigValuesJson(), offlineCollectionDefaults(channel, subject));
        String storedConfigValuesJson = storeConfigValues(fields, configValuesJson, current == null ? null : current.getConfigValuesJson());
        validateCapabilities(channel, command.getCapabilities());
        return new ValidatedContract(channel, fields, storedConfigValuesJson);
    }

    private Map<String, Object> offlineCollectionDefaults(PaymentChannel channel, PaymentEnterpriseSubject subject) {
        if (!PaymentChannelCode.OFFLINE_COLLECTION.name().equals(channel.getChannelCode())) {
            return Map.of();
        }
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put(OFFLINE_ACCOUNT_NAME_FIELD, subject.getSubjectName());
        defaults.put(OFFLINE_ACCOUNT_NO_FIELD, subject.getBankAccountNo());
        defaults.put(OFFLINE_BANK_NAME_FIELD, subject.getBankName());
        return defaults;
    }

    private String configValuesWithDefaults(String configValuesJson, Map<String, Object> defaults) {
        if (defaults.isEmpty()) {
            return configValuesJson;
        }
        Map<String, Object> values = new LinkedHashMap<>(parseConfigValues(configValuesJson));
        defaults.forEach((key, value) -> {
            if (isBlankValue(values.get(key)) && !isBlankValue(value)) {
                values.put(key, value);
            }
        });
        try {
            return values.isEmpty() ? null : objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            Require.isTrue(false, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID);
            return null;
        }
    }

    private List<FieldDefinition> parseTemplate(String templateJson) {
        String text = PaymentContextSupport.trimToNull(templateJson);
        if (text == null) {
            return List.of();
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(text, FIELD_TEMPLATE_TYPE);
            List<FieldDefinition> fields = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                FieldDefinition field = FieldDefinition.from(row);
                Require.notNull(field, PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID);
                fields.add(field);
            }
            return fields.stream()
                    .sorted(Comparator.comparing(FieldDefinition::sort).thenComparing(FieldDefinition::name))
                    .toList();
        } catch (JsonProcessingException ex) {
            Require.isTrue(false, PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID);
            return List.of();
        }
    }

    private String storeConfigValues(List<FieldDefinition> fields, String configValuesJson, String currentConfigValuesJson) {
        Map<String, Object> input = parseConfigValues(configValuesJson);
        Map<String, Object> current = parseConfigValues(currentConfigValuesJson);
        Map<String, Object> output = new LinkedHashMap<>();
        for (FieldDefinition field : fields) {
            Object raw = input.get(field.name());
            if (isBlankValue(raw)) {
                if (field.required()) {
                    Object previous = current.get(field.name());
                    Require.isTrue(!isBlankValue(previous), PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "不能为空");
                    output.put(field.name(), storedPreviousValue(field, previous));
                }
                continue;
            }
            Object normalized = normalizeConfigValue(field, raw);
            if (SECRET_MASK.equals(String.valueOf(normalized)) && (field.sensitive() || field.encrypted() || field.masked())) {
                Object previous = current.get(field.name());
                Require.isTrue(!isBlankValue(previous), PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "不能为空");
                output.put(field.name(), storedPreviousValue(field, previous));
            } else if (isSecureField(field)) {
                output.put(field.name(), encryptedStorageValue(normalized));
            } else {
                output.put(field.name(), normalized);
            }
        }
        try {
            return output.isEmpty() ? null : objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException ex) {
            Require.isTrue(false, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID);
            return null;
        }
    }

    private Object storedPreviousValue(FieldDefinition field, Object previous) {
        if (!isSecureField(field)) {
            return previous;
        }
        String value = String.valueOf(previous);
        if (value.startsWith(ENCRYPTED_PREFIX)) {
            return value;
        }
        return encryptedStorageValue(previous);
    }

    private String encryptedStorageValue(Object value) {
        String text = String.valueOf(value);
        if (text.startsWith(ENCRYPTED_PREFIX)) {
            return text;
        }
        return ENCRYPTED_PREFIX + cryptoService.encrypt(text);
    }

    private boolean isSecureField(FieldDefinition field) {
        return field.sensitive() || field.encrypted();
    }

    private String encryptedPayload(Object storedValue) {
        String value = String.valueOf(storedValue);
        if (value.startsWith(ENCRYPTED_PREFIX)) {
            return value.substring(ENCRYPTED_PREFIX.length());
        }
        return cryptoService.encrypt(value);
    }

    private Map<String, Object> parseConfigValues(String value) {
        String text = PaymentContextSupport.trimToNull(value);
        if (text == null) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(text, CONFIG_VALUES_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (JsonProcessingException ex) {
            Require.isTrue(false, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID);
            return Map.of();
        }
    }

    private Object normalizeConfigValue(FieldDefinition field, Object raw) {
        return switch (field.dataType()) {
            case "number" -> normalizeNumber(field, raw);
            case "boolean" -> normalizeBoolean(raw);
            case "json" -> normalizeJson(field, raw);
            default -> normalizeText(field, raw);
        };
    }

    private Object normalizeNumber(FieldDefinition field, Object raw) {
        try {
            if (raw instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            throw new io.mango.common.exception.BizException(
                    PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "必须是数字");
        }
    }

    private Object normalizeBoolean(Object raw) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    private Object normalizeJson(FieldDefinition field, Object raw) {
        if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
            return raw;
        }
        try {
            Object parsed = objectMapper.readValue(String.valueOf(raw), Object.class);
            Require.isTrue(parsed instanceof Map<?, ?> || parsed instanceof List<?>, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "必须是 JSON 对象或数组");
            return parsed;
        } catch (JsonProcessingException ex) {
            throw new io.mango.common.exception.BizException(
                    PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "必须是合法 JSON");
        }
    }

    private Object normalizeText(FieldDefinition field, Object raw) {
        String value = String.valueOf(raw).trim();
        if ("url".equals(field.dataType()) || "url".equals(field.component())) {
            Require.isTrue(value.startsWith("http://") || value.startsWith("https://"), PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "必须是 URL");
        }
        if ("datetime".equals(field.dataType()) || "datetime".equals(field.component())) {
            try {
                LocalDateTime.parse(value);
            } catch (DateTimeParseException ex) {
                throw new io.mango.common.exception.BizException(
                        PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "必须是日期时间");
            }
        }
        if ("fileId".equals(field.dataType()) || "fileId".equals(field.component())) {
            Require.isTrue(!value.startsWith("http://") && !value.startsWith("https://"), PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "只能保存文件 ID");
            parseFileId(field, value);
        }
        return value;
    }

    private boolean isBlankValue(Object value) {
        return value == null || !StringUtils.hasText(String.valueOf(value));
    }

    private void validateCapabilities(PaymentChannel channel, List<SavePaymentChannelContractCapabilityCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        Set<Long> capabilityIds = new LinkedHashSet<>();
        for (SavePaymentChannelContractCapabilityCommand command : commands) {
            Require.notNull(command, PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID);
            Require.notNull(command.getChannelCapabilityId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "通道能力不能为空");
            Require.notNull(command.getStatus(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力状态不能为空");
            Require.isTrue(command.getFeeRate() == null || command.getFeeRate().compareTo(BigDecimal.ZERO) >= 0, PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力费率不能小于 0");
            Require.isTrue(command.getFeeRate() == null || command.getFeeRate().scale() <= FEE_RATE_SCALE, PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力费率最多保留 10 位小数");
            Money.requireRange(command.getMinAmount(), command.getMaxAmount(), "签约能力");
            Require.isTrue(capabilityIds.add(command.getChannelCapabilityId()), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "通道能力不能重复");
            PaymentChannelCapability capability = selectChannelCapability(command.getChannelCapabilityId());
            Require.isTrue(Objects.equals(capability.getChannelId(), channel.getId()), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力必须来自当前通道能力");
            Require.isTrue(PaymentContextSupport.currentTenantId().equals(capability.getTenantId()), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID);
        }
    }

    private PaymentChannelCapability selectChannelCapability(Long id) {
        PaymentChannelCapability capability = channelCapabilityMapper.selectById(id);
        Require.notNull(capability, PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "通道能力不存在");
        return capability;
    }

    private PaymentChannelContractCapability selectContractCapability(Long id, Long contractId, Long tenantId) {
        PaymentChannelContractCapability capability = contractCapabilityMapper.selectById(id);
        Require.notNull(capability, PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力不存在");
        Require.isTrue(Objects.equals(capability.getContractId(), contractId)
                        && Objects.equals(capability.getTenantId(), tenantId)
                        && Objects.equals(capability.getDelFlag(), 0),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力不存在");
        return capability;
    }

    private FieldDefinition selectCertificateFileField(PaymentChannel channel, String fieldCode) {
        String normalizedFieldCode = fieldCode.trim();
        return parseTemplate(channel.getFieldTemplateJson()).stream()
                .filter(FieldDefinition::isFileReference)
                .filter(field -> field.name().equals(normalizedFieldCode))
                .findFirst()
                .orElseThrow(() -> new io.mango.common.exception.BizException(
                        PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), "证书字段必须是通道模板中的文件 ID 字段"));
    }

    private void copy(SavePaymentChannelContractCommand command, PaymentChannelContract entity, PaymentChannel channel, String configValuesJson) {
        if (!StringUtils.hasText(entity.getContractCode())) {
            entity.setContractCode(generateContractCode(channel, command.getSubjectId()));
        }
        entity.setContractName(command.getContractName().trim());
        entity.setSubjectId(command.getSubjectId());
        entity.setChannelId(command.getChannelId());
        entity.setEnvironment(routeEnvironment(channel));
        entity.setMerchantNo(command.getMerchantNo().trim());
        entity.setAppId(PaymentContextSupport.trimToNull(command.getAppId()));
        entity.setConfigValuesJson(configValuesJson);
        entity.setEnabledMethodCodes(PaymentContextSupport.trimToNull(command.getEnabledMethodCodes()));
        entity.setStatus(command.getStatus());
    }

    private String routeEnvironment(PaymentChannel channel) {
        String environment = PaymentContextSupport.trimToNull(channel.getEnvironment());
        Require.notBlank(environment, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道路由域不能为空");
        return environment;
    }

    private String generateContractCode(PaymentChannel channel, Long subjectId) {
        String channelCode = normalizeContractCodePart(channel.getChannelCode(), "CHANNEL");
        String subjectPart = subjectId == null ? "SUBJECT" : String.valueOf(subjectId);
        String prefix = channelCode + "_" + subjectPart + "_";
        String id = IdWorker.getIdStr();
        int maxPrefixLength = 64 - id.length();
        if (prefix.length() > maxPrefixLength) {
            prefix = prefix.substring(0, maxPrefixLength);
        }
        return prefix + id;
    }

    private String normalizeContractCodePart(String value, String fallback) {
        String text = PaymentContextSupport.trimToNull(value);
        if (text == null) {
            return fallback;
        }
        String normalized = text.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private void syncContractValues(PaymentChannelContract entity, List<FieldDefinition> fields) {
        Require.notNull(entity.getId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "签约配置 ID 不能为空");
        deleteContractValuesPhysically(entity.getId(), entity.getTenantId());
        Map<String, Object> stored = parseConfigValues(entity.getConfigValuesJson());
        for (FieldDefinition field : fields) {
            Object value = stored.get(field.name());
            if (isBlankValue(value)) {
                continue;
            }
            PaymentChannelContractValueEntity row = new PaymentChannelContractValueEntity();
            row.setContractId(entity.getId());
            row.setTenantId(entity.getTenantId());
            row.setFieldCode(field.name());
            row.setSensitiveFlag(isSecureField(field) ? 1 : 0);
            if (field.isFileReference()) {
                row.setFileId(parseFileId(field, value));
                row.setValueSource("FILE");
            } else if (isSecureField(field)) {
                row.setEncryptedValue(encryptedPayload(value));
                row.setValueSource("CONFIG");
            } else {
                row.setValueText(String.valueOf(value));
                row.setValueSource("CONFIG");
            }
            contractValueMapper.insert(row);
        }
    }

    private Long parseFileId(FieldDefinition field, Object value) {
        try {
            long fileId = Long.parseLong(String.valueOf(value));
            Require.isTrue(fileId > 0, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "只能保存文件 ID");
            return fileId;
        } catch (NumberFormatException ex) {
            throw new io.mango.common.exception.BizException(
                    PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), field.label() + "只能保存文件 ID");
        }
    }

    private Long findCurrentFileId(PaymentChannelContract contract, FieldDefinition certificateField) {
        PaymentChannelContractValueEntity value = contractValueMapper.selectOne(
                new LambdaQueryWrapper<PaymentChannelContractValueEntity>()
                        .eq(PaymentChannelContractValueEntity::getTenantId, contract.getTenantId())
                        .eq(PaymentChannelContractValueEntity::getContractId, contract.getId())
                        .eq(PaymentChannelContractValueEntity::getFieldCode, certificateField.name())
                        .eq(PaymentChannelContractValueEntity::getDelFlag, 0));
        if (value != null) {
            return value.getFileId();
        }
        Object stored = parseConfigValues(contract.getConfigValuesJson()).get(certificateField.name());
        return isBlankValue(stored) ? null : parseFileId(certificateField, stored);
    }

    private void updateCertificateFileValue(
            PaymentChannelContract contract,
            FieldDefinition certificateField,
            Long newCertificateFileId) {
        Map<String, Object> values = new LinkedHashMap<>(parseConfigValues(contract.getConfigValuesJson()));
        values.put(certificateField.name(), String.valueOf(newCertificateFileId));
        try {
            contract.setConfigValuesJson(objectMapper.writeValueAsString(values));
        } catch (JsonProcessingException ex) {
            Require.isTrue(false, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID);
        }
        contractMapper.updateById(contract);
        upsertCertificateFileValue(contract, certificateField, newCertificateFileId);
    }

    private void upsertCertificateFileValue(
            PaymentChannelContract contract,
            FieldDefinition certificateField,
            Long newCertificateFileId) {
        PaymentChannelContractValueEntity value = contractValueMapper.selectOne(
                new LambdaQueryWrapper<PaymentChannelContractValueEntity>()
                        .eq(PaymentChannelContractValueEntity::getTenantId, contract.getTenantId())
                        .eq(PaymentChannelContractValueEntity::getContractId, contract.getId())
                        .eq(PaymentChannelContractValueEntity::getFieldCode, certificateField.name())
                        .eq(PaymentChannelContractValueEntity::getDelFlag, 0));
        if (value == null) {
            value = new PaymentChannelContractValueEntity();
            value.setContractId(contract.getId());
            value.setTenantId(contract.getTenantId());
            value.setFieldCode(certificateField.name());
            value.setValueSource("FILE");
            value.setSensitiveFlag(0);
            value.setFileId(newCertificateFileId);
            contractValueMapper.insert(value);
            return;
        }
        value.setFileId(newCertificateFileId);
        value.setValueSource("FILE");
        value.setValueText(null);
        value.setEncryptedValue(null);
        value.setSensitiveFlag(0);
        contractValueMapper.updateById(value);
    }

    private PaymentChannelCertificateRotationRecordEntity createRotationRecord(
            RotatePaymentChannelContractCertificateCommand command,
            PaymentChannelContract contract,
            Long oldFileId,
            LocalDateTime oldExpireTime) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentChannelCertificateRotationRecordEntity record = new PaymentChannelCertificateRotationRecordEntity();
        record.setContractId(contract.getId());
        record.setContractCapabilityId(command.getContractCapabilityId());
        record.setCertificateFieldCode(command.getCertificateFieldCode().trim());
        record.setOldCertificateFileId(oldFileId);
        record.setNewCertificateFileId(command.getNewCertificateFileId());
        record.setOldCertificateExpireTime(oldExpireTime);
        record.setNewCertificateExpireTime(command.getNewCertificateExpireTime());
        record.setRotateReason(command.getRotateReason().trim());
        record.setOperatorId(operatorId);
        record.setOperatorName(PaymentContextSupport.currentPrincipalName());
        record.setRotateTime(now);
        record.setTenantId(contract.getTenantId());
        record.setCreatedBy(operatorId);
        record.setCreatedAt(now);
        record.setUpdatedBy(operatorId);
        record.setUpdatedAt(now);
        return record;
    }

    private void syncCapabilities(PaymentChannelContract entity, PaymentChannel channel, List<SavePaymentChannelContractCapabilityCommand> commands) {
        List<SavePaymentChannelContractCapabilityCommand> requested = commands == null ? List.of() : commands;
        List<PaymentChannelContractCapability> existingCapabilities = entity.getId() == null ? List.of()
                : contractCapabilityMapper.selectList(new LambdaQueryWrapper<PaymentChannelContractCapability>()
                        .eq(PaymentChannelContractCapability::getTenantId, entity.getTenantId())
                        .eq(PaymentChannelContractCapability::getContractId, entity.getId()));
        Map<Long, PaymentChannelContractCapability> existingRows = existingCapabilities.stream()
                .collect(Collectors.toMap(PaymentChannelContractCapability::getChannelCapabilityId, item -> item, (left, right) -> left));
        Set<Long> requestedCapabilityIds = requested.stream()
                .map(SavePaymentChannelContractCapabilityCommand::getChannelCapabilityId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (PaymentChannelContractCapability existing : existingRows.values()) {
            if (!requestedCapabilityIds.contains(existing.getChannelCapabilityId())) {
                Require.isTrue(countCapabilityRouteRelations(existing.getId(), existing.getTenantId()) == 0,
                        PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力已被路由引用，不能移除");
                contractCapabilityMapper.deletePhysicallyById(existing.getId(), existing.getTenantId());
            }
        }
        for (SavePaymentChannelContractCapabilityCommand command : requested) {
            PaymentChannelCapability channelCapability = selectChannelCapability(command.getChannelCapabilityId());
            Require.isTrue(Objects.equals(channelCapability.getChannelId(), channel.getId()), PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getCode(), "签约能力必须来自当前通道能力");
            PaymentChannelContractCapability capability = existingRows.getOrDefault(channelCapability.getId(), new PaymentChannelContractCapability());
            capability.setContractId(entity.getId());
            capability.setChannelCapabilityId(channelCapability.getId());
            capability.setMethodCode(channelCapability.getMethodCode());
            capability.setTerminalType(channelCapability.getTerminalType());
            capability.setFeeRate(command.getFeeRate());
            capability.setMinAmount(command.getMinAmount() == null ? channelCapability.getMinAmount() : command.getMinAmount());
            capability.setMaxAmount(command.getMaxAmount() == null ? channelCapability.getMaxAmount() : command.getMaxAmount());
            capability.setPriority(command.getPriority() == null ? 100 : command.getPriority());
            capability.setCertificateExpireTime(command.getCertificateExpireTime());
            capability.setStatus(command.getStatus());
            capability.setTenantId(entity.getTenantId());
            if (capability.getId() == null) {
                contractCapabilityMapper.insert(capability);
            } else {
                contractCapabilityMapper.updateById(capability);
            }
        }
    }

    private void deleteCapabilitiesPhysically(Long contractId, Long tenantId) {
        contractCapabilityMapper.deletePhysicallyByContractId(contractId, tenantId);
    }

    private void deleteContractValuesPhysically(Long contractId, Long tenantId) {
        contractValueMapper.deletePhysicallyByContractId(contractId, tenantId);
    }

    private long countDeleteRelations(PaymentChannelContract entity) {
        return contractMapper.countDeleteRelations(entity.getTenantId(), entity.getId());
    }

    private long countCapabilityRouteRelations(Long contractCapabilityId, Long tenantId) {
        return contractCapabilityMapper.countRouteRelations(tenantId, contractCapabilityId);
    }

    private PaymentChannelContractVO toVO(PaymentChannelContract entity) {
        PaymentChannelContractVO vo = new PaymentChannelContractVO();
        vo.setId(entity.getId());
        vo.setContractCode(entity.getContractCode());
        vo.setContractName(entity.getContractName());
        vo.setSubjectId(entity.getSubjectId());
        vo.setChannelId(entity.getChannelId());
        vo.setEnvironment(entity.getEnvironment());
        vo.setMerchantNo(entity.getMerchantNo());
        vo.setAppId(entity.getAppId());
        vo.setConfigValuesJson(maskConfigValues(entity));
        vo.setEnabledMethodCodes(entity.getEnabledMethodCodes());
        vo.setCapabilities(capabilityVOs(entity.getId(), entity.getTenantId()));
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        PaymentEnterpriseSubject subject = subjectMapper.selectById(entity.getSubjectId());
        if (subject != null && PaymentContextSupport.currentTenantId().equals(subject.getTenantId())) {
            vo.setSubjectName(subject.getSubjectName());
        }
        PaymentChannel channel = channelMapper.selectById(entity.getChannelId());
        if (channel != null && PaymentContextSupport.currentTenantId().equals(channel.getTenantId())) {
            vo.setChannelName(channel.getChannelName());
        }
        return vo;
    }

    private String maskConfigValues(PaymentChannelContract entity) {
        PaymentChannel channel = channelMapper.selectById(entity.getChannelId());
        List<FieldDefinition> fields = channel == null ? List.of() : parseTemplate(channel.getFieldTemplateJson());
        Map<String, FieldDefinition> fieldMap = fields.stream().collect(Collectors.toMap(FieldDefinition::name, item -> item, (left, right) -> left));
        Map<String, Object> stored = parseConfigValues(entity.getConfigValuesJson());
        Map<String, Object> masked = new LinkedHashMap<>();
        stored.forEach((key, value) -> {
            FieldDefinition field = fieldMap.get(key);
            if (field != null && (field.sensitive() || field.masked())) {
                masked.put(key, SECRET_MASK);
            } else {
                masked.put(key, value);
            }
        });
        try {
            return masked.isEmpty() ? null : objectMapper.writeValueAsString(masked);
        } catch (JsonProcessingException ex) {
            Require.isTrue(false, PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID);
            return null;
        }
    }

    private List<PaymentChannelContractCapabilityVO> capabilityVOs(Long contractId, Long tenantId) {
        return contractCapabilityMapper.selectList(new LambdaQueryWrapper<PaymentChannelContractCapability>()
                        .eq(PaymentChannelContractCapability::getTenantId, tenantId)
                        .eq(PaymentChannelContractCapability::getContractId, contractId)
                        .orderByAsc(PaymentChannelContractCapability::getPriority))
                .stream()
                .map(this::toCapabilityVO)
                .toList();
    }

    private PaymentChannelContractCapabilityVO toCapabilityVO(PaymentChannelContractCapability entity) {
        PaymentChannelContractCapabilityVO vo = new PaymentChannelContractCapabilityVO();
        vo.setId(entity.getId());
        vo.setChannelCapabilityId(entity.getChannelCapabilityId());
        vo.setMethodCode(entity.getMethodCode());
        PaymentChannelCapabilityVO capability = channelCapabilityMapper.selectChannelCapabilityDetail(
                entity.getTenantId(), entity.getChannelCapabilityId());
        if (capability != null) {
            vo.setMethodName(capability.getMethodName());
        }
        vo.setTerminalType(entity.getTerminalType());
        vo.setFeeRate(entity.getFeeRate());
        vo.setMinAmount(entity.getMinAmount());
        vo.setMaxAmount(entity.getMaxAmount());
        vo.setPriority(entity.getPriority());
        vo.setCertificateExpireTime(entity.getCertificateExpireTime());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private PaymentChannelCertificateRotationRecordVO toRotationRecordVO(
            PaymentChannelCertificateRotationRecordEntity entity) {
        PaymentChannelCertificateRotationRecordVO vo = new PaymentChannelCertificateRotationRecordVO();
        vo.setId(entity.getId());
        vo.setContractId(entity.getContractId());
        vo.setContractCapabilityId(entity.getContractCapabilityId());
        vo.setCertificateFieldCode(entity.getCertificateFieldCode());
        vo.setOldCertificateFileId(entity.getOldCertificateFileId());
        vo.setNewCertificateFileId(entity.getNewCertificateFileId());
        vo.setOldCertificateExpireTime(entity.getOldCertificateExpireTime());
        vo.setNewCertificateExpireTime(entity.getNewCertificateExpireTime());
        vo.setRotateReason(entity.getRotateReason());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setRotateTime(entity.getRotateTime());
        return vo;
    }

    private record ValidatedContract(PaymentChannel channel, List<FieldDefinition> fields, String storedConfigValuesJson) {
    }

    private record FieldDefinition(
            String name,
            String label,
            String component,
            String dataType,
            boolean required,
            boolean sensitive,
            boolean encrypted,
            boolean masked,
            String validationRule,
            Object defaultValue,
            int sort,
            String group) {

        private static FieldDefinition from(Map<String, Object> source) {
            String name = text(source.getOrDefault("name", source.get("code")));
            String label = text(source.getOrDefault("label", source.get("nameLabel")));
            if (!StringUtils.hasText(label)) {
                label = text(source.get("title"));
            }
            String component = component(text(source.get("component")));
            String dataType = dataType(text(source.get("dataType")), component);
            if (!StringUtils.hasText(name) || !StringUtils.hasText(label)) {
                return null;
            }
            return new FieldDefinition(
                    name,
                    label,
                    component,
                    dataType,
                    bool(source.get("required")),
                    bool(source.get("sensitive")),
                    bool(source.get("encrypted")),
                    bool(source.get("masked")),
                    text(source.get("validationRule")),
                    source.get("defaultValue"),
                    integer(source.get("sort")),
                    text(source.get("group")));
        }

        private static String component(String value) {
            if ("file".equals(value)) {
                return "fileId";
            }
            return COMPONENTS.contains(value) ? value : "input";
        }

        private static String dataType(String value, String component) {
            if (StringUtils.hasText(value) && DATA_TYPES.contains(value)) {
                return value;
            }
            return switch (component) {
                case "number" -> "number";
                case "switch" -> "boolean";
                case "url" -> "url";
                case "datetime" -> "datetime";
                case "json" -> "json";
                case "fileId" -> "fileId";
                default -> "string";
            };
        }

        private boolean isFileReference() {
            return "fileId".equals(dataType) || "fileId".equals(component);
        }

        private static String text(Object value) {
            return PaymentContextSupport.trimToNull(value == null ? null : String.valueOf(value));
        }

        private static boolean bool(Object value) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }

        private static int integer(Object value) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            String text = text(value);
            return text == null ? 100 : Integer.parseInt(text);
        }
    }
}
