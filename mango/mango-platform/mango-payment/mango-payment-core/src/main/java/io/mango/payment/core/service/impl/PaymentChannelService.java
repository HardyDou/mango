package io.mango.payment.core.service.impl;

import static io.mango.payment.core.model.PaymentProjectionConverter.toApi;
import static io.mango.payment.core.model.PaymentProjectionConverter.toApiList;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.command.SavePaymentChannelCapabilityCommand;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.enums.PaymentChannelBillFetchModeEnum;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentChannelVO;
import io.mango.payment.core.entity.PaymentChannelEntity;
import io.mango.payment.core.entity.PaymentChannelCapabilityEntity;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.model.Money;
import io.mango.payment.core.service.IPaymentChannelService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentChannelService implements IPaymentChannelService {

    private static final TypeReference<List<Map<String, Object>>> FIELD_TEMPLATE_TYPE = new TypeReference<>() {
    };
    private static final String ROUTE_ENV_MANGO_PAY = "MANGO_PAY";
    private static final String ROUTE_ENV_OFFLINE_COLLECTION = "OFFLINE_COLLECTION";
    private static final String ROUTE_ENV_PROD = "PROD";
    private static final String CHANNEL_PRODUCT_ENV = "CHANNEL_PRODUCT";
    private static final Set<String> CHANNEL_TYPES = Set.of("BUILTIN_VIRTUAL", "BUILTIN_OFFLINE", "AGGREGATOR", "BANK", "DIRECT");
    private static final Set<String> BUILTIN_CHANNEL_CODES = Set.of("MANGO_PAY", "OFFLINE_COLLECTION");
    private static final Set<String> TERMINAL_TYPES = Set.of("WEB", "H5", "APP", "MP");
    private static final Set<String> COMPONENTS = Set.of(
            "input", "textarea", "password", "fileId", "select", "number", "switch", "url", "datetime", "json");
    private static final Set<String> DATA_TYPES = Set.of(
            "string", "number", "boolean", "url", "datetime", "json", "fileId");

    private final PaymentChannelMapper channelMapper;
    private final PaymentChannelCapabilityMapper capabilityMapper;
    private final PaymentOperationAuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<PaymentChannelVO> pageChannels(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        IPage<PaymentChannelEntity> page = channelMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<PaymentChannelVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PaymentChannelVO detailChannel(Long id) {
        PaymentChannelEntity entity = selectRequired(id);
        PaymentChannelVO vo = toVO(entity);
        vo.setCapabilities(capabilities(entity.getId()).stream().map(this::toCapabilityVO).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChannel(SavePaymentChannelCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CHANNEL_INVALID);
        validate(command, false);
        PaymentChannelEntity entity = new PaymentChannelEntity();
        copy(command, entity);
        entity.setTenantId(PaymentContextSupport.currentTenantId());
        channelMapper.insert(entity);
        syncCapabilities(entity, command.getCapabilities());
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_CREATE_CHANNEL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                entity.getChannelCode(),
                PaymentOperationAuditService.RESULT_SUCCESS));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateChannel(SavePaymentChannelCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CHANNEL_INVALID);
        validate(command, true);
        PaymentChannelEntity entity = selectRequired(command.getId());
        copy(command, entity);
        channelMapper.updateById(entity);
        syncCapabilities(entity, command.getCapabilities());
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_UPDATE_CHANNEL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                entity.getChannelCode(),
                PaymentOperationAuditService.RESULT_SUCCESS));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteChannel(Long id) {
        PaymentChannelEntity entity = selectRequired(id);
        if (BUILTIN_CHANNEL_CODES.contains(entity.getChannelCode())) {
            auditService.record(new PaymentOperationAuditService.AuditEntry(
                    PaymentOperationAuditService.ACTION_DELETE_CHANNEL,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                    entity.getChannelCode(),
                    PaymentOperationAuditService.RESULT_REJECTED));
        }
        Require.isTrue(!BUILTIN_CHANNEL_CODES.contains(entity.getChannelCode()), PaymentCode.PAYMENT_CHANNEL_BUILTIN_DELETE_FORBIDDEN);
        long relationCount = countDeleteRelations(entity);
        if (relationCount > 0) {
            auditService.record(new PaymentOperationAuditService.AuditEntry(
                    PaymentOperationAuditService.ACTION_DELETE_CHANNEL,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                    entity.getChannelCode(),
                    PaymentOperationAuditService.RESULT_REJECTED));
        }
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_CHANNEL_DELETE_HAS_RELATIONS);
        for (PaymentChannelCapabilityEntity capability : capabilities(entity.getId())) {
            capabilityMapper.deletePhysicallyById(capability.getId(), entity.getTenantId());
        }
        boolean deleted = channelMapper.deleteById(id) > 0;
        Require.isTrue(deleted, PaymentCode.PAYMENT_CHANNEL_DELETE_FAILED);
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_DELETE_CHANNEL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL,
                entity.getChannelCode(),
                PaymentOperationAuditService.RESULT_SUCCESS));
        return true;
    }

    @Override
    public PageResult<PaymentChannelCapabilityVO> pageChannelCapabilities(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String tenantId = PaymentContextSupport.currentTenantId();
        long total = capabilityMapper.countChannelCapabilities(tenantId, keyword, resolved.getStatus(), resolved.getChannelId());
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentChannelCapabilityVO> rows = toApiList(capabilityMapper.selectChannelCapabilityPage(
                tenantId, keyword, resolved.getStatus(), resolved.getChannelId(), size, (page - 1) * size),
                PaymentChannelCapabilityVO.class);
        return PageResult.of(rows, total, page, size);
    }

    private LambdaQueryWrapper<PaymentChannelEntity> wrapper(PaymentConfigPageQuery query) {
        String keyword = PaymentContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<PaymentChannelEntity>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(PaymentChannelEntity::getChannelName, keyword)
                        .or()
                        .like(PaymentChannelEntity::getChannelCode, keyword)
                .or()
                .like(PaymentChannelEntity::getAdapterType, keyword))
                .eq(query.getStatus() != null, PaymentChannelEntity::getStatus, query.getStatus())
                .eq(PaymentChannelEntity::getTenantId, PaymentContextSupport.currentTenantId())
                .orderByDesc(PaymentChannelEntity::getUpdatedAt);
    }

    private PaymentChannelEntity selectRequired(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_CHANNEL_INVALID, "通道 ID 不能为空");
        PaymentChannelEntity entity = channelMapper.selectById(id);
        Require.notNull(entity, PaymentCode.PAYMENT_CHANNEL_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(entity.getTenantId()), PaymentCode.PAYMENT_CHANNEL_NOT_FOUND);
        return entity;
    }

    private void validate(SavePaymentChannelCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), PaymentCode.PAYMENT_CHANNEL_INVALID, "通道 ID 不能为空");
        }
        Require.notNull(command.getChannelCode(), PaymentCode.PAYMENT_CHANNEL_INVALID, "通道编码不能为空");
        Require.notBlank(command.getChannelName(), PaymentCode.PAYMENT_CHANNEL_INVALID, "通道名称不能为空");
        Require.notBlank(command.getChannelType(), PaymentCode.PAYMENT_CHANNEL_INVALID, "通道类型不能为空");
        Require.isTrue(CHANNEL_TYPES.contains(command.getChannelType().trim()), PaymentCode.PAYMENT_CHANNEL_INVALID, "通道类型不正确");
        Require.notBlank(command.getAdapterType(), PaymentCode.PAYMENT_CHANNEL_INVALID, "适配器类型不能为空");
        Require.notNull(command.getStatus(), PaymentCode.PAYMENT_CHANNEL_INVALID, "状态不能为空");
        validateFieldTemplate(command.getFieldTemplateJson());
        validateBillFetchModes(command.getBillFetchModes());
        validateCapabilities(command.getCapabilities());
    }

    private void validateFieldTemplate(String templateJson) {
        String text = PaymentContextSupport.trimToNull(templateJson);
        if (text == null) {
            return;
        }
        List<Map<String, Object>> fields = parseFieldTemplate(text);
        Set<String> names = new LinkedHashSet<>();
        for (Map<String, Object> field : fields) {
            String name = trimToNull(field.get("name"));
            String label = trimToNull(field.get("label"));
            String component = trimToNull(field.get("component"));
            String dataType = trimToNull(field.get("dataType"));
            Require.notBlank(name, PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, "签约字段编码不能为空");
            Require.notBlank(label, PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, "签约字段名称不能为空");
            Require.isTrue(names.add(name), PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, "签约字段编码不能重复");
            Require.isTrue(COMPONENTS.contains(component), PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, label + "控件类型不正确");
            Require.isTrue(DATA_TYPES.contains(dataType), PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, label + "数据类型不正确");
            if (Boolean.TRUE.equals(field.get("sensitive"))) {
                Require.isTrue(Boolean.TRUE.equals(field.get("encrypted")) || Boolean.TRUE.equals(field.get("masked")),
                        PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, label + "敏感字段必须声明加密或脱敏");
            }
            if ("select".equals(component)) {
                Object options = field.get("options");
                Require.isTrue(options instanceof List<?> list && !list.isEmpty(),
                        PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, label + "枚举字段必须配置选项");
            }
        }
    }

    private List<Map<String, Object>> parseFieldTemplate(String templateJson) {
        try {
            List<Map<String, Object>> fields = objectMapper.readValue(templateJson, FIELD_TEMPLATE_TYPE);
            Require.isTrue(!fields.isEmpty(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID, "签约字段模板不能为空");
            return fields;
        } catch (JsonProcessingException ex) {
            Require.isTrue(false, PaymentCode.PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID);
            return List.of();
        }
    }

    private void validateCapabilities(List<SavePaymentChannelCapabilityCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        for (SavePaymentChannelCapabilityCommand command : commands) {
            Require.notNull(command, PaymentCode.PAYMENT_CHANNEL_INVALID, "通道能力不能为空");
            Require.notBlank(command.getMethodCode(), PaymentCode.PAYMENT_CHANNEL_INVALID, "标准支付方式编码不能为空");
            Require.notBlank(command.getTerminalType(), PaymentCode.PAYMENT_CHANNEL_INVALID, "终端类型不能为空");
            Require.isTrue(TERMINAL_TYPES.contains(command.getTerminalType().trim()), PaymentCode.PAYMENT_CHANNEL_INVALID, "终端类型不正确");
            Require.notNull(command.getStatus(), PaymentCode.PAYMENT_CHANNEL_INVALID, "通道能力状态不能为空");
            Money.requireRange(command.getMinAmount(), command.getMaxAmount(), "通道能力");
            String key = command.getMethodCode().trim() + "|" + command.getTerminalType().trim();
            Require.isTrue(keys.add(key), PaymentCode.PAYMENT_CHANNEL_INVALID, "通道能力不能重复");
        }
    }

    private void validateBillFetchModes(List<String> modes) {
        if (modes == null || modes.isEmpty()) {
            return;
        }
        Set<String> normalizedModes = new LinkedHashSet<>();
        for (String mode : modes) {
            String normalized = normalizeCode(mode);
            Require.notBlank(normalized, PaymentCode.PAYMENT_CHANNEL_INVALID, "账单获取方式不能为空");
            Require.isTrue(PaymentChannelBillFetchModeEnum.contains(normalized),
                    PaymentCode.PAYMENT_CHANNEL_INVALID, "账单获取方式仅支持 MANUAL、FTP、FTPS、HTTP");
            Require.isTrue(normalizedModes.add(normalized),
                    PaymentCode.PAYMENT_CHANNEL_INVALID, "账单获取方式不能重复");
        }
    }

    private void copy(SavePaymentChannelCommand command, PaymentChannelEntity entity) {
        entity.setChannelCode(command.getChannelCode().name());
        entity.setChannelName(command.getChannelName().trim());
        entity.setChannelType(command.getChannelType().trim());
        entity.setAdapterType(command.getAdapterType().trim());
        entity.setGatewayBaseUrl(PaymentContextSupport.trimToNull(command.getGatewayBaseUrl()));
        entity.setFieldTemplateJson(PaymentContextSupport.trimToNull(command.getFieldTemplateJson()));
        entity.setCapabilitySummary(PaymentContextSupport.trimToNull(command.getCapabilitySummary()));
        entity.setBillFetchModes(joinBillFetchModes(command.getBillFetchModes()));
        entity.setEnvironment(routeEnvironment(command.getChannelCode()));
        entity.setStatus(command.getStatus());
    }

    private void syncCapabilities(PaymentChannelEntity channel, List<SavePaymentChannelCapabilityCommand> commands) {
        List<PaymentChannelCapabilityEntity> existing = capabilities(channel.getId());
        Map<Long, PaymentChannelCapabilityEntity> existingById = existing.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(PaymentChannelCapabilityEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Set<Long> retainedIds = new LinkedHashSet<>();
        if (commands != null) {
            for (SavePaymentChannelCapabilityCommand command : commands) {
                PaymentChannelCapabilityEntity capability = command.getId() == null ? null : existingById.get(command.getId());
                if (capability == null) {
                    capability = new PaymentChannelCapabilityEntity();
                    capability.setChannelId(channel.getId());
                    capability.setTenantId(channel.getTenantId());
                    copyCapability(channel, command, capability);
                    capabilityMapper.insert(capability);
                } else {
                    retainedIds.add(capability.getId());
                    copyCapability(channel, command, capability);
                    capabilityMapper.updateById(capability);
                }
                retainedIds.add(capability.getId());
            }
        }
        for (PaymentChannelCapabilityEntity capability : existing) {
            if (!retainedIds.contains(capability.getId())) {
                ensureCapabilityRemovable(capability);
                capabilityMapper.deletePhysicallyById(capability.getId(), channel.getTenantId());
            }
        }
    }

    private void copyCapability(PaymentChannelEntity channel, SavePaymentChannelCapabilityCommand command, PaymentChannelCapabilityEntity capability) {
        capability.setMethodCode(command.getMethodCode().trim());
        capability.setTerminalType(command.getTerminalType().trim());
        capability.setEnvironment(routeEnvironment(channel.getChannelCode()));
        capability.setSupportsRefund(defaultSwitch(command.getSupportsRefund()));
        capability.setSupportsQuery(defaultSwitch(command.getSupportsQuery()));
        capability.setSupportsClose(defaultSwitch(command.getSupportsClose()));
        capability.setSupportsBill(defaultSwitch(command.getSupportsBill()));
        capability.setSupportsReconcile(defaultSwitch(command.getSupportsReconcile()));
        capability.setMinAmount(command.getMinAmount());
        capability.setMaxAmount(command.getMaxAmount());
        capability.setStatus(command.getStatus());
    }

    private int defaultSwitch(Integer value) {
        return value == null ? 1 : value;
    }

    private String routeEnvironment(PaymentChannelCode channelCode) {
        if (PaymentChannelCode.MANGO_PAY == channelCode) {
            return ROUTE_ENV_MANGO_PAY;
        }
        if (PaymentChannelCode.OFFLINE_COLLECTION == channelCode) {
            return ROUTE_ENV_OFFLINE_COLLECTION;
        }
        return ROUTE_ENV_PROD;
    }

    private String routeEnvironment(String channelCode) {
        if (!StringUtils.hasText(channelCode)) {
            return CHANNEL_PRODUCT_ENV;
        }
        try {
            return routeEnvironment(PaymentChannelCode.valueOf(channelCode));
        } catch (IllegalArgumentException ex) {
            return CHANNEL_PRODUCT_ENV;
        }
    }

    private void ensureCapabilityRemovable(PaymentChannelCapabilityEntity capability) {
        long relationCount = capabilityMapper.countDeleteRelations(capability.getTenantId(), capability.getId());
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_CHANNEL_DELETE_HAS_RELATIONS, "通道能力存在签约或路由引用，不能删除");
    }

    private List<PaymentChannelCapabilityEntity> capabilities(Long channelId) {
        return capabilityMapper.selectList(new LambdaQueryWrapper<PaymentChannelCapabilityEntity>()
                .eq(PaymentChannelCapabilityEntity::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentChannelCapabilityEntity::getChannelId, channelId)
                .orderByAsc(PaymentChannelCapabilityEntity::getMethodCode)
                .orderByAsc(PaymentChannelCapabilityEntity::getTerminalType)
                .orderByAsc(PaymentChannelCapabilityEntity::getEnvironment));
    }

    private long countDeleteRelations(PaymentChannelEntity entity) {
        return channelMapper.countDeleteRelations(entity.getTenantId(), entity.getId(), entity.getChannelCode());
    }

    private PaymentChannelVO toVO(PaymentChannelEntity entity) {
        PaymentChannelVO vo = new PaymentChannelVO();
        vo.setId(entity.getId());
        vo.setChannelCode(PaymentChannelCode.valueOf(entity.getChannelCode()));
        vo.setChannelName(entity.getChannelName());
        vo.setChannelType(entity.getChannelType());
        vo.setAdapterType(entity.getAdapterType());
        vo.setGatewayBaseUrl(entity.getGatewayBaseUrl());
        vo.setFieldTemplateJson(entity.getFieldTemplateJson());
        vo.setCapabilitySummary(entity.getCapabilitySummary());
        vo.setBillFetchModes(splitBillFetchModes(entity.getBillFetchModes()));
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private PaymentChannelCapabilityVO toCapabilityVO(PaymentChannelCapabilityEntity entity) {
        PaymentChannelCapabilityVO vo = new PaymentChannelCapabilityVO();
        vo.setId(entity.getId());
        vo.setChannelId(entity.getChannelId());
        vo.setMethodCode(entity.getMethodCode());
        vo.setTerminalType(entity.getTerminalType());
        vo.setEnvironment(entity.getEnvironment());
        vo.setSupportsRefund(entity.getSupportsRefund());
        vo.setSupportsQuery(entity.getSupportsQuery());
        vo.setSupportsClose(entity.getSupportsClose());
        vo.setSupportsBill(entity.getSupportsBill());
        vo.setSupportsReconcile(entity.getSupportsReconcile());
        vo.setMinAmount(entity.getMinAmount());
        vo.setMaxAmount(entity.getMaxAmount());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private String trimToNull(Object value) {
        return PaymentContextSupport.trimToNull(value == null ? null : String.valueOf(value));
    }

    private String normalizeCode(String value) {
        String text = PaymentContextSupport.trimToNull(value);
        return text == null ? null : text.toUpperCase();
    }

    private String joinBillFetchModes(List<String> modes) {
        if (modes == null || modes.isEmpty()) {
            return null;
        }
        return modes.stream()
                .map(this::normalizeCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<String> splitBillFetchModes(String modes) {
        String text = PaymentContextSupport.trimToNull(modes);
        if (text == null) {
            return List.of();
        }
        return List.of(text.split(",")).stream()
                .map(this::normalizeCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
