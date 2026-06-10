package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentCashierConfigVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentCashierConfig;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.service.IPaymentCashierConfigService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentCashierConfigServiceImpl implements IPaymentCashierConfigService {

    private static final Set<Integer> SWITCH_VALUES = Set.of(0, 1);

    private final PaymentCashierConfigMapper cashierConfigMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentEnterpriseSubjectMapper enterpriseSubjectMapper;
    private final PaymentMethodMapper methodMapper;
    private final PaymentOperationAuditService auditService;

    @Override
    public R<PageResult<PaymentCashierConfigVO>> pageCashierConfigs(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        IPage<PaymentCashierConfig> page = cashierConfigMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<PaymentCashierConfigVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<PaymentCashierConfigVO> detailCashierConfig(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createCashierConfig(SavePaymentCashierConfigCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID);
        validate(command, false);
        PaymentCashierConfig entity = new PaymentCashierConfig();
        copy(command, entity);
        entity.setTenantId(PaymentContextSupport.currentTenantId());
        cashierConfigMapper.insert(entity);
        auditService.record(
                PaymentOperationAuditService.ACTION_CREATE_CASHIER_CONFIG,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CASHIER_CONFIG,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateCashierConfig(SavePaymentCashierConfigCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID);
        validate(command, true);
        PaymentCashierConfig entity = selectRequired(command.getId());
        copy(command, entity);
        boolean updated = cashierConfigMapper.updateById(entity) > 0;
        auditService.record(
                PaymentOperationAuditService.ACTION_UPDATE_CASHIER_CONFIG,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CASHIER_CONFIG,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteCashierConfig(Long id) {
        PaymentCashierConfig entity = selectRequired(id);
        long relationCount = countDeleteRelations(entity);
        if (relationCount > 0) {
            auditService.record(
                    PaymentOperationAuditService.ACTION_DELETE_CASHIER_CONFIG,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_CASHIER_CONFIG,
                    String.valueOf(entity.getId()),
                    PaymentOperationAuditService.RESULT_REJECTED);
        }
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_CASHIER_CONFIG_DELETE_HAS_RELATIONS);
        boolean deleted = cashierConfigMapper.deleteById(id) > 0;
        Require.isTrue(deleted, PaymentCode.PAYMENT_CASHIER_CONFIG_DELETE_FAILED);
        auditService.record(
                PaymentOperationAuditService.ACTION_DELETE_CASHIER_CONFIG,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CASHIER_CONFIG,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(true);
    }

    private LambdaQueryWrapper<PaymentCashierConfig> wrapper(PaymentConfigPageQuery query) {
        String keyword = PaymentContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<PaymentCashierConfig>()
                .like(StringUtils.hasText(keyword), PaymentCashierConfig::getCashierName, keyword)
                .eq(query.getStatus() != null, PaymentCashierConfig::getStatus, query.getStatus())
                .eq(PaymentCashierConfig::getTenantId, PaymentContextSupport.currentTenantId())
                .orderByDesc(PaymentCashierConfig::getUpdatedAt);
    }

    private PaymentCashierConfig selectRequired(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台配置 ID 不能为空");
        PaymentCashierConfig entity = cashierConfigMapper.selectById(id);
        Require.notNull(entity, PaymentCode.PAYMENT_CASHIER_CONFIG_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(entity.getTenantId()), PaymentCode.PAYMENT_CASHIER_CONFIG_NOT_FOUND);
        return entity;
    }

    private void validate(SavePaymentCashierConfigCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台配置 ID 不能为空");
        }
        Require.notBlank(command.getCashierName(), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台名称不能为空");
        Require.notNull(command.getApplicationId(), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "应用 ID 不能为空");
        Require.notNull(command.getDefaultCashier(), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "默认收银台标记不能为空");
        Require.isTrue(SWITCH_VALUES.contains(command.getDefaultCashier()), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "默认收银台标记不正确");
        PaymentApplication application = applicationMapper.selectById(command.getApplicationId());
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(application.getTenantId()), PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        parseIds(command.getEnterpriseSubjectIds(), "允许企业主体不能为空").forEach(this::requireSubject);
        validateMethods(command.getMethodCodes(), command.getDefaultMethodCode(), command.getMethodDisplayOrder());
        validateDefaultCashier(command);
        Require.notNull(command.getStatus(), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "状态不能为空");
        Require.isTrue(SWITCH_VALUES.contains(command.getStatus()), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "状态不正确");
    }

    private void validateDefaultCashier(SavePaymentCashierConfigCommand command) {
        if (!Integer.valueOf(1).equals(command.getDefaultCashier()) || !Integer.valueOf(1).equals(command.getStatus())) {
            return;
        }
        Long count = cashierConfigMapper.selectCount(new LambdaQueryWrapper<PaymentCashierConfig>()
                .eq(PaymentCashierConfig::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentCashierConfig::getApplicationId, command.getApplicationId())
                .eq(PaymentCashierConfig::getDefaultCashier, 1)
                .eq(PaymentCashierConfig::getStatus, 1)
                .ne(command.getId() != null, PaymentCashierConfig::getId, command.getId()));
        Require.isTrue(count == null || count == 0, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "同一应用只能启用一个默认收银台");
    }

    private void copy(SavePaymentCashierConfigCommand command, PaymentCashierConfig entity) {
        entity.setCashierName(command.getCashierName().trim());
        entity.setApplicationId(command.getApplicationId());
        entity.setDefaultCashier(command.getDefaultCashier());
        entity.setEnterpriseSubjectIds(joinIds(parseIds(command.getEnterpriseSubjectIds(), "允许企业主体不能为空")));
        entity.setMethodCodes(PaymentContextSupport.trimToNull(command.getMethodCodes()));
        entity.setDefaultMethodCode(PaymentContextSupport.trimToNull(command.getDefaultMethodCode()));
        entity.setMethodDisplayOrder(PaymentContextSupport.trimToNull(command.getMethodDisplayOrder()));
        entity.setResultReturnUrl(PaymentContextSupport.trimToNull(command.getResultReturnUrl()));
        entity.setDisplayConfig(PaymentContextSupport.trimToNull(command.getDisplayConfig()));
        entity.setStatus(command.getStatus());
    }

    private PaymentCashierConfigVO toVO(PaymentCashierConfig entity) {
        PaymentCashierConfigVO vo = new PaymentCashierConfigVO();
        vo.setId(entity.getId());
        vo.setCashierName(entity.getCashierName());
        vo.setApplicationId(entity.getApplicationId());
        vo.setApplicationName(applicationName(entity.getApplicationId()));
        vo.setDefaultCashier(entity.getDefaultCashier());
        vo.setEnterpriseSubjectIds(entity.getEnterpriseSubjectIds());
        vo.setEnterpriseSubjectNames(subjectNames(entity.getEnterpriseSubjectIds()));
        vo.setMethodCodes(entity.getMethodCodes());
        vo.setMethodNames(methodNames(entity.getMethodCodes()));
        vo.setDefaultMethodCode(entity.getDefaultMethodCode());
        vo.setDefaultMethodName(methodName(entity.getDefaultMethodCode()));
        vo.setResultReturnUrl(entity.getResultReturnUrl());
        vo.setDisplayConfig(entity.getDisplayConfig());
        vo.setCashierPath("/payment/cashier-configs/" + entity.getId() + "/cashier");
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private void validateMethods(String methodCodes, String defaultMethodCode, String methodDisplayOrder) {
        Require.notBlank(methodCodes, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "可见支付方式不能为空");
        List<String> visibleMethodCodes = parseMethodCodes(methodCodes);
        Require.isTrue(!visibleMethodCodes.isEmpty(), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "可见支付方式不能为空");
        visibleMethodCodes.forEach(this::requireMethod);
        String normalizedDefault = PaymentContextSupport.trimToNull(defaultMethodCode);
        if (normalizedDefault != null) {
            requireMethod(normalizedDefault);
            Require.isTrue(visibleMethodCodes.contains(normalizedDefault), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "默认支付方式必须包含在可见支付方式中");
        }
        List<String> displayOrder = parseMethodCodes(methodDisplayOrder);
        Require.isTrue(displayOrder.stream().allMatch(visibleMethodCodes::contains), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "支付方式展示顺序必须包含在可见支付方式中");
    }

    private PaymentMethod requireMethod(String methodCode) {
        PaymentMethod method = methodMapper.selectOne(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethod::getMethodCode, methodCode));
        Require.notNull(method, PaymentCode.PAYMENT_METHOD_NOT_FOUND);
        return method;
    }

    private List<String> parseMethodCodes(String methodCodes) {
        String normalized = PaymentContextSupport.trimToNull(methodCodes);
        if (normalized == null) {
            return List.of();
        }
        return java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private List<Long> parseIds(String ids, String emptyMessage) {
        String normalized = PaymentContextSupport.trimToNull(ids);
        Require.notBlank(normalized, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), emptyMessage);
        List<String> idTexts = java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
        Require.isTrue(idTexts.stream().allMatch(item -> item.matches("\\d+")), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "企业主体 ID 格式不正确");
        return idTexts.stream().map(Long::valueOf).toList();
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private PaymentEnterpriseSubject requireSubject(Long subjectId) {
        PaymentEnterpriseSubject subject = enterpriseSubjectMapper.selectById(subjectId);
        Require.notNull(subject, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(subject.getTenantId()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        return subject;
    }

    private long countDeleteRelations(PaymentCashierConfig entity) {
        return cashierConfigMapper.countDeleteRelations(entity.getTenantId(), entity.getId());
    }

    private String applicationName(Long applicationId) {
        PaymentApplication application = applicationMapper.selectById(applicationId);
        return application == null || !PaymentContextSupport.currentTenantId().equals(application.getTenantId()) ? null : application.getAppName();
    }

    private String subjectNames(String subjectIds) {
        String normalized = PaymentContextSupport.trimToNull(subjectIds);
        if (normalized == null) {
            return null;
        }
        return java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .map(this::subjectName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    private String subjectName(Long subjectId) {
        PaymentEnterpriseSubject subject = enterpriseSubjectMapper.selectById(subjectId);
        return subject == null || !PaymentContextSupport.currentTenantId().equals(subject.getTenantId()) ? null : subject.getSubjectName();
    }

    private String methodName(String methodCode) {
        if (methodCode == null) {
            return null;
        }
        PaymentMethod method = methodMapper.selectOne(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethod::getMethodCode, methodCode));
        return method == null ? null : method.getMethodName();
    }

    private String methodNames(String methodCodes) {
        return parseMethodCodes(methodCodes).stream()
                .map(this::methodName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }
}
