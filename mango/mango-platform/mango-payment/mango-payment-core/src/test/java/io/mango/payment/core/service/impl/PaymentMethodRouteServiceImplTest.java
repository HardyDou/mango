package io.mango.payment.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleItemCommand;
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
import io.mango.payment.core.model.PaymentMethodRouteCandidate;
import io.mango.payment.core.service.PaymentOperationAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMethodRouteServiceImplTest {

    private PaymentMethodRouteRuleMapper routeRuleMapper;
    private PaymentMethodRouteRuleItemMapper routeRuleItemMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentEnterpriseSubjectMapper subjectMapper;
    private PaymentMethodMapper methodMapper;
    private PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private PaymentOperationAuditService auditService;
    private PaymentMethodRouteServiceImpl service;

    @BeforeEach
    void setUp() {
        routeRuleMapper = mock(PaymentMethodRouteRuleMapper.class);
        routeRuleItemMapper = mock(PaymentMethodRouteRuleItemMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        subjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        methodMapper = mock(PaymentMethodMapper.class);
        contractCapabilityMapper = mock(PaymentChannelContractCapabilityMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        service = new PaymentMethodRouteServiceImpl(
                routeRuleMapper,
                routeRuleItemMapper,
                applicationMapper,
                subjectMapper,
                methodMapper,
                contractCapabilityMapper,
                auditService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(methodMapper.selectOne(any())).thenReturn(method());
        when(contractCapabilityMapper.selectRouteCapability(1L, 333001L, "PERSONAL_WECHAT_QR", "WEB", "MANGO_PAY")).thenReturn(capability());
        when(contractCapabilityMapper.selectRouteEnvironment(1L, 333001L, "PERSONAL_WECHAT_QR", "WEB")).thenReturn("MANGO_PAY");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("createRouteRule should save rule items and audit success")
    void createRouteRule_validCommand_savesRuleItemsAndAudits() {
        when(routeRuleMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<PaymentMethodRouteRule> ruleCaptor = ArgumentCaptor.forClass(PaymentMethodRouteRule.class);
        ArgumentCaptor<PaymentMethodRouteRuleItem> itemCaptor = ArgumentCaptor.forClass(PaymentMethodRouteRuleItem.class);

        service.createRouteRule(command());

        verify(routeRuleMapper).insert(ruleCaptor.capture());
        PaymentMethodRouteRule rule = ruleCaptor.getValue();
        assertThat(rule.getTenantId()).isEqualTo(1L);
        assertThat(rule.getRuleCode()).isEqualTo("ORDER_CENTER_WECHAT_MANGO_PAY_TEST");
        assertThat(rule.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(rule.getEnvironment()).isEqualTo("MANGO_PAY");
        verify(routeRuleItemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getContractCapabilityId()).isEqualTo(333001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_METHOD_ROUTE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                "ORDER_CENTER_WECHAT_MANGO_PAY_TEST",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("deleteRouteRule should reject and audit when payment orders reference route")
    void deleteRouteRule_withPaymentOrders_rejectsAndAudits() {
        PaymentMethodRouteRule rule = routeRule();
        when(routeRuleMapper.selectById(334001L)).thenReturn(rule);
        when(routeRuleMapper.countDeleteRelations(1L, 334001L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteRouteRule(334001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_METHOD_ROUTE_DELETE_HAS_RELATIONS.getMessage());

        verify(routeRuleItemMapper, never()).deleteByRuleId(334001L, 1L);
        verify(routeRuleMapper, never()).deleteById(334001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_METHOD_ROUTE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                "ORDER_CENTER_WECHAT_MANGO_PAY",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("trialRoute should return matched rule and item when candidate is enabled and in amount range")
    void trialRoute_matchedCandidate_returnsMatch() {
        when(routeRuleMapper.selectRouteCandidates(1L, 310001L, 320001L, "PERSONAL_WECHAT_QR", "WEB", null))
                .thenReturn(List.of(candidate(1L, 500000L, 1)));

        PaymentMethodRouteTrialVO result = service.trialRoute(trialCommand(9900L)).getData();

        assertThat(result.getMatched()).isTrue();
        assertThat(result.getMatchedRule().getRuleCode()).isEqualTo("ORDER_CENTER_WECHAT_MANGO_PAY");
        assertThat(result.getMatchedItem().getContractCapabilityId()).isEqualTo(333001L);
        assertThat(result.getFilterReasons()).isEmpty();
    }

    @Test
    @DisplayName("trialRoute should return filter reason when amount exceeds capability range")
    void trialRoute_amountExceeded_returnsFilterReason() {
        when(routeRuleMapper.selectRouteCandidates(1L, 310001L, 320001L, "PERSONAL_WECHAT_QR", "WEB", null))
                .thenReturn(List.of(candidate(1L, 100L, 1)));

        PaymentMethodRouteTrialVO result = service.trialRoute(trialCommand(9900L)).getData();

        assertThat(result.getMatched()).isFalse();
        assertThat(result.getFilterReasons()).anyMatch(reason -> reason.contains("超过签约能力最大金额"));
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_TRIAL_METHOD_ROUTE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD_ROUTE,
                "PERSONAL_WECHAT_QR",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("createRouteRule should reject when contract capability cannot resolve route environment")
    void createRouteRule_environmentMismatch_rejects() {
        when(routeRuleMapper.selectCount(any())).thenReturn(0L);
        when(contractCapabilityMapper.selectRouteCapability(1L, 333001L, "PERSONAL_WECHAT_QR", "WEB", "MANGO_PAY")).thenReturn(null);
        when(contractCapabilityMapper.selectRouteEnvironment(1L, 333001L, "PERSONAL_WECHAT_QR", "WEB")).thenReturn(null);
        SavePaymentMethodRouteRuleCommand command = command();
        command.setEnvironment(null);

        assertThatThrownBy(() -> service.createRouteRule(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID.getMessage());

        verify(routeRuleMapper, never()).insert(any(PaymentMethodRouteRule.class));
        verify(routeRuleItemMapper, never()).insert(any(PaymentMethodRouteRuleItem.class));
    }

    private SavePaymentMethodRouteRuleCommand command() {
        SavePaymentMethodRouteRuleCommand command = new SavePaymentMethodRouteRuleCommand();
        command.setRuleCode("ORDER_CENTER_WECHAT_MANGO_PAY_TEST");
        command.setRuleName("订单中心微信芒果支付路由测试");
        command.setAppId(310001L);
        command.setSubjectId(320001L);
        command.setMethodCode("PERSONAL_WECHAT_QR");
        command.setTerminalType("WEB");
        command.setRouteMode("PRIORITY");
        command.setFallbackEnabled(1);
        command.setStatus(1);
        command.setItems(List.of(itemCommand()));
        return command;
    }

    private SavePaymentMethodRouteRuleItemCommand itemCommand() {
        SavePaymentMethodRouteRuleItemCommand item = new SavePaymentMethodRouteRuleItemCommand();
        item.setContractCapabilityId(333001L);
        item.setPriority(10);
        item.setWeight(100);
        item.setMinAmount(1L);
        item.setMaxAmount(500000L);
        item.setStatus(1);
        return item;
    }

    private PaymentMethodRouteTrialCommand trialCommand(Long amount) {
        PaymentMethodRouteTrialCommand command = new PaymentMethodRouteTrialCommand();
        command.setApplicationId(310001L);
        command.setSubjectId(320001L);
        command.setMethodCode("PERSONAL_WECHAT_QR");
        command.setTerminalType("WEB");
        command.setAmount(amount);
        return command;
    }

    private PaymentMethodRouteRule routeRule() {
        PaymentMethodRouteRule rule = new PaymentMethodRouteRule();
        rule.setId(334001L);
        rule.setTenantId(1L);
        rule.setRuleCode("ORDER_CENTER_WECHAT_MANGO_PAY");
        rule.setRuleName("订单中心微信芒果支付路由");
        return rule;
    }

    private PaymentMethodRouteCandidate candidate(Long minAmount, Long maxAmount, Integer status) {
        PaymentMethodRouteCandidate candidate = new PaymentMethodRouteCandidate();
        candidate.setRouteRuleId(334001L);
        candidate.setRuleCode("ORDER_CENTER_WECHAT_MANGO_PAY");
        candidate.setRuleName("订单中心微信芒果支付路由");
        candidate.setAppId(310001L);
        candidate.setAppName("订单中心");
        candidate.setSubjectId(320001L);
        candidate.setSubjectName("芒果科技有限公司");
        candidate.setMethodCode("PERSONAL_WECHAT_QR");
        candidate.setMethodName("微信扫码");
        candidate.setTerminalType("WEB");
        candidate.setEnvironment("MANGO_PAY");
        candidate.setRouteMode("PRIORITY");
        candidate.setFallbackEnabled(1);
        candidate.setRouteItemId(335001L);
        candidate.setContractCapabilityId(333001L);
        candidate.setContractId(331001L);
        candidate.setContractName("芒果科技芒果支付签约");
        candidate.setChannelId(330001L);
        candidate.setChannelName("芒果支付");
        candidate.setItemPriority(10);
        candidate.setItemWeight(100);
        candidate.setItemMinAmount(1L);
        candidate.setItemMaxAmount(500000L);
        candidate.setItemStatus(status);
        candidate.setCapabilityMinAmount(minAmount);
        candidate.setCapabilityMaxAmount(maxAmount);
        candidate.setCapabilityStatus(1);
        candidate.setContractStatus(1);
        candidate.setChannelStatus(1);
        return candidate;
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppName("订单中心");
        return application;
    }

    private PaymentEnterpriseSubject subject() {
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setSubjectName("芒果科技有限公司");
        return subject;
    }

    private PaymentMethod method() {
        PaymentMethod method = new PaymentMethod();
        method.setId(340001L);
        method.setTenantId(1L);
        method.setMethodCode("PERSONAL_WECHAT_QR");
        method.setMethodName("微信扫码");
        return method;
    }

    private PaymentChannelContractCapability capability() {
        PaymentChannelContractCapability capability = new PaymentChannelContractCapability();
        capability.setId(333001L);
        capability.setTenantId(1L);
        capability.setMethodCode("PERSONAL_WECHAT_QR");
        capability.setTerminalType("WEB");
        return capability;
    }
}
