package io.mango.payment.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentCashierConfig;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.model.PaymentCashierRouteMatch;
import io.mango.payment.core.service.IPaymentChannelAdapter;
import io.mango.payment.core.service.PaymentChannelAdapterRegistry;
import io.mango.payment.core.service.PaymentNumberService;
import io.mango.payment.core.service.PaymentOrderStateService;
import io.mango.payment.core.service.PaymentOrderStatusFlowService;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentCashierServiceImplTest {

    private PaymentCashierConfigMapper cashierConfigMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentEnterpriseSubjectMapper subjectMapper;
    private PaymentMethodMapper methodMapper;
    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private PaymentChannelContractMapper channelContractMapper;
    private PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private TestPaymentChannelAdapter channelAdapter;
    private TestPaymentChannelAdapter offlineChannelAdapter;
    private PaymentOrderStatusFlowService statusFlowService;
    private PaymentSensitiveValueService sensitiveValueService;
    private PaymentNumberService numberService;
    private PaymentCashierServiceImpl service;

    @BeforeEach
    void setUp() {
        cashierConfigMapper = mock(PaymentCashierConfigMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        subjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        methodMapper = mock(PaymentMethodMapper.class);
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        channelContractMapper = mock(PaymentChannelContractMapper.class);
        contractCapabilityMapper = mock(PaymentChannelContractCapabilityMapper.class);
        channelAdapter = new TestPaymentChannelAdapter();
        offlineChannelAdapter = new TestPaymentChannelAdapter();
        offlineChannelAdapter.channelCode = "OFFLINE_COLLECTION";
        statusFlowService = mock(PaymentOrderStatusFlowService.class);
        sensitiveValueService = mock(PaymentSensitiveValueService.class);
        numberService = mock(PaymentNumberService.class);
        when(numberService.next(PaymentNumberService.PAY_ORDER_NO)).thenReturn("PO2026060600000001");
        service = new PaymentCashierServiceImpl(
                cashierConfigMapper,
                applicationMapper,
                subjectMapper,
                methodMapper,
                businessOrderMapper,
                paymentOrderMapper,
                channelContractMapper,
                contractCapabilityMapper,
                new PaymentChannelAdapterRegistry(List.of(channelAdapter, offlineChannelAdapter)),
                new PaymentOrderStateService(),
                statusFlowService,
                new ObjectMapper(),
                sensitiveValueService,
                numberService);
        when(channelContractMapper.selectById(any())).thenAnswer(invocation ->
                contract(invocation.getArgument(0), "{}"));
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @Test
    @DisplayName("detailSession should mask encrypted subject values")
    void detailSession_masksEncryptedSubjectValues() {
        when(cashierConfigMapper.selectById(350001L)).thenReturn(cashierConfig());
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        PaymentEnterpriseSubject subject = subject();
        subject.setCreditCode("enc:credit-ciphertext");
        subject.setBankAccountNo("enc:account-ciphertext");
        when(subjectMapper.selectById(320001L)).thenReturn(subject);
        when(methodMapper.selectOne(any())).thenReturn(method());
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "PERSONAL_WECHAT_QR", "WEB", 9900L))
                .thenReturn(route());
        when(sensitiveValueService.mask("enc:credit-ciphertext", 4, 4)).thenReturn("9131****001X");
        when(sensitiveValueService.mask("enc:account-ciphertext", 4, 4)).thenReturn("6222****0001");

        var result = service.detailSession(350001L, 360001L).getData();

        assertThat(result.getSubject().getCreditCode()).isEqualTo("9131****001X");
        assertThat(result.getSubject().getBankAccountNo()).isEqualTo("6222****0001");
    }

    @Test
    @DisplayName("detailSession should use configured cashier display groups")
    void detailSession_groupsEbankAndOfflineTransferWithBusinessNames() {
        PaymentCashierConfig config = cashierConfig();
        config.setMethodCodes("PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT");
        config.setMethodDisplayOrder("PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT,CORPORATE_OFFLINE_ACCOUNT");
        config.setDefaultMethodCode("PERSONAL_EBANK_REDIRECT");
        when(cashierConfigMapper.selectById(350001L)).thenReturn(config);
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(methodMapper.selectOne(any()))
                .thenReturn(method("PERSONAL_EBANK_REDIRECT", "个人网银", "PERSONAL", "EBANK", "BANK_GATEWAY", "HTML_FORM"))
                .thenReturn(method("CORPORATE_EBANK_REDIRECT", "企业网银", "CORPORATE", "EBANK", "BANK_GATEWAY", "HTML_FORM"))
                .thenReturn(method("CORPORATE_OFFLINE_ACCOUNT", "线下转账", "CORPORATE", "OFFLINE_TRANSFER", "ACCOUNT_TRANSFER", "TRANSFER_ACCOUNT"));
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "PERSONAL_EBANK_REDIRECT", "WEB", 9900L))
                .thenReturn(route("PERSONAL_EBANK_REDIRECT", "MANGO_PAY"));
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "CORPORATE_EBANK_REDIRECT", "WEB", 9900L))
                .thenReturn(route("CORPORATE_EBANK_REDIRECT", "MANGO_PAY"));
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "CORPORATE_OFFLINE_ACCOUNT", "WEB", 9900L))
                .thenReturn(route("CORPORATE_OFFLINE_ACCOUNT", "OFFLINE_COLLECTION"));

        var result = service.detailSession(350001L, 360001L).getData();

        assertThat(result.getMethods())
                .extracting("methodCode", "methodName", "categoryCode", "categoryName")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("PERSONAL_EBANK_REDIRECT", "个人网银", "EBANK", "网银支付"),
                        org.assertj.core.groups.Tuple.tuple("CORPORATE_EBANK_REDIRECT", "企业网银", "EBANK", "网银支付"),
                        org.assertj.core.groups.Tuple.tuple("CORPORATE_OFFLINE_ACCOUNT", "线下转账", "OFFLINE_TRANSFER", "线下转账"));
    }

    @Test
    @DisplayName("pay should pass offline collection contract account config to channel adapter")
    void pay_offlineTransfer_usesContractAccountConfig() {
        PaymentCashierConfig config = cashierConfig();
        config.setMethodCodes("CORPORATE_OFFLINE_ACCOUNT");
        config.setDefaultMethodCode("CORPORATE_OFFLINE_ACCOUNT");
        config.setMethodDisplayOrder("CORPORATE_OFFLINE_ACCOUNT");
        PaymentMethod method = method();
        method.setMethodCode("CORPORATE_OFFLINE_ACCOUNT");
        method.setPaymentMaterialType("TRANSFER_ACCOUNT");
        PaymentChannelContract offlineContract = contract(331004L,
                "{\"accountName\":\"芒果科技签约户\",\"accountNo\":\"enc:offline-account\",\"bankName\":\"签约开户行\"}");
        when(cashierConfigMapper.selectById(350001L)).thenReturn(config);
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(methodMapper.selectOne(any())).thenReturn(method);
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "CORPORATE_OFFLINE_ACCOUNT", "WEB", 9900L))
                .thenReturn(offlineRoute());
        when(channelContractMapper.selectById(331004L)).thenReturn(offlineContract);
        when(paymentOrderMapper.countSuccessfulCashierOrders(1L, 360001L)).thenReturn(0L);
        when(paymentOrderMapper.selectProcessingPayOrderNo(1L, 360001L, 340001L)).thenReturn(null);
        when(businessOrderMapper.touchCashierPayingOrder(1L, 360001L)).thenReturn(1);
        offlineChannelAdapter.materialType = "TRANSFER_ACCOUNT";
        PaymentCashierPayCommand command = new PaymentCashierPayCommand();
        command.setCashierConfigId(350001L);
        command.setBusinessOrderId(360001L);
        command.setMethodCode("CORPORATE_OFFLINE_ACCOUNT");

        PaymentCashierPayResultVO result = service.pay(command).getData();

        assertThat(result.getChannelCode()).isEqualTo("OFFLINE_COLLECTION");
        assertThat(offlineChannelAdapter.lastPaymentCommand.contractConfigValuesJson()).isEqualTo(offlineContract.getConfigValuesJson());
        assertThat(offlineChannelAdapter.lastPaymentCommand.subjectId()).isEqualTo(320001L);
        assertThat(offlineChannelAdapter.lastPaymentCommand.subjectName()).isEqualTo("芒果科技有限公司");
        assertThat(offlineChannelAdapter.afterCreatedCalled).isTrue();
        assertThat(offlineChannelAdapter.createdOrder.getChannelCode()).isEqualTo("OFFLINE_COLLECTION");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("pay should create paying payment order and wait for channel evidence")
    void pay_successConfiguredRoute_createsPayingOrderWithoutSuccessSideEffects() {
        when(cashierConfigMapper.selectById(350001L)).thenReturn(cashierConfig());
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(methodMapper.selectOne(any())).thenReturn(method());
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "PERSONAL_WECHAT_QR", "WEB", 9900L))
                .thenReturn(route());
        when(paymentOrderMapper.countSuccessfulCashierOrders(1L, 360001L)).thenReturn(0L);
        when(paymentOrderMapper.selectProcessingPayOrderNo(1L, 360001L, 340001L)).thenReturn(null);
        when(businessOrderMapper.touchCashierPayingOrder(1L, 360001L)).thenReturn(1);
        ArgumentCaptor<PaymentOrderEntity> orderCaptor = ArgumentCaptor.forClass(PaymentOrderEntity.class);
        PaymentCashierPayCommand command = new PaymentCashierPayCommand();
        command.setCashierConfigId(350001L);
        command.setBusinessOrderId(360001L);
        command.setMethodCode("PERSONAL_WECHAT_QR");

        PaymentCashierPayResultVO result = service.pay(command).getData();

        verify(paymentOrderMapper).insert(orderCaptor.capture());
        PaymentOrderEntity order = orderCaptor.getValue();
        assertThat(channelAdapter.lastPaymentCommand.tenantId()).isEqualTo(1L);
        assertThat(channelAdapter.lastPaymentCommand.channelCode()).isEqualTo("MANGO_PAY");
        assertThat(channelAdapter.lastPaymentCommand.contractId()).isEqualTo(331001L);
        assertThat(channelAdapter.lastPaymentCommand.payOrderNo()).isEqualTo(order.getPayOrderNo());
        assertThat(channelAdapter.lastPaymentCommand.bizOrderNo()).isEqualTo("BO202606060001");
        assertThat(channelAdapter.lastPaymentCommand.methodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(channelAdapter.lastPaymentCommand.paymentMaterialType()).isEqualTo("QR");
        assertThat(channelAdapter.lastPaymentCommand.amount()).isEqualTo(9900L);
        assertThat(channelAdapter.lastPaymentCommand.currency()).isEqualTo("CNY");
        assertThat(channelAdapter.lastPaymentCommand.title()).isEqualTo("测试订单");
        assertThat(order.getTenantId()).isEqualTo(1L);
        assertThat(order.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(order.getCashierConfigId()).isEqualTo(350001L);
        assertThat(order.getMethodId()).isEqualTo(340001L);
        assertThat(order.getStatus()).isEqualTo("PAYING");
        assertThat(order.getSuccessFlag()).isZero();
        assertThat(order.getPayTime()).isNull();
        assertThat(order.getChannelTradeNo()).isEqualTo("TEST-" + order.getPayOrderNo());
        assertThat(order.getPaymentMaterialJson()).contains("qrContent");
        assertThat(order.getPaymentMaterialJson()).contains(order.getPayOrderNo());
        assertThat(order.getAmount()).isEqualTo(9900L);
        verify(businessOrderMapper).touchCashierPayingOrder(1L, 360001L);
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT),
                eq(order.getId()),
                eq(order.getPayOrderNo()),
                isNull(),
                eq("CREATED"),
                eq(PaymentOrderStatusFlowService.SOURCE_CASHIER_PAY),
                eq(order.getPayOrderNo()),
                any(LocalDateTime.class),
                eq("收银台生成支付订单"));
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT),
                eq(order.getId()),
                eq(order.getPayOrderNo()),
                eq("CREATED"),
                eq("PAYING"),
                eq(PaymentOrderStatusFlowService.SOURCE_CASHIER_PAY),
                eq(order.getPayOrderNo()),
                any(LocalDateTime.class),
                eq("收银台支付请求已提交，等待通道回调、主动查单或对账补偿推进"));
        assertThat(result.getStatus()).isEqualTo("PAYING");
        assertThat(result.getChannelCode()).isEqualTo("MANGO_PAY");
        assertThat(result.getMethodCode()).isEqualTo("PERSONAL_WECHAT_QR");
        assertThat(result.getAmount()).isEqualTo(9900L);
        assertThat(result.getMaterial().getQrContent()).isEqualTo("test-pay:" + order.getPayOrderNo());
    }

    @Test
    @DisplayName("pay should advance TO_PAY business order to PAYING when payment is submitted")
    void pay_toPayBusinessOrder_advancesBusinessOrderToPaying() {
        PaymentBusinessOrderEntity toPayOrder = businessOrder();
        toPayOrder.setStatus("TO_PAY");
        when(cashierConfigMapper.selectById(350001L)).thenReturn(cashierConfig());
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(toPayOrder);
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(methodMapper.selectOne(any())).thenReturn(method());
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "PERSONAL_WECHAT_QR", "WEB", 9900L))
                .thenReturn(route());
        when(paymentOrderMapper.countSuccessfulCashierOrders(1L, 360001L)).thenReturn(0L);
        when(paymentOrderMapper.selectProcessingPayOrderNo(1L, 360001L, 340001L)).thenReturn(null);
        when(businessOrderMapper.touchCashierPayingOrder(1L, 360001L)).thenReturn(1);
        PaymentCashierPayCommand command = new PaymentCashierPayCommand();
        command.setCashierConfigId(350001L);
        command.setBusinessOrderId(360001L);
        command.setMethodCode("PERSONAL_WECHAT_QR");

        PaymentCashierPayResultVO result = service.pay(command).getData();

        assertThat(result.getStatus()).isEqualTo("PAYING");
        verify(businessOrderMapper).touchCashierPayingOrder(1L, 360001L);
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
    }

    @Test
    @DisplayName("pay should surface duplicate successful payment constraint as business error")
    void pay_successDuplicateConstraint_throwsAlreadyPaid() {
        when(cashierConfigMapper.selectById(350001L)).thenReturn(cashierConfig());
        when(applicationMapper.selectById(310001L)).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        when(subjectMapper.selectById(320001L)).thenReturn(subject());
        when(methodMapper.selectOne(any())).thenReturn(method());
        when(contractCapabilityMapper.selectRoutedCashierCapability(1L, 310001L, 320001L, "PERSONAL_WECHAT_QR", "WEB", 9900L))
                .thenReturn(route());
        when(paymentOrderMapper.countSuccessfulCashierOrders(1L, 360001L)).thenReturn(0L);
        when(paymentOrderMapper.selectProcessingPayOrderNo(1L, 360001L, 340001L)).thenReturn(null);
        when(paymentOrderMapper.insert(any(PaymentOrderEntity.class))).thenThrow(new DuplicateKeyException("duplicate success"));
        PaymentCashierPayCommand command = new PaymentCashierPayCommand();
        command.setCashierConfigId(350001L);
        command.setBusinessOrderId(360001L);
        command.setMethodCode("PERSONAL_WECHAT_QR");

        assertThatThrownBy(() -> service.pay(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_BUSINESS_ORDER_ALREADY_PAID.getCode());
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
    }

    private PaymentCashierConfig cashierConfig() {
        PaymentCashierConfig config = new PaymentCashierConfig();
        config.setId(350001L);
        config.setTenantId(1L);
        config.setCashierName("订单中心 Web 收银台");
        config.setApplicationId(310001L);
        config.setEnterpriseSubjectIds("320001");
        config.setMethodCodes("PERSONAL_WECHAT_QR");
        config.setDefaultMethodCode("PERSONAL_WECHAT_QR");
        config.setMethodDisplayOrder("PERSONAL_WECHAT_QR");
        config.setStatus(1);
        return config;
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppId("app_order_center");
        application.setAppName("订单中心");
        application.setStatus(1);
        return application;
    }

    private PaymentBusinessOrderEntity businessOrder() {
        PaymentBusinessOrderEntity order = new PaymentBusinessOrderEntity();
        order.setId(360001L);
        order.setTenantId(1L);
        order.setBizOrderNo("BO202606060001");
        order.setAppCode("app_order_center");
        order.setTitle("测试订单");
        order.setSubjectId(320001L);
        order.setAmount(9900L);
        order.setCurrency("CNY");
        order.setStatus("PAYING");
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        return order;
    }

    private PaymentEnterpriseSubject subject() {
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setSubjectName("芒果科技有限公司");
        subject.setCreditCode("91310000MA1PAY001X");
        subject.setBankAccountNo("6222000000000001");
        subject.setBankName("招商银行");
        subject.setStatus(1);
        return subject;
    }

    private PaymentMethod method() {
        PaymentMethod method = new PaymentMethod();
        method.setId(340001L);
        method.setTenantId(1L);
        method.setMethodCode("PERSONAL_WECHAT_QR");
        method.setMethodName("微信扫码");
        method.setAccountNature("PERSONAL");
        method.setInstrumentType("WECHAT");
        method.setInteractionType("QR_CODE");
        method.setTerminalScope("WEB,H5");
        method.setPaymentMaterialType("QR");
        method.setCashierGroupCode("WECHAT_PAY");
        method.setCashierGroupName("微信支付");
        method.setCashierGroupSort(10);
        method.setMinAmount(1L);
        method.setMaxAmount(5000000L);
        method.setStatus(1);
        return method;
    }

    private PaymentMethod method(
            String methodCode,
            String methodName,
            String accountNature,
            String instrumentType,
            String interactionType,
            String paymentMaterialType) {
        PaymentMethod method = method();
        method.setMethodCode(methodCode);
        method.setMethodName(methodName);
        method.setAccountNature(accountNature);
        method.setInstrumentType(instrumentType);
        method.setInteractionType(interactionType);
        method.setPaymentMaterialType(paymentMaterialType);
        if ("EBANK".equals(instrumentType)) {
            method.setCashierGroupCode("EBANK");
            method.setCashierGroupName("网银支付");
            method.setCashierGroupSort(30);
        } else if ("OFFLINE_TRANSFER".equals(instrumentType)) {
            method.setCashierGroupCode("OFFLINE_TRANSFER");
            method.setCashierGroupName("线下转账");
            method.setCashierGroupSort(40);
        }
        return method;
    }

    private PaymentCashierRouteMatch route() {
        PaymentCashierRouteMatch route = new PaymentCashierRouteMatch();
        route.setChannelId(330001L);
        route.setChannelCode("MANGO_PAY");
        route.setChannelName("芒果支付");
        route.setContractId(331001L);
        route.setContractName("芒果科技芒果支付签约");
        route.setContractCapabilityId(333001L);
        route.setRouteRuleId(334001L);
        route.setChannelMerchantNo("MANGO_PAY_MERCHANT_001");
        return route;
    }

    private PaymentCashierRouteMatch route(String methodCode, String channelCode) {
        PaymentCashierRouteMatch route = route();
        route.setChannelCode(channelCode);
        route.setChannelName("OFFLINE_COLLECTION".equals(channelCode) ? "线下收款" : "芒果支付");
        route.setContractCapabilityId(Math.abs((long) methodCode.hashCode()));
        return route;
    }

    private PaymentCashierRouteMatch offlineRoute() {
        PaymentCashierRouteMatch route = new PaymentCashierRouteMatch();
        route.setChannelId(330004L);
        route.setChannelCode("OFFLINE_COLLECTION");
        route.setChannelName("线下收款");
        route.setContractId(331004L);
        route.setContractName("芒果科技线下收款签约");
        route.setContractCapabilityId(333014L);
        route.setRouteRuleId(334007L);
        route.setChannelMerchantNo("OFFLINE_COLLECTION_MERCHANT_001");
        return route;
    }

    private PaymentChannelContract contract(Long id, String configValuesJson) {
        PaymentChannelContract contract = new PaymentChannelContract();
        contract.setId(id);
        contract.setTenantId(1L);
        contract.setConfigValuesJson(configValuesJson);
        return contract;
    }

    private static class TestPaymentChannelAdapter implements IPaymentChannelAdapter {

        private IPaymentChannelAdapter.PaymentApplyCommand lastPaymentCommand;
        private PaymentOrderEntity createdOrder;
        private String materialType = "QR";
        private String channelCode = "MANGO_PAY";
        private boolean afterCreatedCalled;

        @Override
        public String channelCode() {
            return channelCode;
        }

        @Override
        public PaymentApplyResult applyPayment(PaymentApplyCommand command) {
            this.lastPaymentCommand = command;
            PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
            material.setMaterialType(materialType);
            if ("TRANSFER_ACCOUNT".equals(materialType)) {
                material.setAccountName("签约收款户");
                material.setAccountNo("6222000000000001");
                material.setBankName("签约开户行");
                material.setTransferRemark("A1b2C3");
            } else {
                material.setQrContent("test-pay:" + command.payOrderNo());
            }
            return new PaymentApplyResult(
                    "SUCCESS",
                    "MANGO_PAY_SUCCESS",
                    "SYNC_SUCCESS",
                    "SUCCESS",
                    "TEST-" + command.payOrderNo(),
                    material);
        }

        @Override
        public void afterPaymentOrderCreated(PaymentApplyCommand command, PaymentApplyResult result, PaymentOrderEntity order) {
            this.afterCreatedCalled = true;
            this.createdOrder = order;
        }

        @Override
        public RefundApplyResult applyRefund(RefundApplyCommand command) {
            throw new AssertionError("退款不属于本测试适配器覆盖范围");
        }

        @Override
        public ChannelBillResult generateBill(ChannelBillCommand command) {
            return new ChannelBillResult(List.<PaymentChannelBillItemRow>of());
        }

        @Override
        public PaymentQueryResult queryPayment(PaymentQueryCommand command) {
            throw new AssertionError("查单不属于本测试适配器覆盖范围");
        }

        @Override
        public RefundQueryResult queryRefund(RefundQueryCommand command) {
            throw new AssertionError("查退款不属于本测试适配器覆盖范围");
        }
    }
}
