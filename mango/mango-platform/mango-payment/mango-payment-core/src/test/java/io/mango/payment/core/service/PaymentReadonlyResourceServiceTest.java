package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentBusinessOrderCommand;
import io.mango.payment.api.command.HandlePaymentDifferenceCommand;
import io.mango.payment.api.command.HandlePaymentExceptionOrderCommand;
import io.mango.payment.api.command.QueryPaymentRefundOrderCommand;
import io.mango.payment.api.command.RetryPaymentNotificationRecordCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentBusinessOrderVO;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentDifferenceVO;
import io.mango.payment.api.vo.PaymentExceptionOrderVO;
import io.mango.payment.api.vo.PaymentNotificationRecordVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.api.vo.PaymentOperationAuditVO;
import io.mango.payment.api.vo.PaymentOrderStatusFlowVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentDifferenceEntity;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentExceptionOrderEntity;
import io.mango.payment.core.entity.PaymentNotificationRecordEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentExceptionOrderMapper;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMapper;
import io.mango.payment.core.mapper.PaymentOperationAuditMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;

class PaymentReadonlyResourceServiceTest {

    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentEnterpriseSubjectMapper enterpriseSubjectMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentTransactionFlowMapper transactionFlowMapper;
    private PaymentChannelCapabilityMapper channelCapabilityMapper;
    private PaymentExceptionOrderMapper exceptionOrderMapper;
    private PaymentNotificationRecordMapper notificationRecordMapper;
    private PaymentDifferenceMapper differenceMapper;
    private PaymentOfflineCollectionMapper offlineCollectionMapper;
    private PaymentOperationAuditMapper operationAuditMapper;
    private PaymentOperationAuditService auditService;
    private PaymentChannelSyncService channelSyncService;
    private PaymentChannelOrderCloseService channelOrderCloseService;
    private PaymentOrderStatusFlowService statusFlowService;
    private PaymentNotificationService notificationService;
    private PaymentNumberService numberService;
    private PaymentReadonlyResourceService service;

    @BeforeEach
    void setUp() {
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        enterpriseSubjectMapper = mock(PaymentEnterpriseSubjectMapper.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        transactionFlowMapper = mock(PaymentTransactionFlowMapper.class);
        channelCapabilityMapper = mock(PaymentChannelCapabilityMapper.class);
        exceptionOrderMapper = mock(PaymentExceptionOrderMapper.class);
        notificationRecordMapper = mock(PaymentNotificationRecordMapper.class);
        differenceMapper = mock(PaymentDifferenceMapper.class);
        offlineCollectionMapper = mock(PaymentOfflineCollectionMapper.class);
        operationAuditMapper = mock(PaymentOperationAuditMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        channelSyncService = mock(PaymentChannelSyncService.class);
        channelOrderCloseService = mock(PaymentChannelOrderCloseService.class);
        statusFlowService = mock(PaymentOrderStatusFlowService.class);
        notificationService = mock(PaymentNotificationService.class);
        numberService = mock(PaymentNumberService.class);
        when(numberService.next(PaymentNumberService.PAY_BIZ_ORDER_NO)).thenReturn("BO2026060600000001");
        when(numberService.next(PaymentNumberService.PAY_ADJUST_FLOW_NO)).thenReturn("AF2026060600000001");
        service = new PaymentReadonlyResourceService(applicationMapper, enterpriseSubjectMapper, businessOrderMapper, paymentOrderMapper, refundOrderMapper, transactionFlowMapper, channelCapabilityMapper, exceptionOrderMapper, notificationRecordMapper, differenceMapper, offlineCollectionMapper, operationAuditMapper, auditService, channelSyncService, channelOrderCloseService, statusFlowService, notificationService, numberService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @Test
    @DisplayName("pageChannelCapabilities should query typed mapper")
    void pageChannelCapabilities_usesTypedMapper() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setPage(2L);
        query.setSize(20L);
        query.setKeyword(" WECHAT ");
        query.setStatus(1);
        query.setChannelId(330001L);
        PaymentChannelCapabilityVO row = new PaymentChannelCapabilityVO();
        row.setId(331001L);
        row.setChannelId(330001L);
        row.setChannelName("芒果支付");
        row.setMethodCode("PERSONAL_WECHAT_QR");
        row.setMethodName("微信扫码");
        when(channelCapabilityMapper.countChannelCapabilities(1L, "WECHAT", 1, 330001L)).thenReturn(1L);
        when(channelCapabilityMapper.selectChannelCapabilityPage(1L, "WECHAT", 1, 330001L, 20L, 20L)).thenReturn(List.of(row));

        var result = service.pageChannelCapabilities(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).containsExactly(row);
        verify(channelCapabilityMapper).countChannelCapabilities(1L, "WECHAT", 1, 330001L);
        verify(channelCapabilityMapper).selectChannelCapabilityPage(1L, "WECHAT", 1, 330001L, 20L, 20L);
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("pageBusinessOrders should query through mapper and fill status name")
    void pageBusinessOrders_usesMapperAndFillsStatusName() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setPage(2L);
        query.setSize(5L);
        query.setKeyword(" BO-001 ");
        query.setStatusCode("PAYING");
        query.setApplicationId(320001L);
        query.setEnterpriseSubjectId(310001L);
        PaymentBusinessOrderVO row = new PaymentBusinessOrderVO();
        row.setId(910001L);
        row.setBizOrderNo("BO-001");
        row.setStatus("PAYING");
        row.setCashierConfigId(350001L);
        when(businessOrderMapper.countBusinessOrders(1L, "BO-001", "PAYING", 320001L, 310001L)).thenReturn(1L);
        when(businessOrderMapper.selectBusinessOrderPage(1L, "BO-001", "PAYING", 320001L, 310001L, 5L, 5L)).thenReturn(List.of(row));

        var result = service.pageBusinessOrders(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getStatusName()).isEqualTo("支付中");
        assertThat(result.getList().get(0).getCashierConfigId()).isEqualTo(350001L);
        verify(businessOrderMapper).countBusinessOrders(1L, "BO-001", "PAYING", 320001L, 310001L);
        verify(businessOrderMapper).selectBusinessOrderPage(1L, "BO-001", "PAYING", 320001L, 310001L, 5L, 5L);
    }

    @Test
    @DisplayName("detailBusinessOrder should reject missing row with payment code")
    void detailBusinessOrder_missingRow_rejectsWithPaymentCode() {
        when(businessOrderMapper.selectBusinessOrderDetail(1L, 910001L)).thenReturn(null);

        assertThatThrownBy(() -> service.detailBusinessOrder(910001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("createBusinessOrder should persist to-pay order and record status flow")
    void createBusinessOrder_persistsToPayOrderAndRecordsStatusFlow() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppId("app_order_center");
        application.setStatus(1);
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setStatus(1);
        PaymentBusinessOrderVO detail = new PaymentBusinessOrderVO();
        detail.setId(910002L);
        detail.setBizOrderNo("BO-MANUAL-001");
        detail.setStatus("TO_PAY");
        when(applicationMapper.selectOne(any())).thenReturn(application);
        when(enterpriseSubjectMapper.selectById(320001L)).thenReturn(subject);
        when(businessOrderMapper.selectOne(any())).thenReturn(null);
        when(businessOrderMapper.selectBusinessOrderDetail(eq(1L), any())).thenReturn(detail);
        ArgumentCaptor<PaymentBusinessOrderEntity> orderCaptor = ArgumentCaptor.forClass(PaymentBusinessOrderEntity.class);
        CreatePaymentBusinessOrderCommand command = new CreatePaymentBusinessOrderCommand();
        command.setAppId(" app_order_center ");
        command.setBizOrderNo(" BO-MANUAL-001 ");
        command.setTitle(" 人工验收订单 ");
        command.setSubjectId(320001L);
        command.setAmount(128800L);
        command.setCurrency("CNY");
        command.setExpireTime(LocalDateTime.of(2026, 6, 8, 10, 0));

        PaymentBusinessOrderVO result = service.createBusinessOrder(command);

        verify(businessOrderMapper).insert(orderCaptor.capture());
        PaymentBusinessOrderEntity entity = orderCaptor.getValue();
        assertThat(entity.getAppCode()).isEqualTo("app_order_center");
        assertThat(entity.getBizOrderNo()).isEqualTo("BO-MANUAL-001");
        assertThat(entity.getTitle()).isEqualTo("人工验收订单");
        assertThat(entity.getAmount()).isEqualTo(128800L);
        assertThat(entity.getPaidAmount()).isZero();
        assertThat(entity.getRefundedAmount()).isZero();
        assertThat(entity.getStatus()).isEqualTo("TO_PAY");
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS),
                eq(entity.getId()),
                eq("BO-MANUAL-001"),
                eq(null),
                eq("TO_PAY"),
                eq("MANUAL_CREATE_BUSINESS_ORDER"),
                eq("BO-MANUAL-001"),
                any(LocalDateTime.class),
                eq("后台创建业务订单"));
        assertThat(result.getStatusName()).isEqualTo("待支付");
    }

    @Test
    @DisplayName("createBusinessOrder should generate business order number on server when omitted")
    void createBusinessOrder_withoutBizOrderNo_generatesServerNumber() {
        PaymentApplication application = new PaymentApplication();
        application.setId(310001L);
        application.setTenantId(1L);
        application.setAppId("app_order_center");
        application.setStatus(1);
        PaymentEnterpriseSubject subject = new PaymentEnterpriseSubject();
        subject.setId(320001L);
        subject.setTenantId(1L);
        subject.setStatus(1);
        PaymentBusinessOrderVO detail = new PaymentBusinessOrderVO();
        detail.setId(910002L);
        detail.setBizOrderNo("BO202606071200001234");
        detail.setStatus("TO_PAY");
        when(applicationMapper.selectOne(any())).thenReturn(application);
        when(enterpriseSubjectMapper.selectById(320001L)).thenReturn(subject);
        when(businessOrderMapper.selectOne(any())).thenReturn(null);
        when(businessOrderMapper.selectBusinessOrderDetail(eq(1L), any())).thenReturn(detail);
        ArgumentCaptor<PaymentBusinessOrderEntity> orderCaptor = ArgumentCaptor.forClass(PaymentBusinessOrderEntity.class);
        CreatePaymentBusinessOrderCommand command = new CreatePaymentBusinessOrderCommand();
        command.setAppId("app_order_center");
        command.setTitle("人工验收订单");
        command.setSubjectId(320001L);
        command.setAmount(128800L);

        service.createBusinessOrder(command);

        verify(businessOrderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getBizOrderNo()).startsWith("BO");
    }

    @Test
    @DisplayName("pagePaymentOrders should query through mapper and fill status name and flow number")
    void pagePaymentOrders_usesMapperAndFillsFlowNo() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setKeyword(" PO-001 ");
        query.setStatusCode("PAYING");
        query.setChannelId(330001L);
        PaymentOrderVO row = new PaymentOrderVO();
        row.setId(370001L);
        row.setPayOrderNo("PO-001");
        row.setStatus("PAYING");
        row.setRefundedAmount(1200L);
        when(paymentOrderMapper.countPaymentOrders(1L, "PO-001", "PAYING", 330001L)).thenReturn(1L);
        when(paymentOrderMapper.selectPaymentOrderPage(1L, "PO-001", "PAYING", 330001L, 10L, 0L)).thenReturn(List.of(row));
        when(paymentOrderMapper.selectLatestFlowNo(1L, 370001L)).thenReturn("FLOW-001");

        var result = service.pagePaymentOrders(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList().get(0).getStatusName()).isEqualTo("支付中");
        assertThat(result.getList().get(0).getFlowNo()).isEqualTo("FLOW-001");
        assertThat(result.getList().get(0).getRefundedAmount()).isEqualTo(1200L);
    }

    @Test
    @DisplayName("detailPaymentOrder should reject missing row with payment code")
    void detailPaymentOrder_missingRow_rejectsWithPaymentCode() {
        when(paymentOrderMapper.selectPaymentOrderDetail(1L, 370001L)).thenReturn(null);

        assertThatThrownBy(() -> service.detailPaymentOrder(370001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("pageOfflineCollections should query through mapper and fill collection status name")
    void pageOfflineCollections_usesMapperAndFillsStatusName() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setPage(2L);
        query.setSize(5L);
        query.setKeyword(" OC-001 ");
        query.setStatusCode("PENDING_CONFIRM");
        PaymentOfflineCollectionVO row = new PaymentOfflineCollectionVO();
        row.setId(382001L);
        row.setOfflineCollectionNo("OC-001");
        row.setCollectionStatus("PENDING_CONFIRM");
        when(offlineCollectionMapper.countOfflineCollections(1L, "OC-001", "PENDING_CONFIRM")).thenReturn(1L);
        when(offlineCollectionMapper.selectOfflineCollectionPage(1L, "OC-001", "PENDING_CONFIRM", 5L, 5L))
                .thenReturn(List.of(row));

        var result = service.pageOfflineCollections(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getCollectionStatusName()).isEqualTo("待确认到账");
        verify(offlineCollectionMapper).countOfflineCollections(1L, "OC-001", "PENDING_CONFIRM");
        verify(offlineCollectionMapper).selectOfflineCollectionPage(1L, "OC-001", "PENDING_CONFIRM", 5L, 5L);
    }

    @Test
    @DisplayName("detailOfflineCollection should reject missing row with payment code")
    void detailOfflineCollection_missingRow_rejectsWithPaymentCode() {
        when(offlineCollectionMapper.selectOfflineCollectionDetail(1L, 382001L)).thenReturn(null);

        assertThatThrownBy(() -> service.detailOfflineCollection(382001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("pageRefundOrders should query through mapper and normalize status name")
    void pageRefundOrders_usesMapperAndNormalizesStatus() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setKeyword(" RO-001 ");
        query.setStatusCode("REFUNDING");
        PaymentRefundOrderVO row = new PaymentRefundOrderVO();
        row.setId(380001L);
        row.setRefundOrderNo("RO-001");
        row.setStatus("PROCESSING");
        when(refundOrderMapper.countRefundOrders(1L, "RO-001", "REFUNDING")).thenReturn(1L);
        when(refundOrderMapper.selectRefundOrderPage(1L, "RO-001", "REFUNDING", 10L, 0L)).thenReturn(List.of(row));
        when(refundOrderMapper.selectLatestFlowNo(1L, 380001L)).thenReturn("RFLOW-001");

        var result = service.pageRefundOrders(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList().get(0).getStatusName()).isEqualTo("退款中");
        assertThat(result.getList().get(0).getFlowNo()).isEqualTo("RFLOW-001");
    }

    @Test
    @DisplayName("detailRefundOrder should include persisted status flows")
    void detailRefundOrder_loadsPersistedStatusFlow() {
        PaymentRefundOrderVO row = new PaymentRefundOrderVO();
        row.setId(380001L);
        row.setStatus("SUCCESS");
        row.setCreateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        row.setRefundTime(LocalDateTime.of(2026, 6, 1, 10, 5));
        PaymentOrderStatusFlowVO flow = new PaymentOrderStatusFlowVO();
        flow.setToStatus("SUCCESS");
        flow.setTriggerSource(PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY);
        flow.setHappenTime(LocalDateTime.of(2026, 6, 1, 10, 5));
        when(refundOrderMapper.selectRefundOrderDetail(1L, 380001L)).thenReturn(row);
        when(statusFlowService.list(1L, PaymentOrderStatusFlowService.ORDER_TYPE_REFUND, 380001L))
                .thenReturn(List.of(flow));

        PaymentRefundOrderVO result = service.detailRefundOrder(380001L);

        assertThat(result.getStatusFlows()).hasSize(1);
        assertThat(result.getStatusFlows().get(0).getStatusName()).isEqualTo("退款成功");
        assertThat(result.getStatusFlows().get(0).getSource()).isEqualTo("主动查退款");
    }

    @Test
    @DisplayName("queryRefundOrder should invoke channel refund query and return refreshed detail")
    void queryRefundOrder_invokesChannelQueryAndReturnsRefreshedDetail() {
        PaymentRefundOrderVO before = new PaymentRefundOrderVO();
        before.setId(380002L);
        before.setRefundOrderNo("RO2026061000000001");
        before.setStatus("REFUNDING");
        PaymentRefundOrderVO after = new PaymentRefundOrderVO();
        after.setId(380002L);
        after.setRefundOrderNo("RO2026061000000001");
        after.setStatus("SUCCESS");
        after.setRefundTime(LocalDateTime.of(2026, 6, 1, 10, 10));
        when(refundOrderMapper.selectRefundOrderDetail(1L, 380002L)).thenReturn(before, after);
        when(channelSyncService.syncRefundStatus("RO2026061000000001"))
                .thenReturn(new PaymentChannelSyncService.RefundSyncResult(
                        "RO2026061000000001",
                        "SUCCESS",
                        "RFLOW202606100002",
                        true,
                        1L,
                        "UPDATED"));

        QueryPaymentRefundOrderCommand command = new QueryPaymentRefundOrderCommand();
        command.setId(380002L);
        PaymentRefundOrderVO result = service.queryRefundOrder(command);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getStatusName()).isEqualTo("退款成功");
        verify(channelSyncService).syncRefundStatus("RO2026061000000001");
    }

    @Test
    @DisplayName("pageTransactionFlows should query through mapper and fill flow type name")
    void pageTransactionFlows_usesMapperAndFillsFlowTypeName() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setKeyword(" FLOW-001 ");
        PaymentTransactionFlowVO row = new PaymentTransactionFlowVO();
        row.setId(390001L);
        row.setFlowNo("FLOW-001");
        row.setFlowType("PAY_SUCCESS");
        when(transactionFlowMapper.countTransactionFlows(1L, "FLOW-001")).thenReturn(1L);
        when(transactionFlowMapper.selectTransactionFlowPage(1L, "FLOW-001", 10L, 0L)).thenReturn(List.of(row));

        var result = service.pageTransactionFlows(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList().get(0).getFlowTypeName()).isEqualTo("支付成功收入");
        verify(transactionFlowMapper).countTransactionFlows(1L, "FLOW-001");
        verify(transactionFlowMapper).selectTransactionFlowPage(1L, "FLOW-001", 10L, 0L);
    }

    @Test
    @DisplayName("detailTransactionFlow should reject missing row with payment code")
    void detailTransactionFlow_missingRow_rejectsWithPaymentCode() {
        when(transactionFlowMapper.selectTransactionFlowDetail(1L, 390001L)).thenReturn(null);

        assertThatThrownBy(() -> service.detailTransactionFlow(390001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("pageExceptionOrders should query through mapper and fill labels")
    void pageExceptionOrders_usesMapperAndFillsLabels() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setKeyword(" EX-001 ");
        query.setStatusCode("PENDING");
        PaymentExceptionOrderVO row = new PaymentExceptionOrderVO();
        row.setId(400001L);
        row.setExceptionNo("EX-001");
        row.setExceptionType("AMOUNT_MISMATCH");
        row.setSeverity("HIGH");
        row.setHandleStatus("PENDING");
        when(exceptionOrderMapper.countExceptionOrders(1L, "EX-001", "PENDING")).thenReturn(1L);
        when(exceptionOrderMapper.selectExceptionOrderPage(1L, "EX-001", "PENDING", 10L, 0L)).thenReturn(List.of(row));

        var result = service.pageExceptionOrders(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList().get(0).getExceptionTypeName()).isEqualTo("金额不一致");
        assertThat(result.getList().get(0).getSeverityName()).isEqualTo("高");
        assertThat(result.getList().get(0).getHandleStatusName()).isEqualTo("待处理");
    }

    @Test
    @DisplayName("handleExceptionOrder should update controlled fields and record audit")
    void handleExceptionOrder_updatesFieldsAndRecordsAudit() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400001L);
        entity.setExceptionNo("EX-001");
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400001L)).thenReturn(entity);
        PaymentExceptionOrderVO detail = new PaymentExceptionOrderVO();
        detail.setId(400001L);
        detail.setExceptionNo("EX-001");
        detail.setHandleStatus("PROCESSING");
        when(exceptionOrderMapper.handleExceptionOrder(eq(1L), eq(400001L), eq("PROCESSING"), eq("ADD_EVIDENCE"),
                eq("补充通道凭据"), eq("已补充凭据，等待继续处理"), eq("channel-query-001"),
                eq(1001L), eq("admin"), any(LocalDateTime.class))).thenReturn(1);
        when(exceptionOrderMapper.selectExceptionOrderDetail(1L, 400001L)).thenReturn(detail);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400001L);
        command.setHandleAction("ADD_EVIDENCE");
        command.setHandleReason("补充通道凭据");
        command.setHandleResult("已补充凭据，等待继续处理");
        command.setHandleEvidence("channel-query-001");

        PaymentExceptionOrderVO result = service.handleExceptionOrder(command);

        assertThat(result.getHandleStatusName()).isEqualTo("处理中");
        verify(exceptionOrderMapper).handleExceptionOrder(eq(1L), eq(400001L), eq("PROCESSING"), eq("ADD_EVIDENCE"),
                eq("补充通道凭据"), eq("已补充凭据，等待继续处理"), eq("channel-query-001"),
                eq(1001L), eq("admin"), any(LocalDateTime.class));
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_HANDLE_EXCEPTION_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_EXCEPTION_ORDER,
                "EX-001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("listExceptionOrderActions should expose only implemented actions")
    void listExceptionOrderActions_exposesOnlyImplementedActions() {
        var actions = service.listExceptionOrderActions();

        assertThat(actions)
                .extracting(action -> action.getActionCode())
                .containsExactly("ACTIVE_QUERY", "CLOSE_PAYMENT_ORDER", "ACTIVE_REFUND_QUERY", "ADD_EVIDENCE", "MANUAL_CLOSE")
                .doesNotContain("REFUND_COMPENSATE", "NOTIFY_RETRY");
        assertThat(actions.stream()
                .filter(action -> "ACTIVE_QUERY".equals(action.getActionCode()))
                .findFirst()
                .orElseThrow()
                .getAllowedExceptionTypes())
                .containsExactlyInAnyOrder("DUPLICATE_PAYMENT", "PAY_TIMEOUT", "CHANNEL_FAILED")
                .doesNotContain("REFUND_MISMATCH");
        assertThat(actions.stream()
                .filter(action -> "ACTIVE_REFUND_QUERY".equals(action.getActionCode()))
                .findFirst()
                .orElseThrow()
                .getAllowedExceptionTypes())
                .containsExactly("REFUND_MISMATCH");
        assertThat(actions.stream()
                .filter(action -> "ADD_EVIDENCE".equals(action.getActionCode()))
                .findFirst()
                .orElseThrow()
                .getAllowedExceptionTypes())
                .contains("REFUND_MISMATCH");
    }

    @Test
    @DisplayName("handleExceptionOrder should reject unsupported refund compensate action")
    void handleExceptionOrder_refundCompensate_rejectsBeforeUpdate() {
        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400007L);
        command.setHandleAction("REFUND_COMPENSATE");
        command.setHandleReason("重复支付需要退款");
        command.setHandleResult("发起退款补偿");

        assertThatThrownBy(() -> service.handleExceptionOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessage("处理动作不受支持");
        verify(exceptionOrderMapper, never()).selectById(400007L);
        verify(exceptionOrderMapper, never()).handleExceptionOrder(
                any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any());
        verify(auditService, never()).record(
                eq(PaymentOperationAuditService.ACTION_HANDLE_EXCEPTION_ORDER),
                eq(PaymentOperationAuditService.RESOURCE_PAYMENT_EXCEPTION_ORDER),
                anyString(),
                eq(PaymentOperationAuditService.RESULT_SUCCESS));
    }

    @Test
    @DisplayName("handleExceptionOrder add evidence should keep exception order processing")
    void handleExceptionOrder_addEvidence_keepsProcessing() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400008L);
        entity.setExceptionNo("EX-EVIDENCE");
        entity.setRelatedOrderNo("PO2026061000000008");
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400008L)).thenReturn(entity);
        PaymentExceptionOrderVO detail = new PaymentExceptionOrderVO();
        detail.setId(400008L);
        detail.setExceptionNo("EX-EVIDENCE");
        detail.setHandleStatus("PROCESSING");
        when(exceptionOrderMapper.handleExceptionOrder(eq(1L), eq(400008L), eq("PROCESSING"), eq("ADD_EVIDENCE"),
                eq("补充通道凭据"), eq("已补充凭据，等待继续复核"), eq("mango-file:900008"),
                eq(1001L), eq("admin"), any(LocalDateTime.class))).thenReturn(1);
        when(exceptionOrderMapper.selectExceptionOrderDetail(1L, 400008L)).thenReturn(detail);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400008L);
        command.setHandleAction("ADD_EVIDENCE");
        command.setHandleReason("补充通道凭据");
        command.setHandleResult("已补充凭据，等待继续复核");
        command.setHandleEvidence("mango-file:900008");

        PaymentExceptionOrderVO result = service.handleExceptionOrder(command);

        assertThat(result.getHandleStatusName()).isEqualTo("处理中");
        verify(exceptionOrderMapper).handleExceptionOrder(eq(1L), eq(400008L), eq("PROCESSING"), eq("ADD_EVIDENCE"),
                eq("补充通道凭据"), eq("已补充凭据，等待继续复核"), eq("mango-file:900008"),
                eq(1001L), eq("admin"), any(LocalDateTime.class));
        verifyNoInteractions(channelOrderCloseService);
    }

    @Test
    @DisplayName("handleExceptionOrder manual close should only record handling and never mark orders success")
    void handleExceptionOrder_manualClose_doesNotTouchOrderSuccessState() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400006L);
        entity.setExceptionNo("EX-MANUAL-CLOSE");
        entity.setRelatedOrderNo("PO2026061000000006");
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400006L)).thenReturn(entity);
        PaymentExceptionOrderVO detail = new PaymentExceptionOrderVO();
        detail.setId(400006L);
        detail.setExceptionNo("EX-MANUAL-CLOSE");
        detail.setHandleStatus("CLOSED");
        when(exceptionOrderMapper.handleExceptionOrder(eq(1L), eq(400006L), eq("CLOSED"), eq("MANUAL_CLOSE"),
                eq("人工复核确认异常单可关闭"), eq("仅关闭异常处理记录，不修改订单状态"), eq("mango-file:900006"),
                eq(1001L), eq("admin"), any(LocalDateTime.class))).thenReturn(1);
        when(exceptionOrderMapper.selectExceptionOrderDetail(1L, 400006L)).thenReturn(detail);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400006L);
        command.setHandleAction("MANUAL_CLOSE");
        command.setHandleReason("人工复核确认异常单可关闭");
        command.setHandleResult("仅关闭异常处理记录，不修改订单状态");
        command.setHandleEvidence("mango-file:900006");

        PaymentExceptionOrderVO result = service.handleExceptionOrder(command);

        assertThat(result.getHandleStatusName()).isEqualTo("已关闭");
        verify(exceptionOrderMapper).handleExceptionOrder(eq(1L), eq(400006L), eq("CLOSED"), eq("MANUAL_CLOSE"),
                eq("人工复核确认异常单可关闭"), eq("仅关闭异常处理记录，不修改订单状态"), eq("mango-file:900006"),
                eq(1001L), eq("admin"), any(LocalDateTime.class));
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_HANDLE_EXCEPTION_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_EXCEPTION_ORDER,
                "EX-MANUAL-CLOSE",
                PaymentOperationAuditService.RESULT_SUCCESS);
        verifyNoInteractions(paymentOrderMapper, refundOrderMapper, businessOrderMapper,
                channelSyncService, channelOrderCloseService);
    }

    @Test
    @DisplayName("handleExceptionOrder should trigger channel query for numgen payment order")
    void handleExceptionOrder_activeQueryNumgenPaymentOrder_triggersChannelQuery() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400012L);
        entity.setExceptionNo("EX2026061000000001");
        entity.setRelatedOrderNo("PO2026061000000001");
        entity.setExceptionType(PaymentExceptionOrderService.TYPE_CHANNEL_FAILED);
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400012L)).thenReturn(entity);
        when(channelSyncService.syncPaymentStatus("PO2026061000000001"))
                .thenReturn(new PaymentChannelSyncService.PaymentSyncResult(
                        "PO2026061000000001",
                        "FAILED",
                        null,
                        false,
                        2L,
                        "NO_QUERY_TERMINAL"));
        PaymentExceptionOrderVO detail = new PaymentExceptionOrderVO();
        detail.setId(400012L);
        detail.setExceptionNo("EX2026061000000001");
        detail.setHandleStatus("HANDLED");
        when(exceptionOrderMapper.handleExceptionOrder(eq(1L), eq(400012L), eq("HANDLED"), eq("ACTIVE_QUERY"),
                eq("查单确认支付失败"),
                eq("根据通道查单结果处理异常；查单结果：支付订单 PO2026061000000001 当前状态 FAILED，本地状态未变化"),
                eq("channel-query-numgen"), eq(1001L), eq("admin"), any(LocalDateTime.class))).thenReturn(1);
        when(exceptionOrderMapper.selectExceptionOrderDetail(1L, 400012L)).thenReturn(detail);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400012L);
        command.setHandleAction("ACTIVE_QUERY");
        command.setHandleReason("查单确认支付失败");
        command.setHandleResult("根据通道查单结果处理异常");
        command.setHandleEvidence("channel-query-numgen");

        service.handleExceptionOrder(command);

        verify(channelSyncService).syncPaymentStatus("PO2026061000000001");
        verify(exceptionOrderMapper).handleExceptionOrder(eq(1L), eq(400012L), eq("HANDLED"), eq("ACTIVE_QUERY"),
                eq("查单确认支付失败"),
                eq("根据通道查单结果处理异常；查单结果：支付订单 PO2026061000000001 当前状态 FAILED，本地状态未变化"),
                eq("channel-query-numgen"), eq(1001L), eq("admin"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("handleExceptionOrder should reject non numgen payment order number")
    void handleExceptionOrder_activeQueryNonNumgenPaymentOrder_rejects() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400013L);
        entity.setExceptionNo("EX202606101780896910696123456");
        entity.setRelatedOrderNo("PO202606101780896910696123456");
        entity.setExceptionType(PaymentExceptionOrderService.TYPE_PAY_TIMEOUT);
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400013L)).thenReturn(entity);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400013L);
        command.setHandleAction("ACTIVE_QUERY");
        command.setHandleReason("支付超时后主动查单");
        command.setHandleResult("根据通道查单结果处理异常");

        assertThatThrownBy(() -> service.handleExceptionOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessage("主动查单动作必须关联支付订单号");

        verify(channelSyncService, never()).syncPaymentStatus("PO202606101780896910696123456");
        verify(exceptionOrderMapper, never()).handleExceptionOrder(
                any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("handleExceptionOrder should reject active query for non payment order reference")
    void handleExceptionOrder_activeQueryNonPaymentReference_rejects() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400003L);
        entity.setExceptionNo("EX-003");
        entity.setRelatedOrderNo("PO-EX-E2E-202606060001");
        entity.setExceptionType(PaymentExceptionOrderService.TYPE_CHANNEL_FAILED);
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400003L)).thenReturn(entity);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400003L);
        command.setHandleAction("ACTIVE_QUERY");
        command.setHandleReason("查单确认通道未成功");
        command.setHandleResult("维持原订单状态并关闭异常");

        assertThatThrownBy(() -> service.handleExceptionOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessage("主动查单动作必须关联支付订单号");

        verify(channelSyncService, never()).syncPaymentStatus("PO-EX-E2E-202606060001");
        verify(exceptionOrderMapper, never()).handleExceptionOrder(
                any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("handleExceptionOrder should reject payment actions for refund exception")
    void handleExceptionOrder_paymentActionForRefundException_rejects() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400010L);
        entity.setExceptionNo("EX-REFUND");
        entity.setRelatedOrderNo("RO2026061000000010");
        entity.setExceptionType(PaymentExceptionOrderService.TYPE_REFUND_MISMATCH);
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400010L)).thenReturn(entity);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400010L);
        command.setHandleAction("ACTIVE_QUERY");
        command.setHandleReason("查单确认退款结果");
        command.setHandleResult("退款异常不应触发支付查单");

        assertThatThrownBy(() -> service.handleExceptionOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessage("处理动作不适用于当前异常类型");

        verifyNoInteractions(channelOrderCloseService);
        verify(exceptionOrderMapper, never()).handleExceptionOrder(
                any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("handleExceptionOrder should trigger refund query for refund mismatch")
    void handleExceptionOrder_activeRefundQuery_triggersChannelRefundQuery() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400014L);
        entity.setExceptionNo("EX-REFUND-QUERY");
        entity.setRelatedOrderNo("RO2026061000000014");
        entity.setExceptionType(PaymentExceptionOrderService.TYPE_REFUND_MISMATCH);
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400014L)).thenReturn(entity);
        when(channelSyncService.syncRefundStatus("RO2026061000000014"))
                .thenReturn(new PaymentChannelSyncService.RefundSyncResult(
                        "RO2026061000000014",
                        "SUCCESS",
                        "RFLOW2026061000000014",
                        true,
                        2L,
                        "UPDATED"));
        PaymentExceptionOrderVO detail = new PaymentExceptionOrderVO();
        detail.setId(400014L);
        detail.setExceptionNo("EX-REFUND-QUERY");
        detail.setHandleStatus("HANDLED");
        when(exceptionOrderMapper.handleExceptionOrder(eq(1L), eq(400014L), eq("HANDLED"), eq("ACTIVE_REFUND_QUERY"),
                eq("查退款确认通道结果"),
                eq("根据通道查退款结果处理异常；查退款结果：退款订单 RO2026061000000014 当前状态 SUCCESS，已按通道结果推进"),
                eq("refund-query-evidence"), eq(1001L), eq("admin"), any(LocalDateTime.class))).thenReturn(1);
        when(exceptionOrderMapper.selectExceptionOrderDetail(1L, 400014L)).thenReturn(detail);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400014L);
        command.setHandleAction("ACTIVE_REFUND_QUERY");
        command.setHandleReason("查退款确认通道结果");
        command.setHandleResult("根据通道查退款结果处理异常");
        command.setHandleEvidence("refund-query-evidence");

        service.handleExceptionOrder(command);

        verify(channelSyncService).syncRefundStatus("RO2026061000000014");
        verify(exceptionOrderMapper).handleExceptionOrder(eq(1L), eq(400014L), eq("HANDLED"), eq("ACTIVE_REFUND_QUERY"),
                eq("查退款确认通道结果"),
                eq("根据通道查退款结果处理异常；查退款结果：退款订单 RO2026061000000014 当前状态 SUCCESS，已按通道结果推进"),
                eq("refund-query-evidence"), eq(1001L), eq("admin"), any(LocalDateTime.class));
        verifyNoInteractions(channelOrderCloseService);
    }

    @Test
    @DisplayName("handleExceptionOrder should trigger payment close for controlled close action")
    void handleExceptionOrder_closePaymentOrder_triggersChannelClose() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400004L);
        entity.setExceptionNo("EX-004");
        entity.setRelatedOrderNo("PO2026061000000002");
        entity.setExceptionType(PaymentExceptionOrderService.TYPE_PAY_TIMEOUT);
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400004L)).thenReturn(entity);
        when(channelOrderCloseService.closePaymentOrder("PO2026061000000002"))
                .thenReturn(new PaymentChannelOrderCloseService.CloseResult(
                        "PO2026061000000002",
                        "CLOSED",
                        true));
        PaymentExceptionOrderVO detail = new PaymentExceptionOrderVO();
        detail.setId(400004L);
        detail.setExceptionNo("EX-004");
        detail.setHandleStatus("CLOSED");
        when(exceptionOrderMapper.handleExceptionOrder(eq(1L), eq(400004L), eq("CLOSED"), eq("CLOSE_PAYMENT_ORDER"),
                eq("通道确认未支付，执行关单"),
                eq("按通道结果关闭支付订单；关单结果：支付订单 PO2026061000000002 当前状态 CLOSED，已关闭"),
                eq(null), eq(1001L), eq("admin"), any(LocalDateTime.class))).thenReturn(1);
        when(exceptionOrderMapper.selectExceptionOrderDetail(1L, 400004L)).thenReturn(detail);

        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400004L);
        command.setHandleAction("CLOSE_PAYMENT_ORDER");
        command.setHandleReason("通道确认未支付，执行关单");
        command.setHandleResult("按通道结果关闭支付订单");

        service.handleExceptionOrder(command);

        verify(channelOrderCloseService).closePaymentOrder("PO2026061000000002");
        verify(channelSyncService, never()).syncPaymentStatus("PO2026061000000002");
        verify(exceptionOrderMapper).handleExceptionOrder(eq(1L), eq(400004L), eq("CLOSED"), eq("CLOSE_PAYMENT_ORDER"),
                eq("通道确认未支付，执行关单"),
                eq("按通道结果关闭支付订单；关单结果：支付订单 PO2026061000000002 当前状态 CLOSED，已关闭"),
                eq(null), eq(1001L), eq("admin"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("handleExceptionOrder should reject terminal status")
    void handleExceptionOrder_terminalStatus_rejects() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400001L);
        entity.setExceptionNo("EX-001");
        entity.setTenantId(1L);
        entity.setHandleStatus("HANDLED");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400001L)).thenReturn(entity);
        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400001L);
        command.setHandleAction("ACTIVE_QUERY");
        command.setHandleReason("查单确认");
        command.setHandleResult("无需继续处理");

        assertThatThrownBy(() -> service.handleExceptionOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_EXCEPTION_ORDER_HANDLE_STATUS_INVALID.getMessage());
    }

    @Test
    @DisplayName("handleExceptionOrder should reject cross tenant row before update")
    void handleExceptionOrder_crossTenant_rejectsBeforeUpdate() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400005L);
        entity.setExceptionNo("EX-CROSS");
        entity.setTenantId(2L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400005L)).thenReturn(entity);
        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400005L);
        command.setHandleAction("ACTIVE_QUERY");
        command.setHandleReason("跨租户处理");
        command.setHandleResult("不允许处理");

        assertThatThrownBy(() -> service.handleExceptionOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND.getMessage());
        verify(exceptionOrderMapper, never()).handleExceptionOrder(
                any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any());
        verify(auditService, never()).record(
                PaymentOperationAuditService.ACTION_HANDLE_EXCEPTION_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_EXCEPTION_ORDER,
                "EX-CROSS",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("handleExceptionOrder should reject when controlled update misses current status")
    void handleExceptionOrder_controlledUpdateMiss_rejectsBeforeAudit() {
        PaymentExceptionOrderEntity entity = new PaymentExceptionOrderEntity();
        entity.setId(400009L);
        entity.setExceptionNo("EX-STATE-CHANGED");
        entity.setTenantId(1L);
        entity.setHandleStatus("PENDING");
        entity.setDelFlag(0);
        when(exceptionOrderMapper.selectById(400009L)).thenReturn(entity);
        when(exceptionOrderMapper.handleExceptionOrder(eq(1L), eq(400009L), eq("PROCESSING"), eq("ADD_EVIDENCE"),
                eq("补充材料"), eq("等待继续处理"), eq("mango-file:900009"),
                eq(1001L), eq("admin"), any(LocalDateTime.class))).thenReturn(0);
        HandlePaymentExceptionOrderCommand command = new HandlePaymentExceptionOrderCommand();
        command.setId(400009L);
        command.setHandleAction("ADD_EVIDENCE");
        command.setHandleReason("补充材料");
        command.setHandleResult("等待继续处理");
        command.setHandleEvidence("mango-file:900009");

        assertThatThrownBy(() -> service.handleExceptionOrder(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_EXCEPTION_ORDER_HANDLE_STATUS_INVALID.getMessage());
        verify(auditService, never()).record(
                eq(PaymentOperationAuditService.ACTION_HANDLE_EXCEPTION_ORDER),
                eq(PaymentOperationAuditService.RESOURCE_PAYMENT_EXCEPTION_ORDER),
                anyString(),
                eq(PaymentOperationAuditService.RESULT_SUCCESS));
    }

    @Test
    @DisplayName("pageNotificationRecords should query through mapper and fill labels")
    void pageNotificationRecords_usesMapperAndFillsLabels() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setKeyword(" NT-001 ");
        query.setStatusCode("FAILED");
        PaymentNotificationRecordVO row = new PaymentNotificationRecordVO();
        row.setId(410001L);
        row.setNotificationNo("NT-001");
        row.setNotificationType("PAYMENT_SUCCESS");
        row.setNotifyStatus("FAILED");
        when(notificationRecordMapper.countNotificationRecords(1L, "NT-001", "FAILED")).thenReturn(1L);
        when(notificationRecordMapper.selectNotificationRecordPage(1L, "NT-001", "FAILED", 10L, 0L)).thenReturn(List.of(row));

        var result = service.pageNotificationRecords(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList().get(0).getNotificationTypeName()).isEqualTo("支付成功通知");
        assertThat(result.getList().get(0).getNotifyStatusName()).isEqualTo("通知失败");
        verify(notificationRecordMapper).countNotificationRecords(1L, "NT-001", "FAILED");
        verify(notificationRecordMapper).selectNotificationRecordPage(1L, "NT-001", "FAILED", 10L, 0L);
    }

    @Test
    @DisplayName("retryNotificationRecord should update retry fields and record audit")
    void retryNotificationRecord_updatesRetryFieldsAndRecordsAudit() {
        PaymentNotificationRecordEntity entity = new PaymentNotificationRecordEntity();
        entity.setId(410001L);
        entity.setNotificationNo("NT-001");
        entity.setTenantId(1L);
        entity.setNotifyStatus("FAILED");
        entity.setRetryTimes(2);
        entity.setPayloadJson("{\"notifyNo\":\"NT-001\"}");
        entity.setDelFlag(0);
        when(notificationRecordMapper.selectById(410001L)).thenReturn(entity);
        when(notificationRecordMapper.manualRetryNotificationRecord(
                eq(1L),
                eq(410001L),
                eq("业务系统已恢复，人工补偿推送"),
                eq("人工补偿重推已登记，等待通知任务执行 ACK"),
                any(LocalDateTime.class),
                eq(1001L),
                eq("admin"))).thenReturn(1);
        PaymentNotificationRecordVO detail = new PaymentNotificationRecordVO();
        detail.setId(410001L);
        detail.setNotificationNo("NT-001");
        detail.setNotificationType("PAYMENT_FAILED");
        detail.setNotifyStatus("RETRYING");
        when(notificationRecordMapper.selectNotificationRecordDetail(1L, 410001L)).thenReturn(detail);

        RetryPaymentNotificationRecordCommand command = new RetryPaymentNotificationRecordCommand();
        command.setId(410001L);
        command.setRetryReason("业务系统已恢复，人工补偿推送");

        PaymentNotificationRecordVO result = service.retryNotificationRecord(command);

        assertThat(result.getNotifyStatusName()).isEqualTo("重试中");
        verify(notificationRecordMapper).manualRetryNotificationRecord(
                eq(1L),
                eq(410001L),
                eq("业务系统已恢复，人工补偿推送"),
                eq("人工补偿重推已登记，等待通知任务执行 ACK"),
                any(LocalDateTime.class),
                eq(1001L),
                eq("admin"));
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_RETRY_NOTIFICATION_RECORD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_NOTIFICATION_RECORD,
                "NT-001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("retryNotificationRecord should reject success status")
    void retryNotificationRecord_successStatus_rejects() {
        PaymentNotificationRecordEntity entity = new PaymentNotificationRecordEntity();
        entity.setId(410001L);
        entity.setNotificationNo("NT-001");
        entity.setTenantId(1L);
        entity.setNotifyStatus("SUCCESS");
        entity.setDelFlag(0);
        entity.setPayloadJson("{\"notifyNo\":\"NT-001\"}");
        when(notificationRecordMapper.selectById(410001L)).thenReturn(entity);
        RetryPaymentNotificationRecordCommand command = new RetryPaymentNotificationRecordCommand();
        command.setId(410001L);
        command.setRetryReason("重复重推");

        assertThatThrownBy(() -> service.retryNotificationRecord(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID.getMessage());
    }

    @Test
    @DisplayName("retryNotificationRecord should reject record without payload snapshot")
    void retryNotificationRecord_missingPayload_rejects() {
        PaymentNotificationRecordEntity entity = new PaymentNotificationRecordEntity();
        entity.setId(410001L);
        entity.setNotificationNo("NT-001");
        entity.setTenantId(1L);
        entity.setNotifyStatus("FAILED");
        entity.setDelFlag(0);
        when(notificationRecordMapper.selectById(410001L)).thenReturn(entity);
        RetryPaymentNotificationRecordCommand command = new RetryPaymentNotificationRecordCommand();
        command.setId(410001L);
        command.setRetryReason("补偿重推");

        assertThatThrownBy(() -> service.retryNotificationRecord(command))
                .isInstanceOf(BizException.class)
                .hasMessage("通知报文快照不存在，不能人工重推");
        verify(notificationRecordMapper, never()).manualRetryNotificationRecord(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("retryNotificationRecord should reject cross tenant row before update")
    void retryNotificationRecord_crossTenant_rejectsBeforeUpdate() {
        PaymentNotificationRecordEntity entity = new PaymentNotificationRecordEntity();
        entity.setId(410002L);
        entity.setNotificationNo("NT-CROSS");
        entity.setTenantId(2L);
        entity.setNotifyStatus("FAILED");
        entity.setDelFlag(0);
        entity.setPayloadJson("{\"notifyNo\":\"NT-CROSS\"}");
        when(notificationRecordMapper.selectById(410002L)).thenReturn(entity);
        RetryPaymentNotificationRecordCommand command = new RetryPaymentNotificationRecordCommand();
        command.setId(410002L);
        command.setRetryReason("跨租户重推");

        assertThatThrownBy(() -> service.retryNotificationRecord(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND.getMessage());
        verify(notificationRecordMapper, never()).manualRetryNotificationRecord(any(), any(), any(), any(), any(), any(), any());
        verify(auditService, never()).record(
                PaymentOperationAuditService.ACTION_RETRY_NOTIFICATION_RECORD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_NOTIFICATION_RECORD,
                "NT-CROSS",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("deliverDueNotificationRecords should dispatch through notification service and record audit")
    void deliverDueNotificationRecords_dispatchesAndRecordsAudit() {
        when(notificationService.deliverDueNotificationRecords(20L)).thenReturn(3);

        int result = service.deliverDueNotificationRecords(20L);

        assertThat(result).isEqualTo(3);
        verify(notificationService).deliverDueNotificationRecords(20L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELIVER_DUE_NOTIFICATION_RECORDS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_NOTIFICATION_RECORD,
                "DUE_NOTIFICATION_RECORDS",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("expireOpenPaymentOrders should close expired open orders and record batch audit")
    void expireOpenPaymentOrders_closesExpiredOrdersAndRecordsAudit() {
        PaymentOrderEntity changed = paymentOrder("PO2026061000000011");
        PaymentOrderEntity skipped = paymentOrder("PO2026061000000012");
        when(paymentOrderMapper.selectExpiredOpenPaymentOrders(eq(1L), any(LocalDateTime.class), eq(20L)))
                .thenReturn(List.of(changed, skipped));
        when(channelOrderCloseService.closeExpiredPaymentOrder("PO2026061000000011"))
                .thenReturn(new PaymentChannelOrderCloseService.CloseResult("PO2026061000000011", "CLOSED", true));
        when(channelOrderCloseService.closeExpiredPaymentOrder("PO2026061000000012"))
                .thenReturn(new PaymentChannelOrderCloseService.CloseResult("PO2026061000000012", "CLOSED", false));

        var result = service.expireOpenPaymentOrders(20L);

        assertThat(result.getScannedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isZero();
        verify(channelOrderCloseService).closeExpiredPaymentOrder("PO2026061000000011");
        verify(channelOrderCloseService).closeExpiredPaymentOrder("PO2026061000000012");
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_EXPIRE_OPEN_PAYMENT_ORDERS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                "EXPIRE_OPEN_PAYMENT_ORDERS:s=2,ok=1,skip=1,fail=0",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("expireOpenPaymentOrders should count per order failures without aborting batch")
    void expireOpenPaymentOrders_singleFailure_continuesBatch() {
        PaymentOrderEntity failed = paymentOrder("PO2026061000000013");
        PaymentOrderEntity changed = paymentOrder("PO2026061000000014");
        when(paymentOrderMapper.selectExpiredOpenPaymentOrders(eq(1L), any(LocalDateTime.class), eq(20L)))
                .thenReturn(List.of(failed, changed));
        when(channelOrderCloseService.closeExpiredPaymentOrder("PO2026061000000013"))
                .thenThrow(new IllegalStateException("state changed"));
        when(channelOrderCloseService.closeExpiredPaymentOrder("PO2026061000000014"))
                .thenReturn(new PaymentChannelOrderCloseService.CloseResult("PO2026061000000014", "CLOSED", true));

        var result = service.expireOpenPaymentOrders(20L);

        assertThat(result.getScannedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isZero();
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_EXPIRE_OPEN_PAYMENT_ORDERS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                "EXPIRE_OPEN_PAYMENT_ORDERS:s=2,ok=1,skip=0,fail=1",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("queryProcessingPaymentOrders should query processing orders and record batch audit")
    void queryProcessingPaymentOrders_queriesOrdersAndRecordsAudit() {
        PaymentOrderEntity changed = paymentOrder("PO2026061000000015");
        PaymentOrderEntity skipped = paymentOrder("PO2026061000000016");
        when(paymentOrderMapper.selectProcessingPaymentOrders(1L, 20L)).thenReturn(List.of(changed, skipped));
        when(channelSyncService.syncPaymentStatus("PO2026061000000015"))
                .thenReturn(new PaymentChannelSyncService.PaymentSyncResult(
                        "PO2026061000000015", "SUCCESS", "FLOW202606100002", true, 1L, "UPDATED"));
        when(channelSyncService.syncPaymentStatus("PO2026061000000016"))
                .thenReturn(new PaymentChannelSyncService.PaymentSyncResult(
                        "PO2026061000000016", "PAYING", null, false, 1L, "NO_CHANGE_PROCESSING"));

        var result = service.queryProcessingPaymentOrders(20L);

        assertThat(result.getScannedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isZero();
        verify(channelSyncService).syncPaymentStatus("PO2026061000000015");
        verify(channelSyncService).syncPaymentStatus("PO2026061000000016");
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_QUERY_PROCESSING_PAYMENT_ORDERS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                "QUERY_PROCESSING_PAYMENT_ORDERS:s=2,ok=1,skip=1,fail=0",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("task endpoints should reject limit outside one to one hundred")
    void paymentTasks_invalidLimit_rejects() {
        assertThatThrownBy(() -> service.expireOpenPaymentOrders(0L))
                .isInstanceOf(BizException.class)
                .hasMessage("任务批次大小必须在 1 到 100 之间");
        assertThatThrownBy(() -> service.queryProcessingPaymentOrders(101L))
                .isInstanceOf(BizException.class)
                .hasMessage("任务批次大小必须在 1 到 100 之间");
    }

    @Test
    @DisplayName("pageDifferences should query through mapper and fill labels")
    void pageDifferences_usesMapperAndFillsLabels() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setKeyword(" DIFF-001 ");
        query.setStatusCode("PENDING");
        PaymentDifferenceVO row = new PaymentDifferenceVO();
        row.setId(420001L);
        row.setDifferenceNo("DIFF-001");
        row.setDifferenceType("LOCAL_SUCCESS_CHANNEL_MISSING");
        row.setProcessStatus("PENDING");
        when(differenceMapper.countDifferences(1L, "DIFF-001", "PENDING")).thenReturn(1L);
        when(differenceMapper.selectDifferencePage(1L, "DIFF-001", "PENDING", 10L, 0L)).thenReturn(List.of(row));

        var result = service.pageDifferences(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList().get(0).getDifferenceTypeName()).isEqualTo("我方成功通道无单");
        assertThat(result.getList().get(0).getProcessStatusName()).isEqualTo("待处理");
        verify(differenceMapper).countDifferences(1L, "DIFF-001", "PENDING");
        verify(differenceMapper).selectDifferencePage(1L, "DIFF-001", "PENDING", 10L, 0L);
    }

    @Test
    @DisplayName("pageOperationAudits should query through mapper with tenant and result filter")
    void pageOperationAudits_usesMapperAndResultFilter() {
        PaymentConfigPageQuery query = new PaymentConfigPageQuery();
        query.setPage(2L);
        query.setSize(20L);
        query.setKeyword(" DELETE_APPLICATION ");
        query.setStatusCode("REJECTED");
        PaymentOperationAuditVO row = new PaymentOperationAuditVO();
        row.setId(430001L);
        row.setOperatorName("admin");
        row.setOperationAction("DELETE_APPLICATION");
        row.setResourceType("PAYMENT_APPLICATION");
        row.setResourceId("app_order_center");
        row.setOperationResult("REJECTED");
        row.setOperationTime(LocalDateTime.of(2026, 6, 2, 10, 0));
        when(operationAuditMapper.countOperationAudits(1L, "DELETE_APPLICATION", "REJECTED")).thenReturn(1L);
        when(operationAuditMapper.selectOperationAuditPage(1L, "DELETE_APPLICATION", "REJECTED", 20L, 20L)).thenReturn(List.of(row));

        var result = service.pageOperationAudits(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(20L);
        assertThat(result.getList().get(0).getOperationAction()).isEqualTo("DELETE_APPLICATION");
        assertThat(result.getList().get(0).getOperationResult()).isEqualTo("REJECTED");
        verify(operationAuditMapper).countOperationAudits(1L, "DELETE_APPLICATION", "REJECTED");
        verify(operationAuditMapper).selectOperationAuditPage(1L, "DELETE_APPLICATION", "REJECTED", 20L, 20L);
    }

    @Test
    @DisplayName("handleDifference should update controlled fields and record audit")
    void handleDifference_updatesFieldsAndRecordsAudit() {
        PaymentDifferenceEntity entity = new PaymentDifferenceEntity();
        entity.setId(420001L);
        entity.setDifferenceNo("DIFF-001");
        entity.setTenantId(1L);
        entity.setProcessStatus("PENDING");
        entity.setDelFlag(0);
        when(differenceMapper.selectById(420001L)).thenReturn(entity);
        PaymentDifferenceVO detail = new PaymentDifferenceVO();
        detail.setId(420001L);
        detail.setDifferenceNo("DIFF-001");
        detail.setProcessStatus("CLOSED");
        detail.setDifferenceType("AMOUNT_MISMATCH");
        when(differenceMapper.selectDifferenceDetail(1L, 420001L)).thenReturn(detail);
        when(differenceMapper.handleDifference(
                eq(1L), eq(420001L), eq("CLOSED"), eq("CLOSE"),
                eq("账单与本地记录已人工核对"), eq("关闭差异，不修改订单状态"),
                eq("mango-file:900001"), any(), anyString(), eq(1001L), eq("admin"), any()))
                .thenReturn(1);

        HandlePaymentDifferenceCommand command = new HandlePaymentDifferenceCommand();
        command.setId(420001L);
        command.setProcessAction("CLOSE");
        command.setProcessReason("账单与本地记录已人工核对");
        command.setProcessResult("关闭差异，不修改订单状态");
        command.setProcessEvidence("mango-file:900001");

        PaymentDifferenceVO result = service.handleDifference(command);

        assertThat(result.getProcessStatusName()).isEqualTo("已关闭");
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("ADJUST_NOTE");
        assertThat(flowCaptor.getValue().getAmount()).isZero();
        assertThat(flowCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(flowCaptor.getValue().getCreatedBy()).isEqualTo(1001L);
        assertThat(flowCaptor.getValue().getFlowNo()).startsWith("AF");
        verify(differenceMapper).handleDifference(
                eq(1L),
                eq(420001L),
                eq("CLOSED"),
                eq("CLOSE"),
                eq("账单与本地记录已人工核对"),
                eq("关闭差异，不修改订单状态"),
                eq("mango-file:900001"),
                eq(flowCaptor.getValue().getId()),
                eq(flowCaptor.getValue().getFlowNo()),
                eq(1001L),
                eq("admin"),
                any());
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_HANDLE_DIFFERENCE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_DIFFERENCE,
                "DIFF-001",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("handleDifference supplement order should only record difference handling and never mark orders success")
    void handleDifference_supplementOrder_doesNotTouchOrderSuccessState() {
        PaymentDifferenceEntity entity = new PaymentDifferenceEntity();
        entity.setId(420003L);
        entity.setDifferenceNo("DIFF-SUPPLEMENT");
        entity.setTenantId(1L);
        entity.setProcessStatus("PENDING");
        entity.setDelFlag(0);
        when(differenceMapper.selectById(420003L)).thenReturn(entity);
        PaymentDifferenceVO detail = new PaymentDifferenceVO();
        detail.setId(420003L);
        detail.setDifferenceNo("DIFF-SUPPLEMENT");
        detail.setProcessStatus("HANDLED");
        detail.setDifferenceType("CHANNEL_SUCCESS_LOCAL_MISSING");
        when(differenceMapper.selectDifferenceDetail(1L, 420003L)).thenReturn(detail);
        when(differenceMapper.handleDifference(
                eq(1L), eq(420003L), eq("PROCESSING"), eq("SUPPLEMENT_ORDER"),
                eq("通道账单存在但本地未匹配，挂起待认领"), eq("仅登记差异处理，不人工置成功"),
                eq("mango-file:920003"), any(), anyString(), eq(1001L), eq("admin"), any()))
                .thenReturn(1);

        HandlePaymentDifferenceCommand command = new HandlePaymentDifferenceCommand();
        command.setId(420003L);
        command.setProcessAction("SUPPLEMENT_ORDER");
        command.setProcessReason("通道账单存在但本地未匹配，挂起待认领");
        command.setProcessResult("仅登记差异处理，不人工置成功");
        command.setProcessEvidence("mango-file:920003");

        PaymentDifferenceVO result = service.handleDifference(command);

        assertThat(result.getProcessStatusName()).isEqualTo("已处理");
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("ADJUST_NOTE");
        assertThat(flowCaptor.getValue().getAmount()).isZero();
        verify(differenceMapper).handleDifference(
                eq(1L),
                eq(420003L),
                eq("PROCESSING"),
                eq("SUPPLEMENT_ORDER"),
                eq("通道账单存在但本地未匹配，挂起待认领"),
                eq("仅登记差异处理，不人工置成功"),
                eq("mango-file:920003"),
                eq(flowCaptor.getValue().getId()),
                eq(flowCaptor.getValue().getFlowNo()),
                eq(1001L),
                eq("admin"),
                any());
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_HANDLE_DIFFERENCE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_DIFFERENCE,
                "DIFF-SUPPLEMENT",
                PaymentOperationAuditService.RESULT_SUCCESS);
        verifyNoInteractions(paymentOrderMapper, refundOrderMapper, businessOrderMapper,
                channelSyncService, channelOrderCloseService, channelSyncService, notificationService);
    }

    @Test
    @DisplayName("handleDifference should reject terminal status")
    void handleDifference_terminalStatus_rejects() {
        PaymentDifferenceEntity entity = new PaymentDifferenceEntity();
        entity.setId(420001L);
        entity.setDifferenceNo("DIFF-001");
        entity.setTenantId(1L);
        entity.setProcessStatus("HANDLED");
        entity.setDelFlag(0);
        when(differenceMapper.selectById(420001L)).thenReturn(entity);
        HandlePaymentDifferenceCommand command = new HandlePaymentDifferenceCommand();
        command.setId(420001L);
        command.setProcessAction("ACTIVE_QUERY");
        command.setProcessReason("重复处理");
        command.setProcessResult("不允许重复处理");

        assertThatThrownBy(() -> service.handleDifference(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_DIFFERENCE_HANDLE_STATUS_INVALID.getMessage());
    }

    @Test
    @DisplayName("handleDifference should reject cross tenant row before update")
    void handleDifference_crossTenant_rejectsBeforeUpdate() {
        PaymentDifferenceEntity entity = new PaymentDifferenceEntity();
        entity.setId(420002L);
        entity.setDifferenceNo("DIFF-CROSS");
        entity.setTenantId(2L);
        entity.setProcessStatus("PENDING");
        entity.setDelFlag(0);
        when(differenceMapper.selectById(420002L)).thenReturn(entity);
        HandlePaymentDifferenceCommand command = new HandlePaymentDifferenceCommand();
        command.setId(420002L);
        command.setProcessAction("CLOSE");
        command.setProcessReason("跨租户处理");
        command.setProcessResult("不允许处理");

        assertThatThrownBy(() -> service.handleDifference(command))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_DIFFERENCE_NOT_FOUND.getMessage());
        verify(differenceMapper, never()).updateById(entity);
        verify(differenceMapper, never()).handleDifference(
                any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any(), anyString(), any());
        verify(auditService, never()).record(
                PaymentOperationAuditService.ACTION_HANDLE_DIFFERENCE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_DIFFERENCE,
                "DIFF-CROSS",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("handleDifference active query should call channel query before recording handling")
    void handleDifference_activeQuery_callsChannelQuery() {
        PaymentDifferenceEntity entity = new PaymentDifferenceEntity();
        entity.setId(420004L);
        entity.setDifferenceNo("DIFF-ACTIVE-QUERY");
        entity.setRelatedOrderNo("PO2026061000000002");
        entity.setTenantId(1L);
        entity.setProcessStatus("PENDING");
        entity.setDelFlag(0);
        when(differenceMapper.selectById(420004L)).thenReturn(entity);
        when(channelSyncService.syncPaymentStatus("PO2026061000000002"))
                .thenReturn(new PaymentChannelSyncService.PaymentSyncResult(
                        "PO2026061000000002",
                        "SUCCESS",
                        "FLOW202606100002",
                        true,
                        1L,
                        "UPDATED"));
        PaymentDifferenceVO detail = new PaymentDifferenceVO();
        detail.setId(420004L);
        detail.setDifferenceNo("DIFF-ACTIVE-QUERY");
        detail.setProcessStatus("HANDLED");
        detail.setDifferenceType("STATUS_MISMATCH");
        when(differenceMapper.selectDifferenceDetail(1L, 420004L)).thenReturn(detail);
        when(differenceMapper.handleDifference(
                eq(1L), eq(420004L), eq("HANDLED"), eq("ACTIVE_QUERY"),
                eq("对账状态不一致，执行主动查单"), eq("已执行主动查单；主动查单结果：支付订单 PO2026061000000002 当前状态 SUCCESS，已按通道结果推进"),
                eq(null), any(), anyString(), eq(1001L), eq("admin"), any()))
                .thenReturn(1);

        HandlePaymentDifferenceCommand command = new HandlePaymentDifferenceCommand();
        command.setId(420004L);
        command.setProcessAction("ACTIVE_QUERY");
        command.setProcessReason("对账状态不一致，执行主动查单");
        command.setProcessResult("已执行主动查单");

        PaymentDifferenceVO result = service.handleDifference(command);

        assertThat(result.getProcessStatusName()).isEqualTo("已处理");
        verify(channelSyncService).syncPaymentStatus("PO2026061000000002");
        verify(differenceMapper).handleDifference(
                eq(1L),
                eq(420004L),
                eq("HANDLED"),
                eq("ACTIVE_QUERY"),
                eq("对账状态不一致，执行主动查单"),
                eq("已执行主动查单；主动查单结果：支付订单 PO2026061000000002 当前状态 SUCCESS，已按通道结果推进"),
                eq(null),
                any(),
                anyString(),
                eq(1001L),
                eq("admin"),
                any());
    }

    @Test
    @DisplayName("handleDifference active query should reject non local order number")
    void handleDifference_activeQueryRejectsNonLocalOrderNo() {
        PaymentDifferenceEntity entity = new PaymentDifferenceEntity();
        entity.setId(420005L);
        entity.setDifferenceNo("DIFF-CHANNEL-MISSING");
        entity.setRelatedOrderNo("CHANNEL-TRADE-001");
        entity.setTenantId(1L);
        entity.setProcessStatus("PENDING");
        entity.setDelFlag(0);
        when(differenceMapper.selectById(420005L)).thenReturn(entity);

        HandlePaymentDifferenceCommand command = new HandlePaymentDifferenceCommand();
        command.setId(420005L);
        command.setProcessAction("ACTIVE_QUERY");
        command.setProcessReason("通道账单存在但本地无单");
        command.setProcessResult("尝试查单");

        assertThatThrownBy(() -> service.handleDifference(command))
                .isInstanceOf(BizException.class)
                .hasMessage("主动查单需要关联本地支付单号或退款单号");
        verifyNoInteractions(channelSyncService);
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(differenceMapper, never()).handleDifference(
                any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any(), anyString(), any());
    }

    @Test
    @DisplayName("difference actions should not expose refund compensate without required refund command context")
    void listDifferenceActions_doesNotExposeRefundCompensate() {
        assertThat(service.listDifferenceActions())
                .extracting(action -> action.getActionCode())
                .containsExactly("ACTIVE_QUERY", "SUPPLEMENT_ORDER", "IGNORE", "CLOSE");
    }

    private PaymentOrderEntity paymentOrder(String payOrderNo) {
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setId(370001L);
        entity.setPayOrderNo(payOrderNo);
        entity.setTenantId(1L);
        entity.setStatus("PAYING");
        entity.setSuccessFlag(0);
        return entity;
    }
}
