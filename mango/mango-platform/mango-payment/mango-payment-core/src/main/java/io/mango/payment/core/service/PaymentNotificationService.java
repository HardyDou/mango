package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.event.NoticeSendEvent;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentOpenNotificationVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentNotificationRecordEntity;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControl;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class PaymentNotificationService {

    private static final String SIGN_ALGORITHM = "HmacSHA256";
    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String FAILED_STATUS = "FAILED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String CLOSED_STATUS = "CLOSED";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final long DEFAULT_RETRY_MINUTES = 5L;
    private static final List<Long> DEFAULT_RETRY_POLICY = List.of(DEFAULT_RETRY_MINUTES);

    private final PaymentNotificationRecordMapper notificationRecordMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final TaskExecutor mangoContextExecutor;
    private final PaymentMangoPayScenarioControlService scenarioControlService;
    private final PaymentSensitiveValueService sensitiveValueService;
    private final PaymentObservabilityService observabilityService;
    private final PaymentNumberService numberService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentNotificationService(
            PaymentNotificationRecordMapper notificationRecordMapper,
            PaymentApplicationMapper applicationMapper,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Qualifier("mangoContextExecutor") TaskExecutor mangoContextExecutor,
            PaymentMangoPayScenarioControlService scenarioControlService,
            PaymentSensitiveValueService sensitiveValueService,
            PaymentObservabilityService observabilityService,
            PaymentNumberService numberService,
            ApplicationEventPublisher eventPublisher) {
        this.notificationRecordMapper = notificationRecordMapper;
        this.applicationMapper = applicationMapper;
        this.objectMapper = objectMapper;
        this.transactionManager = transactionManager;
        this.mangoContextExecutor = mangoContextExecutor;
        this.scenarioControlService = scenarioControlService;
        this.sensitiveValueService = sensitiveValueService;
        this.observabilityService = observabilityService;
        this.numberService = numberService;
        this.eventPublisher = eventPublisher;
    }

    public void notifyPaymentAfterCommit(PaymentApplication application, PaymentBusinessOrderEntity businessOrder, PaymentOrderVO paymentOrder) {
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_INVALID);
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        if (!isNotifiablePaymentStatus(paymentOrder.getStatus())) {
            return;
        }
        MangoContextSnapshot context = MangoContextHolder.get();
        Runnable task = () -> withContext(context, () -> {
            publishPaymentNoticeEvent(application, businessOrder, paymentOrder);
            createAndDeliverPayment(application, businessOrder, paymentOrder);
        });
        runAfterCommit(task);
    }

    public void notifyRefundAfterCommit(PaymentApplication application, PaymentBusinessOrderEntity businessOrder, PaymentRefundOrderVO refundOrder) {
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_INVALID);
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        if (!isNotifiableRefundStatus(refundOrder.getStatus())) {
            return;
        }
        MangoContextSnapshot context = MangoContextHolder.get();
        Runnable task = () -> withContext(context, () -> {
            publishRefundNoticeEvent(application, businessOrder, refundOrder);
            createAndDeliverRefund(application, businessOrder, refundOrder);
        });
        runAfterCommit(task);
    }

    public void createAndDeliverPayment(PaymentApplication application, PaymentBusinessOrderEntity businessOrder, PaymentOrderVO paymentOrder) {
        Require.notBlank(businessOrder.getNotifyUrl(), PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "业务通知地址不能为空");
        LocalDateTime now = LocalDateTime.now();
        String notificationNo = numberService.next(PaymentNumberService.PAY_NOTIFY_NO);
        PaymentNotificationRecordEntity record = createPendingRecord(
                notificationNo,
                paymentOrder.getPayOrderNo(),
                paymentNotificationType(paymentOrder.getStatus()),
                businessOrder.getNotifyUrl(),
                application.getTenantId(),
                now);
        PaymentOpenNotificationVO payload = paymentPayload(notificationNo, application, businessOrder, paymentOrder, now);
        persistOrDeliver(record, payload, paymentOrder.getContractId(), paymentOrder.getChannelCode(), firstRetryDelayMinutes(application));
    }

    public void createAndDeliverRefund(PaymentApplication application, PaymentBusinessOrderEntity businessOrder, PaymentRefundOrderVO refundOrder) {
        Require.notBlank(businessOrder.getNotifyUrl(), PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "业务通知地址不能为空");
        LocalDateTime now = LocalDateTime.now();
        String notificationNo = numberService.next(PaymentNumberService.PAY_NOTIFY_NO);
        PaymentNotificationRecordEntity record = createPendingRecord(
                notificationNo,
                refundOrder.getRefundOrderNo(),
                refundNotificationType(refundOrder.getStatus()),
                businessOrder.getNotifyUrl(),
                application.getTenantId(),
                now);
        PaymentOpenNotificationVO payload = refundPayload(notificationNo, application, businessOrder, refundOrder, now);
        persistOrDeliver(record, payload, refundOrder.getContractId(), refundOrder.getChannelCode(), firstRetryDelayMinutes(application));
    }

    public int deliverDueNotificationRecords(long limit) {
        Require.isTrue(limit > 0 && limit <= 100,
                PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知投递批次大小必须在 1 到 100 之间");
        Long tenantId = PaymentContextSupport.currentTenantId();
        LocalDateTime now = LocalDateTime.now();
        List<PaymentNotificationRecordEntity> records = notificationRecordMapper.selectDueNotificationRecords(tenantId, now, limit);
        int delivered = 0;
        for (PaymentNotificationRecordEntity record : records) {
            PaymentOpenNotificationVO payload = readPayload(record.getPayloadJson());
            int claimed = executeInNewTransactionWithResult(() -> notificationRecordMapper.claimDueNotificationRecord(
                    tenantId,
                    record.getId(),
                    now,
                    PaymentContextSupport.currentUserId()));
            if (claimed == 1) {
                record.setNotifyStatus("RETRYING");
                record.setRetryTimes((record.getRetryTimes() == null ? 0 : record.getRetryTimes()) + 1);
                deliver(record, payload, nextRetryDelayMinutes(payload.getAppId(), record.getRetryTimes()));
                delivered++;
            }
        }
        return delivered;
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mangoContextExecutor.execute(task);
                }
            });
            return;
        }
        mangoContextExecutor.execute(task);
    }

    private void withContext(MangoContextSnapshot context, Runnable task) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(context);
            task.run();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private void publishPaymentNoticeEvent(
            PaymentApplication application,
            PaymentBusinessOrderEntity businessOrder,
            PaymentOrderVO paymentOrder) {
        eventPublisher.publishEvent(NoticeSendEvent.builder()
                .bizType(paymentNoticeBizType(paymentOrder.getStatus()))
                .bizId(paymentOrder.getPayOrderNo())
                .recipientRuleCode("payment.operator")
                .priority(NoticePriority.NORMAL)
                .idempotentKey("payment:" + paymentOrder.getPayOrderNo() + ":" + paymentOrder.getStatus())
                .params(paymentNoticeParams(application, businessOrder, paymentOrder))
                .build());
    }

    private void publishRefundNoticeEvent(
            PaymentApplication application,
            PaymentBusinessOrderEntity businessOrder,
            PaymentRefundOrderVO refundOrder) {
        eventPublisher.publishEvent(NoticeSendEvent.builder()
                .bizType(refundNoticeBizType(refundOrder.getStatus()))
                .bizId(refundOrder.getRefundOrderNo())
                .recipientRuleCode("payment.operator")
                .priority(FAILED_STATUS.equals(refundOrder.getStatus()) ? NoticePriority.HIGH : NoticePriority.NORMAL)
                .idempotentKey("payment:refund:" + refundOrder.getRefundOrderNo() + ":" + refundOrder.getStatus())
                .params(refundNoticeParams(application, businessOrder, refundOrder))
                .build());
    }

    private String paymentNoticeBizType(String status) {
        if (SUCCESS_STATUS.equals(status)) {
            return "payment.order.success";
        }
        return "payment.order.failed";
    }

    private String refundNoticeBizType(String status) {
        if (SUCCESS_STATUS.equals(status)) {
            return "payment.refund.success";
        }
        return "payment.refund.failed";
    }

    private Map<String, Object> paymentNoticeParams(
            PaymentApplication application,
            PaymentBusinessOrderEntity businessOrder,
            PaymentOrderVO paymentOrder) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", application.getAppId());
        params.put("appName", application.getAppName());
        params.put("payOrderNo", paymentOrder.getPayOrderNo());
        params.put("bizOrderNo", businessOrder.getBizOrderNo());
        params.put("amount", paymentOrder.getAmount());
        params.put("currency", paymentOrder.getCurrency());
        params.put("channelCode", paymentOrder.getChannelCode());
        params.put("status", paymentOrder.getStatus());
        params.put("failReason", paymentOrder.getStatusName());
        return params;
    }

    private Map<String, Object> refundNoticeParams(
            PaymentApplication application,
            PaymentBusinessOrderEntity businessOrder,
            PaymentRefundOrderVO refundOrder) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", application.getAppId());
        params.put("appName", application.getAppName());
        params.put("payOrderNo", refundOrder.getPayOrderNo());
        params.put("bizOrderNo", businessOrder.getBizOrderNo());
        params.put("refundOrderNo", refundOrder.getRefundOrderNo());
        params.put("refundAmount", refundOrder.getRefundAmount());
        params.put("currency", refundOrder.getCurrency());
        params.put("channelCode", refundOrder.getChannelCode());
        params.put("status", refundOrder.getStatus());
        params.put("failReason", refundOrder.getStatusName());
        params.put("reason", refundOrder.getReason());
        return params;
    }

    private PaymentNotificationRecordEntity createPendingRecord(
            String notificationNo,
            String relatedOrderNo,
            String notificationType,
            String targetUrl,
            Long tenantId,
            LocalDateTime now) {
        PaymentNotificationRecordEntity entity = new PaymentNotificationRecordEntity();
        entity.setNotificationNo(notificationNo);
        entity.setRelatedOrderNo(relatedOrderNo);
        entity.setNotificationType(notificationType);
        entity.setTargetUrl(targetUrl);
        entity.setNotifyStatus(PENDING_STATUS);
        entity.setRetryTimes(0);
        entity.setScheduledNotifyTime(now);
        entity.setNextRetryTime(null);
        entity.setTenantId(tenantId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private PaymentOpenNotificationVO paymentPayload(
            String notificationNo,
            PaymentApplication application,
            PaymentBusinessOrderEntity businessOrder,
            PaymentOrderVO paymentOrder,
            LocalDateTime notifyTime) {
        PaymentOpenNotificationVO payload = new PaymentOpenNotificationVO();
        payload.setNotifyNo(notificationNo);
        payload.setNotificationType(paymentNotificationType(paymentOrder.getStatus()));
        payload.setTenantId(application.getTenantId());
        payload.setAppId(application.getAppId());
        payload.setBizOrderNo(businessOrder.getBizOrderNo());
        payload.setPayOrderNo(paymentOrder.getPayOrderNo());
        payload.setAmount(paymentOrder.getAmount());
        payload.setCurrency(paymentOrder.getCurrency());
        payload.setStatus(paymentOrder.getStatus());
        payload.setMethodCode(paymentOrder.getMethodCode());
        payload.setChannelCode(paymentOrder.getChannelCode());
        payload.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        payload.setFlowNo(paymentOrder.getFlowNo());
        payload.setEventTime(formatTime(paymentOrder.getPayTime() == null ? notifyTime : paymentOrder.getPayTime()));
        payload.setNotifyTime(formatTime(notifyTime));
        payload.setSignAlgorithm("HMAC_SHA256");
        payload.setSignature(sign(sensitiveValueService.decrypt(application.getAppSecret()), canonical(payload)));
        return payload;
    }

    private PaymentOpenNotificationVO refundPayload(
            String notificationNo,
            PaymentApplication application,
            PaymentBusinessOrderEntity businessOrder,
            PaymentRefundOrderVO refundOrder,
            LocalDateTime notifyTime) {
        PaymentOpenNotificationVO payload = new PaymentOpenNotificationVO();
        payload.setNotifyNo(notificationNo);
        payload.setNotificationType(refundNotificationType(refundOrder.getStatus()));
        payload.setTenantId(application.getTenantId());
        payload.setAppId(application.getAppId());
        payload.setBizOrderNo(businessOrder.getBizOrderNo());
        payload.setPayOrderNo(refundOrder.getPayOrderNo());
        payload.setBizRefundNo(refundOrder.getBizRefundNo());
        payload.setRefundOrderNo(refundOrder.getRefundOrderNo());
        payload.setRefundAmount(refundOrder.getRefundAmount());
        payload.setCurrency(refundOrder.getCurrency());
        payload.setStatus(refundOrder.getStatus());
        payload.setMethodCode(refundOrder.getMethodCode());
        payload.setChannelCode(refundOrder.getChannelCode());
        payload.setChannelTradeNo(refundOrder.getChannelTradeNo());
        payload.setChannelRefundNo(refundOrder.getChannelRefundNo());
        payload.setFlowNo(refundOrder.getFlowNo());
        payload.setEventTime(formatTime(refundOrder.getRefundTime() == null ? notifyTime : refundOrder.getRefundTime()));
        payload.setNotifyTime(formatTime(notifyTime));
        payload.setSignAlgorithm("HMAC_SHA256");
        payload.setSignature(sign(sensitiveValueService.decrypt(application.getAppSecret()), canonical(payload)));
        return payload;
    }

    private void deliver(PaymentNotificationRecordEntity record, PaymentOpenNotificationVO payload, Optional<Long> retryMinutes) {
        long startedAt = System.nanoTime();
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpResponse<String> response = post(record.getTargetUrl(), body);
            boolean acknowledged = response.statusCode() >= 200 && response.statusCode() < 300 && isSuccessAck(response.body());
            if (acknowledged) {
                markSuccess(record, response.statusCode(), response.body());
                logSummary(record, payload, startedAt, SUCCESS_STATUS);
            } else {
                markFailed(record, String.valueOf(response.statusCode()), response.body(), retryMinutes);
                logSummary(record, payload, startedAt, FAILED_STATUS);
            }
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知报文序列化失败", ex);
        } catch (IOException ex) {
            markFailed(record, "IO_ERROR", ex.getMessage(), retryMinutes);
            logSummary(record, payload, startedAt, FAILED_STATUS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            markFailed(record, "INTERRUPTED", ex.getMessage(), retryMinutes);
            logSummary(record, payload, startedAt, FAILED_STATUS);
        } catch (IllegalArgumentException ex) {
            markFailed(record, "INVALID_URL", ex.getMessage(), retryMinutes);
            logSummary(record, payload, startedAt, FAILED_STATUS);
        }
    }

    private void logSummary(
            PaymentNotificationRecordEntity record,
            PaymentOpenNotificationVO payload,
            long startedAt,
            String result) {
        String orderNo = payload.getRefundOrderNo() == null ? payload.getPayOrderNo() : payload.getRefundOrderNo();
        Long amount = payload.getRefundAmount() == null ? payload.getAmount() : payload.getRefundAmount();
        observabilityService.logSummary("BUSINESS_NOTIFICATION", orderNo, record.getNotifyStatus(),
                amount, payload.getChannelCode(), elapsedMillis(startedAt), result);
    }

    private void persistOrDeliver(
            PaymentNotificationRecordEntity record,
            PaymentOpenNotificationVO payload,
            Long contractId,
            String channelCode,
            Optional<Long> retryMinutes) {
        String body = writePayload(payload);
        PaymentMangoPayScenarioControl delayScenario = consumeDelayScenario(contractId, channelCode);
        if (delayScenario != null) {
            LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(delayScenario.getCallbackDelayMinutes());
            record.setScheduledNotifyTime(scheduledTime);
            record.setNextRetryTime(scheduledTime);
            record.setPayloadJson(body);
            executeInNewTransaction(() -> notificationRecordMapper.insert(record));
            return;
        }
        record.setPayloadJson(body);
        executeInNewTransaction(() -> notificationRecordMapper.insert(record));
        deliver(record, payload, retryMinutes);
    }

    private PaymentMangoPayScenarioControl consumeDelayScenario(Long contractId, String channelCode) {
        if (!"MANGO_PAY".equals(channelCode)) {
            return null;
        }
        return scenarioControlService.consumeCallbackDelayScenario(contractId);
    }

    private boolean isNotifiablePaymentStatus(String status) {
        return PaymentOrderStatusEnum.SUCCESS.getCode().equals(status)
                || PaymentOrderStatusEnum.FAILED.getCode().equals(status)
                || PaymentOrderStatusEnum.CLOSED.getCode().equals(status);
    }

    private boolean isNotifiableRefundStatus(String status) {
        return PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(status)
                || PaymentRefundOrderStatusEnum.FAILED.getCode().equals(status);
    }

    private String paymentNotificationType(String status) {
        if (SUCCESS_STATUS.equals(status)) {
            return "PAYMENT_SUCCESS";
        }
        if (FAILED_STATUS.equals(status)) {
            return "PAYMENT_FAILED";
        }
        if (CLOSED_STATUS.equals(status)) {
            return "PAYMENT_CLOSED";
        }
        throw new BizException(PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "支付通知状态不支持");
    }

    private String refundNotificationType(String status) {
        if (SUCCESS_STATUS.equals(status)) {
            return "REFUND_SUCCESS";
        }
        if (FAILED_STATUS.equals(status)) {
            return "REFUND_FAILED";
        }
        throw new BizException(PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "退款通知状态不支持");
    }

    private String writePayload(PaymentOpenNotificationVO payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知报文序列化失败", ex);
        }
    }

    private PaymentOpenNotificationVO readPayload(String payloadJson) {
        Require.notBlank(payloadJson, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知报文快照不能为空");
        try {
            return objectMapper.readValue(payloadJson, PaymentOpenNotificationVO.class);
        } catch (IOException ex) {
            throw new BizException(PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知报文快照解析失败", ex);
        }
    }

    private HttpResponse<String> post(String targetUrl, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(targetUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private boolean isSuccessAck(String body) {
        String normalized = body == null ? "" : body.trim();
        if ("SUCCESS".equalsIgnoreCase(normalized) || "OK".equalsIgnoreCase(normalized)) {
            return true;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        return upper.contains("\"SUCCESS\":TRUE")
                || upper.contains("\"ACK\":\"SUCCESS\"")
                || upper.contains("\"CODE\":\"SUCCESS\"");
    }

    private void markSuccess(PaymentNotificationRecordEntity record, int responseCode, String responseMessage) {
        LocalDateTime now = LocalDateTime.now();
        record.setNotifyStatus(SUCCESS_STATUS);
        record.setResponseCode(String.valueOf(responseCode));
        record.setResponseMessage(limit(responseMessage));
        record.setNextRetryTime(null);
        record.setUpdatedAt(now);
        executeInNewTransaction(() -> updateDeliveryResult(record, now));
    }

    private void markFailed(PaymentNotificationRecordEntity record, String responseCode, String responseMessage, Optional<Long> retryMinutes) {
        LocalDateTime now = LocalDateTime.now();
        record.setNotifyStatus(FAILED_STATUS);
        record.setResponseCode(limit(responseCode));
        if (retryMinutes.isPresent()) {
            record.setResponseMessage(limit(responseMessage));
            record.setNextRetryTime(now.plusMinutes(retryMinutes.get()));
        } else {
            record.setResponseMessage(limit(appendExhaustedMessage(responseMessage)));
            record.setNextRetryTime(null);
        }
        record.setUpdatedAt(now);
        executeInNewTransaction(() -> updateDeliveryResult(record, now));
    }

    private void updateDeliveryResult(PaymentNotificationRecordEntity record, LocalDateTime now) {
        notificationRecordMapper.updateDeliveryResult(
                record.getTenantId(),
                record.getId(),
                record.getNotifyStatus(),
                record.getResponseCode(),
                record.getResponseMessage(),
                record.getNextRetryTime(),
                now,
                PaymentContextSupport.currentUserId());
    }

    private void executeInNewTransaction(Runnable action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> action.run());
    }

    private <T> T executeInNewTransactionWithResult(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());
    }

    private String canonical(PaymentOpenNotificationVO payload) {
        return payload.getNotifyNo()
                + "\n" + payload.getNotificationType()
                + "\n" + payload.getTenantId()
                + "\n" + payload.getAppId()
                + "\n" + nullToEmpty(payload.getBizOrderNo())
                + "\n" + nullToEmpty(payload.getPayOrderNo())
                + "\n" + nullToEmpty(payload.getBizRefundNo())
                + "\n" + nullToEmpty(payload.getRefundOrderNo())
                + "\n" + nullToEmpty(payload.getStatus())
                + "\n" + nullToEmpty(payload.getFlowNo());
    }

    private String sign(String appSecret, String canonical) {
        Require.notBlank(appSecret, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID.getCode(), "支付应用密钥未配置");
        try {
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new BizException(PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID.getCode(), "通知签名计算失败", ex);
        }
    }

    private Optional<Long> firstRetryDelayMinutes(PaymentApplication application) {
        List<Long> policy = retryPolicyMinutes(application == null ? null : application.getNotifyRetryPolicy());
        return policy.isEmpty() ? Optional.empty() : Optional.of(policy.getFirst());
    }

    private Optional<Long> nextRetryDelayMinutes(String appId, Integer retryTimesAfterClaim) {
        PaymentApplication application = selectApplicationByAppId(appId);
        List<Long> policy = retryPolicyMinutes(application == null ? null : application.getNotifyRetryPolicy());
        int retryIndex = Math.max(0, retryTimesAfterClaim == null ? 0 : retryTimesAfterClaim);
        if (retryIndex >= policy.size()) {
            return Optional.empty();
        }
        return Optional.of(policy.get(retryIndex));
    }

    private PaymentApplication selectApplicationByAppId(String appId) {
        String normalizedAppId = trimToNull(appId);
        if (normalizedAppId == null || applicationMapper == null) {
            return null;
        }
        Long tenantId = PaymentContextSupport.currentTenantId();
        return applicationMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, normalizedAppId)
                .eq(PaymentApplication::getStatus, 1));
    }

    private List<Long> retryPolicyMinutes(String value) {
        String policy = trimToNull(value);
        if (policy == null) {
            return DEFAULT_RETRY_POLICY;
        }
        List<Long> result = java.util.Arrays.stream(policy.split(","))
                .map(this::parseRetryDelayMinutes)
                .flatMap(Optional::stream)
                .toList();
        return result.isEmpty() ? DEFAULT_RETRY_POLICY : result;
    }

    private Optional<Long> parseRetryDelayMinutes(String value) {
        String item = trimToNull(value);
        if (item == null) {
            return Optional.empty();
        }
        String normalized = item.toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("m")) {
                return Optional.of(Math.max(1L, Long.parseLong(normalized.substring(0, normalized.length() - 1))));
            }
            if (normalized.endsWith("h")) {
                return Optional.of(Math.max(1L, Long.parseLong(normalized.substring(0, normalized.length() - 1)) * 60L));
            }
            return Optional.of(Math.max(1L, Long.parseLong(normalized)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String appendExhaustedMessage(String responseMessage) {
        String normalized = trimToNull(responseMessage);
        if (normalized == null) {
            return "通知重试策略已耗尽，等待人工补偿重推";
        }
        return normalized + "；通知重试策略已耗尽，等待人工补偿重推";
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String limit(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
