package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleItemCommand;
import io.mango.payment.api.query.PaymentMethodRoutePageQuery;
import io.mango.payment.api.vo.PaymentMethodRouteRuleItemVO;
import io.mango.payment.api.vo.PaymentMethodRouteRuleVO;
import io.mango.payment.api.vo.PaymentMethodRouteTrialVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentChannelContractCapability;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentMethodRouteRule;
import io.mango.payment.core.entity.PaymentMethodRouteRuleItem;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleItemMapper;
import io.mango.payment.core.mapper.PaymentMethodRouteRuleMapper;
import io.mango.payment.core.model.Money;
import io.mango.payment.core.model.PaymentMethodRouteCandidate;
import io.mango.payment.core.service.IPaymentMethodRouteService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentMethodRouteServiceImpl implements IPaymentMethodRouteService {

    private static final Set<Integer> STATUS_VALUES = Set.of(0, 1);
    private static final Set<String> TERMINAL_TYPES = Set.of("WEB", "H5");
    private static final Set<String> ROUTE_MODES = Set.of("PRIORITY", "MANUAL", "WEIGHT", "COST", "HEALTH");

    private final PaymentMethodRouteRuleMapper routeRuleMapper;
    private final PaymentMethodRouteRuleItemMapper routeRuleItemMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentEnterpriseSubjectMapper subjectMapper;
    private final PaymentMethodMapper methodMapper;
    private final PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private final PaymentOperationAuditService auditService;

    @Override
    public R<PageResult<PaymentMethodRouteRuleVO>> pageRouteRules(PaymentMethodRoutePageQuery query) {
        PaymentMethodRoutePageQuery resolved = query == null ? new PaymentMethodRoutePageQuery() : query;
        IPage<PaymentMethodRouteRule> page = routeRuleMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<PaymentMethodRouteRuleVO> records = page.getRecords().stream()
                .map(entity -> toVO(entity, false))
                .toList();
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<PaymentMethodRouteRuleVO> detailRouteRule(Long id) {
        return R.ok(toVO(selectRequired(id), true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createRouteRule(SavePaymentMethodRouteRuleCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_METHOD_ROUTE_INVALID);
        validate(command, false);
        PaymentMethodRouteRule entity = new PaymentMethodRouteRule();
        copy(command, entity);
        entity.setTenantId(PaymentContextSupport.currentTenantId());
        routeRuleMapper.insert(entity);
        saveItems(entity, command.getItems());
        auditService.record(
                PaymentOperationAuditService.ACTION_CREATE_METHOD_ROUTE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                entity.getRuleCode(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateRouteRule(SavePaymentMethodRouteRuleCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_METHOD_ROUTE_INVALID);
        validate(command, true);
        PaymentMethodRouteRule entity = selectRequired(command.getId());
        copy(command, entity);
        boolean updated = routeRuleMapper.updateById(entity) > 0;
        routeRuleItemMapper.deleteByRuleId(entity.getId(), entity.getTenantId());
        saveItems(entity, command.getItems());
        auditService.record(
                PaymentOperationAuditService.ACTION_UPDATE_METHOD_ROUTE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                entity.getRuleCode(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteRouteRule(Long id) {
        PaymentMethodRouteRule entity = selectRequired(id);
        long relationCount = routeRuleMapper.countDeleteRelations(entity.getTenantId(), entity.getId());
        if (relationCount > 0) {
            auditService.record(
                    PaymentOperationAuditService.ACTION_DELETE_METHOD_ROUTE,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                    entity.getRuleCode(),
                    PaymentOperationAuditService.RESULT_REJECTED);
        }
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_METHOD_ROUTE_DELETE_HAS_RELATIONS);
        routeRuleItemMapper.deleteByRuleId(entity.getId(), entity.getTenantId());
        boolean deleted = routeRuleMapper.deleteById(entity.getId()) > 0;
        Require.isTrue(deleted, PaymentCode.PAYMENT_METHOD_ROUTE_DELETE_FAILED);
        auditService.record(
                PaymentOperationAuditService.ACTION_DELETE_METHOD_ROUTE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                entity.getRuleCode(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(true);
    }

    @Override
    public R<PaymentMethodRouteTrialVO> trialRoute(PaymentMethodRouteTrialCommand command) {
        validateTrial(command);
        Long tenantId = PaymentContextSupport.currentTenantId();
        List<PaymentMethodRouteCandidate> candidates = routeRuleMapper.selectRouteCandidates(
                tenantId,
                command.getApplicationId(),
                command.getSubjectId(),
                command.getMethodCode().trim(),
                command.getTerminalType().trim(),
                command.getEnvironment().trim());
        PaymentMethodRouteTrialVO result = new PaymentMethodRouteTrialVO();
        for (PaymentMethodRouteCandidate candidate : candidates) {
            String reason = filterReason(candidate, command.getAmount());
            if (reason == null) {
                result.setMatched(true);
                result.setMatchedRule(toRuleVO(candidate));
                result.setMatchedItem(toItemVO(candidate));
                auditService.record(
                        PaymentOperationAuditService.ACTION_TRIAL_METHOD_ROUTE,
                        PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                        candidate.getRuleCode(),
                        PaymentOperationAuditService.RESULT_SUCCESS);
                return R.ok(result);
            }
            result.getFilterReasons().add(reason);
        }
        result.setMatched(false);
        if (candidates.isEmpty()) {
            result.getFilterReasons().add("未找到启用的路由规则或路由明细");
        }
        auditService.record(
                PaymentOperationAuditService.ACTION_TRIAL_METHOD_ROUTE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                command.getMethodCode().trim(),
                PaymentOperationAuditService.RESULT_REJECTED);
        return R.ok(result);
    }

    private LambdaQueryWrapper<PaymentMethodRouteRule> wrapper(PaymentMethodRoutePageQuery query) {
        String keyword = PaymentContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<PaymentMethodRouteRule>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(PaymentMethodRouteRule::getRuleCode, keyword)
                        .or()
                        .like(PaymentMethodRouteRule::getRuleName, keyword)
                        .or()
                        .like(PaymentMethodRouteRule::getMethodCode, keyword))
                .eq(query.getApplicationId() != null, PaymentMethodRouteRule::getAppId, query.getApplicationId())
                .eq(query.getSubjectId() != null, PaymentMethodRouteRule::getSubjectId, query.getSubjectId())
                .eq(StringUtils.hasText(query.getMethodCode()), PaymentMethodRouteRule::getMethodCode, PaymentContextSupport.trimToNull(query.getMethodCode()))
                .eq(StringUtils.hasText(query.getTerminalType()), PaymentMethodRouteRule::getTerminalType, PaymentContextSupport.trimToNull(query.getTerminalType()))
                .eq(StringUtils.hasText(query.getEnvironment()), PaymentMethodRouteRule::getEnvironment, PaymentContextSupport.trimToNull(query.getEnvironment()))
                .eq(query.getStatus() != null, PaymentMethodRouteRule::getStatus, query.getStatus())
                .eq(PaymentMethodRouteRule::getTenantId, PaymentContextSupport.currentTenantId())
                .orderByDesc(PaymentMethodRouteRule::getUpdatedAt)
                .orderByDesc(PaymentMethodRouteRule::getId);
    }

    private PaymentMethodRouteRule selectRequired(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由规则 ID 不能为空");
        PaymentMethodRouteRule entity = routeRuleMapper.selectById(id);
        Require.notNull(entity, PaymentCode.PAYMENT_METHOD_ROUTE_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(entity.getTenantId()), PaymentCode.PAYMENT_METHOD_ROUTE_NOT_FOUND);
        return entity;
    }

    private void validate(SavePaymentMethodRouteRuleCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由规则 ID 不能为空");
        }
        Require.notBlank(command.getRuleCode(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由规则编码不能为空");
        Require.notBlank(command.getRuleName(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由规则名称不能为空");
        Require.notBlank(command.getMethodCode(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "标准支付方式不能为空");
        Require.notBlank(command.getTerminalType(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "终端类型不能为空");
        Require.notBlank(command.getEnvironment(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "接入场景不能为空");
        Require.notBlank(command.getRouteMode(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由模式不能为空");
        Require.notNull(command.getFallbackEnabled(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "失败降级开关不能为空");
        Require.notNull(command.getStatus(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "状态不能为空");
        Require.isTrue(TERMINAL_TYPES.contains(command.getTerminalType().trim()), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "终端类型仅支持 WEB、H5");
        Require.isTrue(ROUTE_MODES.contains(command.getRouteMode().trim()), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由模式不正确");
        Require.isTrue(STATUS_VALUES.contains(command.getFallbackEnabled()), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "失败降级开关不正确");
        Require.isTrue(STATUS_VALUES.contains(command.getStatus()), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "状态不正确");
        Require.isTrue(command.getItems() != null && !command.getItems().isEmpty(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由明细不能为空");
        validateApplication(command.getAppId());
        validateSubject(command.getSubjectId());
        validateMethod(command.getMethodCode());
        validateUniqueRuleCode(command, update);
        validateItems(command);
    }

    private void validateTrial(PaymentMethodRouteTrialCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_METHOD_ROUTE_INVALID);
        Require.notNull(command.getApplicationId(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "应用不能为空");
        Require.notNull(command.getSubjectId(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "企业主体不能为空");
        Require.notBlank(command.getMethodCode(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "标准支付方式不能为空");
        Require.notBlank(command.getTerminalType(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "终端类型不能为空");
        Require.notBlank(command.getEnvironment(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "接入场景不能为空");
        Money.cents(command.getAmount()).toPositiveCents("试算金额");
        Require.isTrue(TERMINAL_TYPES.contains(command.getTerminalType().trim()), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "终端类型仅支持 WEB、H5");
        validateApplication(command.getApplicationId());
        validateSubject(command.getSubjectId());
        validateMethod(command.getMethodCode());
    }

    private void validateApplication(Long appId) {
        if (appId == null) {
            return;
        }
        PaymentApplication application = applicationMapper.selectById(appId);
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(application.getTenantId()), PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
    }

    private void validateSubject(Long subjectId) {
        if (subjectId == null) {
            return;
        }
        PaymentEnterpriseSubject subject = subjectMapper.selectById(subjectId);
        Require.notNull(subject, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(subject.getTenantId()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
    }

    private void validateMethod(String methodCode) {
        PaymentMethod method = methodMapper.selectOne(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethod::getMethodCode, methodCode.trim())
                .last("limit 1"));
        Require.notNull(method, PaymentCode.PAYMENT_METHOD_NOT_FOUND);
    }

    private void validateUniqueRuleCode(SavePaymentMethodRouteRuleCommand command, boolean update) {
        LambdaQueryWrapper<PaymentMethodRouteRule> wrapper = new LambdaQueryWrapper<PaymentMethodRouteRule>()
                .eq(PaymentMethodRouteRule::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethodRouteRule::getRuleCode, command.getRuleCode().trim());
        if (update) {
            wrapper.ne(PaymentMethodRouteRule::getId, command.getId());
        }
        Long count = routeRuleMapper.selectCount(wrapper);
        Require.isTrue(count == null || count == 0L, PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由规则编码不能重复");
    }

    private void validateItems(SavePaymentMethodRouteRuleCommand command) {
        Set<Long> capabilityIds = new HashSet<>();
        for (SavePaymentMethodRouteRuleItemCommand item : command.getItems()) {
            Require.notNull(item, PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由明细不能为空");
            Require.notNull(item.getContractCapabilityId(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "签约能力不能为空");
            Require.notNull(item.getStatus(), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由明细状态不能为空");
            Require.isTrue(STATUS_VALUES.contains(item.getStatus()), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "路由明细状态不正确");
            Require.isTrue(capabilityIds.add(item.getContractCapabilityId()), PaymentCode.PAYMENT_METHOD_ROUTE_INVALID.getCode(), "同一路由规则不能重复选择签约能力");
            Money.requireRange(item.getMinAmount(), item.getMaxAmount(), "路由明细");
            PaymentChannelContractCapability capability = contractCapabilityMapper.selectRouteCapability(
                    PaymentContextSupport.currentTenantId(),
                    item.getContractCapabilityId(),
                    command.getMethodCode().trim(),
                    command.getTerminalType().trim(),
                    command.getEnvironment().trim());
            Require.notNull(capability, PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID);
        }
    }

    private void copy(SavePaymentMethodRouteRuleCommand command, PaymentMethodRouteRule entity) {
        entity.setRuleCode(command.getRuleCode().trim());
        entity.setRuleName(command.getRuleName().trim());
        entity.setAppId(command.getAppId());
        entity.setSubjectId(command.getSubjectId());
        entity.setMethodCode(command.getMethodCode().trim());
        entity.setTerminalType(command.getTerminalType().trim());
        entity.setEnvironment(command.getEnvironment().trim());
        entity.setRouteMode(command.getRouteMode().trim());
        entity.setFallbackEnabled(command.getFallbackEnabled());
        entity.setStatus(command.getStatus());
    }

    private void saveItems(PaymentMethodRouteRule rule, List<SavePaymentMethodRouteRuleItemCommand> items) {
        for (SavePaymentMethodRouteRuleItemCommand command : items) {
            PaymentMethodRouteRuleItem item = new PaymentMethodRouteRuleItem();
            item.setRuleId(rule.getId());
            item.setContractCapabilityId(command.getContractCapabilityId());
            item.setPriority(command.getPriority() == null ? 100 : command.getPriority());
            item.setWeight(command.getWeight() == null ? 100 : command.getWeight());
            item.setMinAmount(command.getMinAmount());
            item.setMaxAmount(command.getMaxAmount());
            item.setStatus(command.getStatus());
            item.setTenantId(rule.getTenantId());
            routeRuleItemMapper.insert(item);
        }
    }

    private PaymentMethodRouteRuleVO toVO(PaymentMethodRouteRule entity, boolean withItems) {
        PaymentMethodRouteRuleVO vo = new PaymentMethodRouteRuleVO();
        vo.setId(entity.getId());
        vo.setRuleCode(entity.getRuleCode());
        vo.setRuleName(entity.getRuleName());
        vo.setAppId(entity.getAppId());
        vo.setSubjectId(entity.getSubjectId());
        vo.setMethodCode(entity.getMethodCode());
        vo.setTerminalType(entity.getTerminalType());
        vo.setEnvironment(entity.getEnvironment());
        vo.setRouteMode(entity.getRouteMode());
        vo.setFallbackEnabled(entity.getFallbackEnabled());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        if (entity.getAppId() != null) {
            PaymentApplication application = applicationMapper.selectById(entity.getAppId());
            vo.setAppName(application == null ? null : application.getAppName());
        }
        if (entity.getSubjectId() != null) {
            PaymentEnterpriseSubject subject = subjectMapper.selectById(entity.getSubjectId());
            vo.setSubjectName(subject == null ? null : subject.getSubjectName());
        }
        PaymentMethod method = methodMapper.selectOne(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getTenantId, entity.getTenantId())
                .eq(PaymentMethod::getMethodCode, entity.getMethodCode())
                .last("limit 1"));
        vo.setMethodName(method == null ? null : method.getMethodName());
        if (withItems) {
            vo.setItems(routeRuleItemMapper.selectItemsByRuleId(entity.getId(), entity.getTenantId()));
        }
        return vo;
    }

    private PaymentMethodRouteRuleVO toRuleVO(PaymentMethodRouteCandidate candidate) {
        PaymentMethodRouteRuleVO vo = new PaymentMethodRouteRuleVO();
        vo.setId(candidate.getRouteRuleId());
        vo.setRuleCode(candidate.getRuleCode());
        vo.setRuleName(candidate.getRuleName());
        vo.setAppId(candidate.getAppId());
        vo.setAppName(candidate.getAppName());
        vo.setSubjectId(candidate.getSubjectId());
        vo.setSubjectName(candidate.getSubjectName());
        vo.setMethodCode(candidate.getMethodCode());
        vo.setMethodName(candidate.getMethodName());
        vo.setTerminalType(candidate.getTerminalType());
        vo.setEnvironment(candidate.getEnvironment());
        vo.setRouteMode(candidate.getRouteMode());
        vo.setFallbackEnabled(candidate.getFallbackEnabled());
        vo.setStatus(1);
        return vo;
    }

    private PaymentMethodRouteRuleItemVO toItemVO(PaymentMethodRouteCandidate candidate) {
        PaymentMethodRouteRuleItemVO vo = new PaymentMethodRouteRuleItemVO();
        vo.setId(candidate.getRouteItemId());
        vo.setRuleId(candidate.getRouteRuleId());
        vo.setContractCapabilityId(candidate.getContractCapabilityId());
        vo.setContractId(candidate.getContractId());
        vo.setContractName(candidate.getContractName());
        vo.setChannelId(candidate.getChannelId());
        vo.setChannelName(candidate.getChannelName());
        vo.setMethodCode(candidate.getMethodCode());
        vo.setTerminalType(candidate.getTerminalType());
        vo.setPriority(candidate.getItemPriority());
        vo.setWeight(candidate.getItemWeight());
        vo.setMinAmount(candidate.getItemMinAmount());
        vo.setMaxAmount(candidate.getItemMaxAmount());
        vo.setStatus(candidate.getItemStatus());
        return vo;
    }

    private String filterReason(PaymentMethodRouteCandidate candidate, Long amount) {
        if (!Integer.valueOf(1).equals(candidate.getItemStatus())) {
            return candidate.getRuleName() + " / " + candidate.getContractName() + " 路由明细已停用";
        }
        if (!Integer.valueOf(1).equals(candidate.getCapabilityStatus())) {
            return candidate.getRuleName() + " / " + candidate.getContractName() + " 签约能力已停用";
        }
        if (!Integer.valueOf(1).equals(candidate.getContractStatus())) {
            return candidate.getRuleName() + " / " + candidate.getContractName() + " 签约配置已停用";
        }
        if (!Integer.valueOf(1).equals(candidate.getChannelStatus())) {
            return candidate.getRuleName() + " / " + candidate.getChannelName() + " 支付通道已停用";
        }
        if (candidate.getItemMinAmount() != null && candidate.getItemMinAmount() > amount) {
            return candidate.getRuleName() + " / " + candidate.getContractName() + " 低于路由明细最小金额";
        }
        if (candidate.getItemMaxAmount() != null && candidate.getItemMaxAmount() < amount) {
            return candidate.getRuleName() + " / " + candidate.getContractName() + " 超过路由明细最大金额";
        }
        if (candidate.getCapabilityMinAmount() != null && candidate.getCapabilityMinAmount() > amount) {
            return candidate.getRuleName() + " / " + candidate.getContractName() + " 低于签约能力最小金额";
        }
        if (candidate.getCapabilityMaxAmount() != null && candidate.getCapabilityMaxAmount() < amount) {
            return candidate.getRuleName() + " / " + candidate.getContractName() + " 超过签约能力最大金额";
        }
        return null;
    }
}
