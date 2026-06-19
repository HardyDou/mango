package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.vo.PaymentOpenNotificationVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentNotificationRecordEntity;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControl;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class PaymentNotificationServiceTest {

    private final PaymentNotificationRecordMapper notificationRecordMapper = mock(PaymentNotificationRecordMapper.class);
    private final PaymentApplicationMapper applicationMapper = mock(PaymentApplicationMapper.class);
    private final PaymentMangoPayScenarioControlService scenarioControlService = mock(PaymentMangoPayScenarioControlService.class);
    private final PaymentSensitiveValueService sensitiveValueService = mock(PaymentSensitiveValueService.class);
    private final PaymentObservabilityService observabilityService = mock(PaymentObservabilityService.class);
    private final PaymentNumberService numberService = mock(PaymentNumberService.class);
    private final List<Object> publishedEvents = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentNotificationService service = new PaymentNotificationService(
            notificationRecordMapper,
            applicationMapper,
            objectMapper,
            new TestTransactionManager(),
            new SyncTaskExecutor(),
            scenarioControlService,
            sensitiveValueService,
            observabilityService,
            numberService,
            new RecordingEventPublisher(publishedEvents));

    @BeforeEach
    void setUp() {
        when(sensitiveValueService.decrypt("enc:openapi-secret-ciphertext")).thenReturn("openapi-secret");
        when(numberService.next(PaymentNumberService.PAY_NOTIFY_NO))
                .thenReturn("NT2026060600000001", "NT2026060600000002", "NT2026060600000003");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
        publishedEvents.clear();
    }

    private record RecordingEventPublisher(List<Object> events) implements ApplicationEventPublisher {

        @Override
        public void publishEvent(ApplicationEvent event) {
            events.add(event);
        }

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }
    }

    @Test
    @DisplayName("createAndDeliverPayment should persist record, post signed payload and mark success on ACK")
    void createAndDeliverPayment_successAck_marksSuccess() throws Exception {
            CapturedServer server = startServer(200, "SUCCESS");
            List<PaymentNotificationRecordEntity> insertedRecords = captureInsertedRecords();
            List<PaymentNotificationRecordEntity> updatedRecords = captureDeliveryResults();
            try {
                PaymentBusinessOrderEntity businessOrder = businessOrder(server.url());
                service.createAndDeliverPayment(application(), businessOrder, paymentOrder("SUCCESS"));

            PaymentNotificationRecordEntity inserted = insertedRecords.getFirst();
            PaymentNotificationRecordEntity updated = updatedRecords.getFirst();
            assertThat(inserted.getNotificationType()).isEqualTo("PAYMENT_SUCCESS");
            assertThat(inserted.getRelatedOrderNo()).isEqualTo("PO202606060001");
            assertThat(inserted.getNotifyStatus()).isEqualTo("PENDING");
            assertThat(updated.getNotifyStatus()).isEqualTo("SUCCESS");
            assertThat(updated.getResponseCode()).isEqualTo("200");
            JsonNode payload = objectMapper.readTree(server.capturedBody());
            assertThat(payload.path("notifyNo").asText()).isEqualTo(inserted.getNotificationNo());
            assertThat(payload.path("notificationType").asText()).isEqualTo("PAYMENT_SUCCESS");
            assertThat(payload.path("bizOrderNo").asText()).isEqualTo("BIZ_OPENAPI_001");
            assertThat(payload.path("signature").asText()).isNotBlank();
            verify(observabilityService).logSummary(
                    eq("BUSINESS_NOTIFICATION"),
                    eq("PO202606060001"),
                    eq("SUCCESS"),
                    eq(8800L),
                    eq("MANGO_PAY"),
                    any(Long.class),
                    eq("SUCCESS"));
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("deliverDueNotificationRecords should post due payload and mark success on ACK")
    void deliverDueNotificationRecords_duePayload_marksSuccess() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        CapturedServer server = startServer(200, "SUCCESS");
        try {
            PaymentOpenNotificationVO payload = new PaymentOpenNotificationVO();
            payload.setNotifyNo("NT202606060001");
            payload.setNotificationType("PAYMENT_SUCCESS");
            payload.setTenantId(1L);
            payload.setAppId("app_openapi");
            payload.setBizOrderNo("BIZ_OPENAPI_001");
            payload.setPayOrderNo("PO202606060001");
            payload.setStatus("SUCCESS");
            payload.setFlowNo("FLOW202606060001");

            PaymentNotificationRecordEntity due = new PaymentNotificationRecordEntity();
            due.setId(410001L);
            due.setTenantId(1L);
            due.setNotificationNo(payload.getNotifyNo());
            due.setRelatedOrderNo(payload.getPayOrderNo());
            due.setNotificationType(payload.getNotificationType());
            due.setTargetUrl(server.url());
            due.setNotifyStatus("PENDING");
            due.setRetryTimes(0);
            due.setNextRetryTime(LocalDateTime.now().minusMinutes(1));
            due.setPayloadJson(objectMapper.writeValueAsString(payload));
            when(notificationRecordMapper.selectDueNotificationRecords(eq(1L), any(LocalDateTime.class), eq(20L)))
                    .thenReturn(List.of(due));
            when(notificationRecordMapper.claimDueNotificationRecord(eq(1L), eq(410001L), any(LocalDateTime.class), eq(1001L)))
                    .thenReturn(1);
            List<PaymentNotificationRecordEntity> updatedRecords = captureDeliveryResults();

            int delivered = service.deliverDueNotificationRecords(20);

            assertThat(delivered).isEqualTo(1);
            PaymentNotificationRecordEntity updated = updatedRecords.getFirst();
            assertThat(updated.getNotifyStatus()).isEqualTo("SUCCESS");
            assertThat(updated.getResponseCode()).isEqualTo("200");
            assertThat(updated.getNextRetryTime()).isNull();
            JsonNode posted = objectMapper.readTree(server.capturedBody());
            assertThat(posted.path("notifyNo").asText()).isEqualTo("NT202606060001");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("deliverDueNotificationRecords should use next retry policy slot after failed ACK")
    void deliverDueNotificationRecords_failedAck_usesNextRetryPolicySlot() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        CapturedServer server = startServer(200, "WAIT");
        try {
            PaymentOpenNotificationVO payload = duePayload("NT202606060002", "PO202606060002");
            PaymentNotificationRecordEntity due = dueRecord(410002L, payload, server.url(), 0);
            PaymentApplication application = application();
            application.setStatus(1);
            when(notificationRecordMapper.selectDueNotificationRecords(eq(1L), any(LocalDateTime.class), eq(20L)))
                    .thenReturn(List.of(due));
            when(notificationRecordMapper.claimDueNotificationRecord(eq(1L), eq(410002L), any(LocalDateTime.class), eq(1001L)))
                    .thenReturn(1);
            when(applicationMapper.selectOne(any())).thenReturn(application);
            List<PaymentNotificationRecordEntity> updatedRecords = captureDeliveryResults();

            int delivered = service.deliverDueNotificationRecords(20);

            assertThat(delivered).isEqualTo(1);
            PaymentNotificationRecordEntity updated = updatedRecords.getFirst();
            assertThat(updated.getNotifyStatus()).isEqualTo("FAILED");
            assertThat(updated.getResponseMessage()).isEqualTo("WAIT");
            assertThat(updated.getNextRetryTime()).isAfter(LocalDateTime.now().plusMinutes(4));
            assertThat(updated.getNextRetryTime()).isBefore(LocalDateTime.now().plusMinutes(6));
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("deliverDueNotificationRecords should stop automatic retry when policy is exhausted")
    void deliverDueNotificationRecords_failedAck_policyExhaustedStopsAutomaticRetry() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        CapturedServer server = startServer(200, "WAIT");
        try {
            PaymentOpenNotificationVO payload = duePayload("NT202606060003", "PO202606060003");
            PaymentNotificationRecordEntity due = dueRecord(410003L, payload, server.url(), 2);
            PaymentApplication application = application();
            application.setStatus(1);
            when(notificationRecordMapper.selectDueNotificationRecords(eq(1L), any(LocalDateTime.class), eq(20L)))
                    .thenReturn(List.of(due));
            when(notificationRecordMapper.claimDueNotificationRecord(eq(1L), eq(410003L), any(LocalDateTime.class), eq(1001L)))
                    .thenReturn(1);
            when(applicationMapper.selectOne(any())).thenReturn(application);
            List<PaymentNotificationRecordEntity> updatedRecords = captureDeliveryResults();

            int delivered = service.deliverDueNotificationRecords(20);

            assertThat(delivered).isEqualTo(1);
            PaymentNotificationRecordEntity updated = updatedRecords.getFirst();
            assertThat(updated.getNotifyStatus()).isEqualTo("FAILED");
            assertThat(updated.getNextRetryTime()).isNull();
            assertThat(updated.getResponseMessage()).contains("通知重试策略已耗尽，等待人工补偿重推");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("createAndDeliverRefund should mark failed when business ACK is not success")
    void createAndDeliverRefund_nonSuccessAck_marksFailed() throws Exception {
            CapturedServer server = startServer(200, "WAIT");
            List<PaymentNotificationRecordEntity> insertedRecords = captureInsertedRecords();
            List<PaymentNotificationRecordEntity> updatedRecords = captureDeliveryResults();
            try {
                PaymentBusinessOrderEntity businessOrder = businessOrder(server.url());
                service.createAndDeliverRefund(application(), businessOrder, refundOrder("SUCCESS"));

            PaymentNotificationRecordEntity inserted = insertedRecords.getFirst();
            PaymentNotificationRecordEntity updated = updatedRecords.getFirst();
            assertThat(inserted.getNotifyStatus()).isEqualTo("PENDING");
            assertThat(inserted.getNotificationType()).isEqualTo("REFUND_SUCCESS");
            assertThat(inserted.getRelatedOrderNo()).isEqualTo("RO202606060001");
            assertThat(updated.getNotifyStatus()).isEqualTo("FAILED");
            assertThat(updated.getResponseCode()).isEqualTo("200");
            assertThat(updated.getResponseMessage()).isEqualTo("WAIT");
            assertThat(updated.getNextRetryTime()).isNotNull();
            JsonNode payload = objectMapper.readTree(server.capturedBody());
            assertThat(payload.path("bizRefundNo").asText()).isEqualTo("RF_OPENAPI_001");
            assertThat(payload.path("refundOrderNo").asText()).isEqualTo("RO202606060001");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("createAndDeliverPayment should persist scheduled notification when callback delay scenario is active")
    void createAndDeliverPayment_callbackDelay_persistsPendingRecordWithoutHttpPost() throws Exception {
        CapturedServer server = startServer(200, "SUCCESS");
        List<PaymentNotificationRecordEntity> insertedRecords = captureInsertedRecords();
        PaymentMangoPayScenarioControl scenario = new PaymentMangoPayScenarioControl();
        scenario.setCallbackDelayMinutes(10);
        when(scenarioControlService.consumeCallbackDelayScenario(331001L)).thenReturn(scenario);
        try {
            PaymentBusinessOrderEntity businessOrder = businessOrder(server.url());
            service.createAndDeliverPayment(application(), businessOrder, paymentOrder("SUCCESS"));

            PaymentNotificationRecordEntity inserted = insertedRecords.getFirst();
            assertThat(inserted.getNotificationType()).isEqualTo("PAYMENT_SUCCESS");
            assertThat(inserted.getNotifyStatus()).isEqualTo("PENDING");
            assertThat(inserted.getScheduledNotifyTime()).isAfter(LocalDateTime.now().plusMinutes(8));
            assertThat(inserted.getNextRetryTime()).isEqualTo(inserted.getScheduledNotifyTime());
            assertThat(inserted.getPayloadJson()).contains("\"notificationType\":\"PAYMENT_SUCCESS\"");
            assertThat(server.capturedBody()).isBlank();
            verify(notificationRecordMapper, never()).updateDeliveryResult(any(), any(), any(), any(), any(), any(), any(), any());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("notifyPaymentAfterCommit should skip non-terminal payment result")
    void notifyPaymentAfterCommit_nonTerminal_skips() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        service.notifyPaymentAfterCommit(application(), businessOrder("http://127.0.0.1:1/notify"), paymentOrder("PAYING"));

        verify(notificationRecordMapper, times(0)).insert(org.mockito.ArgumentMatchers.any(PaymentNotificationRecordEntity.class));
        verify(notificationRecordMapper, times(0)).updateDeliveryResult(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createAndDeliverPayment should create failed event for failed payment result")
    void createAndDeliverPayment_failedStatus_createsPaymentFailedEvent() throws Exception {
        CapturedServer server = startServer(200, "SUCCESS");
        List<PaymentNotificationRecordEntity> insertedRecords = captureInsertedRecords();
        try {
            service.createAndDeliverPayment(application(), businessOrder(server.url()), paymentOrder("FAILED"));

            PaymentNotificationRecordEntity inserted = insertedRecords.getFirst();
            assertThat(inserted.getNotificationType()).isEqualTo("PAYMENT_FAILED");
            assertThat(inserted.getRelatedOrderNo()).isEqualTo("PO202606060001");
            JsonNode payload = objectMapper.readTree(server.capturedBody());
            assertThat(payload.path("notificationType").asText()).isEqualTo("PAYMENT_FAILED");
            assertThat(payload.path("status").asText()).isEqualTo("FAILED");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("createAndDeliverPayment should create closed event for closed payment result")
    void createAndDeliverPayment_closedStatus_createsPaymentClosedEvent() throws Exception {
        CapturedServer server = startServer(200, "SUCCESS");
        List<PaymentNotificationRecordEntity> insertedRecords = captureInsertedRecords();
        try {
            service.createAndDeliverPayment(application(), businessOrder(server.url()), paymentOrder("CLOSED"));

            PaymentNotificationRecordEntity inserted = insertedRecords.getFirst();
            assertThat(inserted.getNotificationType()).isEqualTo("PAYMENT_CLOSED");
            JsonNode payload = objectMapper.readTree(server.capturedBody());
            assertThat(payload.path("notificationType").asText()).isEqualTo("PAYMENT_CLOSED");
            assertThat(payload.path("status").asText()).isEqualTo("CLOSED");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("createAndDeliverRefund should create failed event for failed refund result")
    void createAndDeliverRefund_failedStatus_createsRefundFailedEvent() throws Exception {
        CapturedServer server = startServer(200, "SUCCESS");
        List<PaymentNotificationRecordEntity> insertedRecords = captureInsertedRecords();
        try {
            service.createAndDeliverRefund(application(), businessOrder(server.url()), refundOrder("FAILED"));

            PaymentNotificationRecordEntity inserted = insertedRecords.getFirst();
            assertThat(inserted.getNotificationType()).isEqualTo("REFUND_FAILED");
            assertThat(inserted.getRelatedOrderNo()).isEqualTo("RO202606060001");
            JsonNode payload = objectMapper.readTree(server.capturedBody());
            assertThat(payload.path("notificationType").asText()).isEqualTo("REFUND_FAILED");
            assertThat(payload.path("status").asText()).isEqualTo("FAILED");
        } finally {
            server.stop();
        }
    }

    private CapturedServer startServer(int statusCode, String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        StringBuilder capturedBody = new StringBuilder();
        server.createContext("/notify", exchange -> {
            capturedBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return new CapturedServer(server, "http://127.0.0.1:" + server.getAddress().getPort() + "/notify", capturedBody);
    }

    private List<PaymentNotificationRecordEntity> captureInsertedRecords() {
        List<PaymentNotificationRecordEntity> records = new ArrayList<>();
        doAnswer(invocation -> {
            PaymentNotificationRecordEntity source = invocation.getArgument(0, PaymentNotificationRecordEntity.class);
            PaymentNotificationRecordEntity snapshot = new PaymentNotificationRecordEntity();
            snapshot.setNotificationNo(source.getNotificationNo());
            snapshot.setRelatedOrderNo(source.getRelatedOrderNo());
            snapshot.setNotificationType(source.getNotificationType());
            snapshot.setTargetUrl(source.getTargetUrl());
            snapshot.setNotifyStatus(source.getNotifyStatus());
            snapshot.setRetryTimes(source.getRetryTimes());
            snapshot.setScheduledNotifyTime(source.getScheduledNotifyTime());
            snapshot.setNextRetryTime(source.getNextRetryTime());
            snapshot.setPayloadJson(source.getPayloadJson());
            snapshot.setResponseCode(source.getResponseCode());
            snapshot.setResponseMessage(source.getResponseMessage());
            snapshot.setTenantId(source.getTenantId());
            records.add(snapshot);
            return 1;
        }).when(notificationRecordMapper).insert(org.mockito.ArgumentMatchers.any(PaymentNotificationRecordEntity.class));
        return records;
    }

    private List<PaymentNotificationRecordEntity> captureDeliveryResults() {
        List<PaymentNotificationRecordEntity> records = new ArrayList<>();
        doAnswer(invocation -> {
            PaymentNotificationRecordEntity snapshot = new PaymentNotificationRecordEntity();
            snapshot.setTenantId(invocation.getArgument(0, Long.class));
            snapshot.setId(invocation.getArgument(1, Long.class));
            snapshot.setNotifyStatus(invocation.getArgument(2, String.class));
            snapshot.setResponseCode(invocation.getArgument(3, String.class));
            snapshot.setResponseMessage(invocation.getArgument(4, String.class));
            snapshot.setNextRetryTime(invocation.getArgument(5, LocalDateTime.class));
            snapshot.setUpdatedAt(invocation.getArgument(6, LocalDateTime.class));
            records.add(snapshot);
            return 1;
        }).when(notificationRecordMapper).updateDeliveryResult(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
        return records;
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setTenantId(1L);
        application.setAppId("app_openapi");
        application.setAppSecret("enc:openapi-secret-ciphertext");
        application.setNotifyRetryPolicy("1m,5m,15m");
        return application;
    }

    private PaymentBusinessOrderEntity businessOrder(String notifyUrl) {
        PaymentBusinessOrderEntity order = new PaymentBusinessOrderEntity();
        order.setId(360001L);
        order.setTenantId(1L);
        order.setAppCode("app_openapi");
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setNotifyUrl(notifyUrl);
        return order;
    }

    private PaymentOrderVO paymentOrder(String status) {
        PaymentOrderVO order = new PaymentOrderVO();
        order.setPayOrderNo("PO202606060001");
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setContractId(331001L);
        order.setAmount(8800L);
        order.setCurrency("CNY");
        order.setStatus(status);
        order.setMethodCode("PERSONAL_WECHAT_QR");
        order.setChannelCode("MANGO_PAY");
        order.setChannelTradeNo("CASHIER-PO202606060001");
        order.setFlowNo("FLOW202606060001");
        order.setPayTime(LocalDateTime.now());
        return order;
    }

    private PaymentRefundOrderVO refundOrder(String status) {
        PaymentRefundOrderVO order = new PaymentRefundOrderVO();
        order.setRefundOrderNo("RO202606060001");
        order.setContractId(331001L);
        order.setBizRefundNo("RF_OPENAPI_001");
        order.setPayOrderNo("PO202606060001");
        order.setBizOrderNo("BIZ_OPENAPI_001");
        order.setRefundAmount(3300L);
        order.setCurrency("CNY");
        order.setStatus(status);
        order.setMethodCode("PERSONAL_WECHAT_QR");
        order.setChannelCode("MANGO_PAY");
        order.setChannelTradeNo("CASHIER-PO202606060001");
        order.setChannelRefundNo("CASHIER-REFUND-RO202606060001");
        order.setFlowNo("RFLOW202606060001");
        order.setRefundTime(LocalDateTime.now());
        return order;
    }

    private PaymentOpenNotificationVO duePayload(String notificationNo, String payOrderNo) {
        PaymentOpenNotificationVO payload = new PaymentOpenNotificationVO();
        payload.setNotifyNo(notificationNo);
        payload.setNotificationType("PAYMENT_SUCCESS");
        payload.setTenantId(1L);
        payload.setAppId("app_openapi");
        payload.setBizOrderNo("BIZ_OPENAPI_001");
        payload.setPayOrderNo(payOrderNo);
        payload.setStatus("SUCCESS");
        payload.setFlowNo("FLOW202606060001");
        return payload;
    }

    private PaymentNotificationRecordEntity dueRecord(Long id, PaymentOpenNotificationVO payload, String targetUrl, int retryTimes) throws IOException {
        PaymentNotificationRecordEntity due = new PaymentNotificationRecordEntity();
        due.setId(id);
        due.setTenantId(1L);
        due.setNotificationNo(payload.getNotifyNo());
        due.setRelatedOrderNo(payload.getPayOrderNo());
        due.setNotificationType(payload.getNotificationType());
        due.setTargetUrl(targetUrl);
        due.setNotifyStatus("FAILED");
        due.setRetryTimes(retryTimes);
        due.setNextRetryTime(LocalDateTime.now().minusMinutes(1));
        due.setPayloadJson(objectMapper.writeValueAsString(payload));
        return due;
    }

    private record CapturedServer(HttpServer server, String url, StringBuilder body) {

        void stop() {
            server.stop(0);
        }

        String capturedBody() {
            return body.toString();
        }
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
