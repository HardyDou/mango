package io.mango.payment.core.service.impl;

import static io.mango.payment.core.model.PaymentProjectionConverter.toApi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.command.CreatePaymentOpenOrderCommand;
import io.mango.payment.api.command.CreatePaymentOpenPayCommand;
import io.mango.payment.api.command.CreatePaymentOpenRefundCommand;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplicationEntity;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentCashierConfigEntity;
import io.mango.payment.core.entity.PaymentOpenApiNonceEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentOpenApiNonceMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.service.IPaymentCashierService;
import io.mango.payment.core.service.IPaymentOpenApiService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOrderStatusFlowRecorder;
import io.mango.payment.core.service.PaymentOrderStatePolicy;
import io.mango.payment.core.service.PaymentRefundApplyCoordinator;
import io.mango.payment.core.service.PaymentSensitiveValueCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentOpenApiService implements IPaymentOpenApiService {

    private static final long SIGNATURE_WINDOW_SECONDS = 300L;
    private static final String EMPTY_BODY = "";
    private static final String DEFAULT_CURRENCY = "CNY";
    private static final String SIGN_ALGORITHM = "HmacSHA256";

    private final PaymentApplicationMapper applicationMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentCashierConfigMapper cashierConfigMapper;
    private final PaymentOpenApiNonceMapper nonceMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final IPaymentCashierService cashierService;
    private final PaymentOrderStatePolicy orderStateService;
    private final PaymentOrderStatusFlowRecorder statusFlowService;
    private final PaymentRefundApplyCoordinator refundApplyService;
    private final PaymentSensitiveValueCodec sensitiveValueService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOpenBusinessOrderVO createOrder(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), request.getBody());
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            CreatePaymentOpenOrderCommand command = parseCreateCommand(request.getBody());
            validateCreateCommand(command, application);
            PaymentBusinessOrderEntity existing = selectBusinessOrder(application, command.getBizOrderNo());
            if (existing != null) {
                Require.isTrue(isSameOrder(existing, command), PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT);
                return toBusinessOrderVO(existing);
            }
            PaymentCashierConfigEntity cashierConfig = selectDefaultCashier(application);
            Long subjectId = resolveSubjectId(command, cashierConfig);
            PaymentBusinessOrderEntity entity = new PaymentBusinessOrderEntity();
            entity.setBizOrderNo(command.getBizOrderNo().trim());
            entity.setAppCode(application.getAppId());
            entity.setTitle(command.getTitle().trim());
            entity.setSubjectId(subjectId);
            entity.setAmount(command.getAmount());
            entity.setPaidAmount(0L);
            entity.setRefundedAmount(0L);
            entity.setCurrency(command.getCurrency().trim().toUpperCase(Locale.ROOT));
            entity.setStatus(PaymentBusinessOrderStatusEnum.TO_PAY.getCode());
            entity.setExpireTime(LocalDateTime.now().plusMinutes(command.getExpireMinutes()));
            entity.setNotifyUrl(command.getNotifyUrl().trim());
            entity.setReturnUrl(command.getReturnUrl().trim());
            entity.setExtendInfo(writeExtendInfo(command.getExtendInfo()));
            entity.setTenantId(application.getTenantId());
            businessOrderMapper.insert(entity);
            statusFlowService.record(
                    application.getTenantId(),
                    PaymentOrderStatusFlowRecorder.ORDER_TYPE_BUSINESS,
                    entity.getId(),
                    entity.getBizOrderNo(),
                    null,
                    entity.getStatus(),
                    PaymentOrderStatusFlowRecorder.SOURCE_OPENAPI_CREATE,
                    entity.getBizOrderNo(),
                    LocalDateTime.now(),
                    "开放接口创建业务订单");
            return toBusinessOrderVO(entity);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public PaymentOpenBusinessOrderVO detailOrder(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), EMPTY_BODY);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            PaymentBusinessOrderEntity entity = selectRequiredBusinessOrder(application, request.getBizOrderNo());
            return toBusinessOrderVO(entity);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public PaymentOpenCashierVO cashier(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), normalizeBody(request.getBody()));
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            PaymentBusinessOrderEntity order = selectRequiredBusinessOrder(application, request.getBizOrderNo());
            orderStateService.requireBusinessOrderPayable(order.getStatus(), order.getExpireTime());
            PaymentCashierConfigEntity cashierConfig = selectCashier(application, order);
            PaymentOpenCashierVO vo = new PaymentOpenCashierVO();
            vo.setCashierConfigId(cashierConfig.getId());
            vo.setBusinessOrderId(order.getId());
            vo.setBizOrderNo(order.getBizOrderNo());
            vo.setCashierUrl("/payment/cashier-configs/" + cashierConfig.getId() + "/cashier?businessOrderId=" + order.getId());
            vo.setExpireTime(order.getExpireTime());
            return vo;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public PaymentOpenPaymentOrderVO pay(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), request.getBody());
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            CreatePaymentOpenPayCommand command = parsePayCommand(request.getBody());
            PaymentBusinessOrderEntity order = selectRequiredBusinessOrder(application, request.getBizOrderNo());
            orderStateService.requireBusinessOrderPayable(order.getStatus(), order.getExpireTime());
            PaymentCashierConfigEntity cashierConfig = selectCashier(application, order);
            PaymentCashierPayCommand cashierCommand = new PaymentCashierPayCommand();
            cashierCommand.setCashierConfigId(cashierConfig.getId());
            cashierCommand.setBusinessOrderId(order.getId());
            cashierCommand.setMethodCode(command.getMethodCode().trim());
            cashierCommand.setClientIp(PaymentContextSupport.trimToNull(request.getClientIp()));
            PaymentCashierPayResultVO payResult = cashierService.pay(cashierCommand);
            Require.notNull(payResult, PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "支付结果不能为空");
            PaymentOrderVO paymentOrder = selectRequiredOpenPaymentOrder(application, payResult.getPayOrderNo());
            return toOpenPaymentOrderVO(paymentOrder, payResult);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public PaymentOpenPaymentOrderVO detailPaymentOrder(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), EMPTY_BODY);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            PaymentOrderVO paymentOrder = selectRequiredOpenPaymentOrder(application, request.getPayOrderNo());
            return toOpenPaymentOrderVO(paymentOrder, null);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public PaymentOpenRefundOrderVO refund(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), request.getBody());
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            CreatePaymentOpenRefundCommand command = parseRefundCommand(request.getBody());
            return refundApplyService.applyRefund(
                    application,
                    command,
                    PaymentOrderStatusFlowRecorder.SOURCE_OPENAPI_REFUND,
                    command.getBizRefundNo().trim(),
                    true);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public PaymentOpenRefundOrderVO detailRefund(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), EMPTY_BODY);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            PaymentRefundOrderVO refundOrder = selectRequiredOpenRefundOrder(application, request.getBizRefundNo());
            refundOrder.setFlowNo(refundOrderMapper.selectLatestFlowNo(application.getTenantId(), refundOrder.getId()));
            return toOpenRefundOrderVO(refundOrder);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public PaymentOpenReceiptVO receipt(PaymentOpenRequestCommand request) {
        Require.notNull(request, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "开放接口请求不能为空");
        PaymentApplicationEntity application = authenticate(request.getAppId(), request.getTenantId(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), "POST", request.getRequestPath(), EMPTY_BODY);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindOpenApiContext(application);
            PaymentOrderVO paymentOrder = selectRequiredSuccessfulPaymentOrder(application, request.getBizOrderNo());
            paymentOrder.setFlowNo(paymentOrderMapper.selectLatestFlowNo(application.getTenantId(), paymentOrder.getId()));
            return toOpenReceiptVO(paymentOrder);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private PaymentApplicationEntity authenticate(
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String method,
            String requestPath,
            String body) {
        Require.notBlank(appId, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "AppId 不能为空");
        Require.notBlank(tenantId, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "tenantId 不能为空");
        Require.notBlank(timestamp, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "timestamp 不能为空");
        Require.notBlank(nonce, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "nonce 不能为空");
        Require.notBlank(signature, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "signature 不能为空");
        Require.notBlank(requestPath, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "请求路径不能为空");
        String resolvedTenantId = tenantId.trim();
        long epochSeconds = parseTimestamp(timestamp);
        long nowSeconds = Instant.now().getEpochSecond();
        Require.isTrue(Math.abs(nowSeconds - epochSeconds) <= SIGNATURE_WINDOW_SECONDS,
                PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "timestamp 已超过有效窗口");
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(String.valueOf(resolvedTenantId)));
            PaymentApplicationEntity application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplicationEntity>()
                    .eq(PaymentApplicationEntity::getTenantId, resolvedTenantId)
                    .eq(PaymentApplicationEntity::getAppId, appId.trim()));
            Require.notNull(application, PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "支付应用认证失败");
            Require.isTrue(Integer.valueOf(1).equals(application.getStatus()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "支付应用未启用");
            Require.isTrue(Integer.valueOf(1).equals(application.getSecretConfigured()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "支付应用密钥未配置");
            Require.notBlank(application.getAppSecret(), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "支付应用密钥未配置");
            Require.isTrue("HMAC_SHA256".equals(application.getSignAlgorithm()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "支付应用签名算法不支持");
            String expectedSignature = sign(sensitiveValueService.decrypt(application.getAppSecret()), canonical(method, requestPath, body, timestamp, nonce));
            Require.isTrue(Objects.equals(expectedSignature, signature.trim()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "signature 不正确");
            recordNonce(application, nonce.trim(), epochSeconds);
            return application;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private void recordNonce(PaymentApplicationEntity application, String nonce, long timestampSeconds) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> {
            nonceMapper.delete(new LambdaQueryWrapper<PaymentOpenApiNonceEntity>()
                    .eq(PaymentOpenApiNonceEntity::getTenantId, application.getTenantId())
                    .eq(PaymentOpenApiNonceEntity::getAppId, application.getAppId())
                    .lt(PaymentOpenApiNonceEntity::getExpireTime, LocalDateTime.now()));
            PaymentOpenApiNonceEntity entity = new PaymentOpenApiNonceEntity();
            entity.setTenantId(application.getTenantId());
            entity.setAppId(application.getAppId());
            entity.setNonce(nonce);
            entity.setExpireTime(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestampSeconds + SIGNATURE_WINDOW_SECONDS), ZoneId.systemDefault()));
            try {
                nonceMapper.insert(entity);
            } catch (DuplicateKeyException ex) {
                Require.fail(PaymentCode.PAYMENT_OPENAPI_NONCE_REPLAY, PaymentCode.PAYMENT_OPENAPI_NONCE_REPLAY.getMessage(), ex);
            }
        });
    }

    private String canonical(String method, String requestPath, String body, String timestamp, String nonce) {
        return method.toUpperCase(Locale.ROOT)
                + "\n" + requestPath
                + "\n" + sha256Hex(normalizeBody(body))
                + "\n" + timestamp.trim()
                + "\n" + nonce.trim();
    }

    private String sign(String appSecret, String canonical) {
        try {
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            return Require.fail(PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "支付开放接口签名计算失败", ex);
        }
    }

    private String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Require.fail(PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "支付开放接口摘要计算失败", ex);
        }
    }

    private CreatePaymentOpenOrderCommand parseCreateCommand(String body) {
        Require.notBlank(body, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "创建业务订单请求体不能为空");
        try {
            return objectMapper.readValue(body, CreatePaymentOpenOrderCommand.class);
        } catch (JsonProcessingException ex) {
            return Require.fail(PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "创建业务订单请求体不是有效 JSON", ex);
        }
    }

    private CreatePaymentOpenPayCommand parsePayCommand(String body) {
        Require.notBlank(body, PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "发起支付请求体不能为空");
        try {
            CreatePaymentOpenPayCommand command = objectMapper.readValue(body, CreatePaymentOpenPayCommand.class);
            Require.notNull(command, PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "发起支付请求体不能为空");
            Require.notBlank(command.getMethodCode(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "支付方式编码不能为空");
            return command;
        } catch (JsonProcessingException ex) {
            return Require.fail(PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "发起支付请求体不是有效 JSON", ex);
        }
    }

    private CreatePaymentOpenRefundCommand parseRefundCommand(String body) {
        Require.notBlank(body, PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "发起退款请求体不能为空");
        try {
            CreatePaymentOpenRefundCommand command = objectMapper.readValue(body, CreatePaymentOpenRefundCommand.class);
            Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "发起退款请求体不能为空");
            return command;
        } catch (JsonProcessingException ex) {
            return Require.fail(PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "发起退款请求体不是有效 JSON", ex);
        }
    }

    private void validateCreateCommand(CreatePaymentOpenOrderCommand command, PaymentApplicationEntity application) {
        Require.notNull(command, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID);
        Require.notNull(command.getTenantId(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "租户 ID 不能为空");
        Require.isTrue(application.getTenantId().equals(command.getTenantId()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "请求租户与签名租户不一致");
        Require.notBlank(command.getAppId(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "AppId 不能为空");
        Require.isTrue(application.getAppId().equals(command.getAppId().trim()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "请求 AppId 与签名 AppId 不一致");
        Require.notBlank(command.getBizOrderNo(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "业务订单号不能为空");
        Require.notBlank(command.getTitle(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "订单标题不能为空");
        Require.notNull(command.getAmount(), PaymentCode.PAYMENT_AMOUNT_INVALID);
        Require.isTrue(command.getAmount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID);
        Require.notBlank(command.getCurrency(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "币种不能为空");
        Require.isTrue(DEFAULT_CURRENCY.equals(command.getCurrency().trim().toUpperCase(Locale.ROOT)),
                PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "当前仅支持 CNY 币种");
        Require.notNull(command.getExpireMinutes(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "订单有效分钟数不能为空");
        Require.isTrue(command.getExpireMinutes() > 0, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "订单有效分钟数必须大于 0");
        Require.notBlank(command.getNotifyUrl(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "业务通知地址不能为空");
        Require.notBlank(command.getReturnUrl(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "业务返回地址不能为空");
    }

    private PaymentBusinessOrderEntity selectBusinessOrder(PaymentApplicationEntity application, String bizOrderNo) {
        Require.notBlank(bizOrderNo, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "业务订单号不能为空");
        return businessOrderMapper.selectOne(new LambdaQueryWrapper<PaymentBusinessOrderEntity>()
                .eq(PaymentBusinessOrderEntity::getTenantId, application.getTenantId())
                .eq(PaymentBusinessOrderEntity::getAppCode, application.getAppId())
                .eq(PaymentBusinessOrderEntity::getBizOrderNo, bizOrderNo.trim()));
    }

    private PaymentBusinessOrderEntity selectRequiredBusinessOrder(PaymentApplicationEntity application, String bizOrderNo) {
        PaymentBusinessOrderEntity entity = selectBusinessOrder(application, bizOrderNo);
        Require.notNull(entity, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        return entity;
    }

    private PaymentCashierConfigEntity selectDefaultCashier(PaymentApplicationEntity application) {
        PaymentCashierConfigEntity config = cashierConfigMapper.selectOne(new LambdaQueryWrapper<PaymentCashierConfigEntity>()
                .eq(PaymentCashierConfigEntity::getTenantId, application.getTenantId())
                .eq(PaymentCashierConfigEntity::getApplicationId, application.getId())
                .eq(PaymentCashierConfigEntity::getDefaultCashier, 1)
                .eq(PaymentCashierConfigEntity::getStatus, 1)
                .orderByDesc(PaymentCashierConfigEntity::getUpdatedAt)
                .last("limit 1"));
        Require.notNull(config, PaymentCode.PAYMENT_OPENAPI_CASHIER_UNAVAILABLE, "应用没有可用默认收银台");
        return config;
    }

    private PaymentCashierConfigEntity selectCashier(PaymentApplicationEntity application, PaymentBusinessOrderEntity order) {
        PaymentCashierConfigEntity config = cashierConfigMapper.selectOne(new LambdaQueryWrapper<PaymentCashierConfigEntity>()
                .eq(PaymentCashierConfigEntity::getTenantId, application.getTenantId())
                .eq(PaymentCashierConfigEntity::getApplicationId, application.getId())
                .eq(PaymentCashierConfigEntity::getDefaultCashier, 1)
                .eq(PaymentCashierConfigEntity::getStatus, 1)
                .apply("find_in_set({0}, enterprise_subject_ids)", String.valueOf(order.getSubjectId()))
                .orderByDesc(PaymentCashierConfigEntity::getUpdatedAt)
                .last("limit 1"));
        Require.notNull(config, PaymentCode.PAYMENT_OPENAPI_CASHIER_UNAVAILABLE, "业务订单没有可用收银台");
        return config;
    }

    private PaymentOrderVO selectRequiredOpenPaymentOrder(PaymentApplicationEntity application, String payOrderNo) {
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_CASHIER_PAY_INVALID, "支付订单号不能为空");
        PaymentOrderVO paymentOrder = toApi(paymentOrderMapper.selectOpenPaymentOrder(
                application.getTenantId(), application.getAppId(), payOrderNo.trim()), PaymentOrderVO.class);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        paymentOrder.setFlowNo(paymentOrderMapper.selectLatestFlowNo(application.getTenantId(), paymentOrder.getId()));
        return paymentOrder;
    }

    private PaymentOrderVO selectRequiredSuccessfulPaymentOrder(PaymentApplicationEntity application, String bizOrderNo) {
        Require.notBlank(bizOrderNo, PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "业务订单号不能为空");
        PaymentOrderVO paymentOrder = toApi(paymentOrderMapper.selectSuccessfulOpenPaymentOrder(
                application.getTenantId(), application.getAppId(), bizOrderNo.trim()), PaymentOrderVO.class);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND, "原成功支付订单不存在");
        return paymentOrder;
    }

    private PaymentRefundOrderVO selectRequiredOpenRefundOrder(PaymentApplicationEntity application, String bizRefundNo) {
        Require.notBlank(bizRefundNo, PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "业务退款单号不能为空");
        PaymentRefundOrderVO refundOrder = toApi(refundOrderMapper.selectOpenRefundOrder(
                application.getTenantId(), application.getAppId(), bizRefundNo.trim()), PaymentRefundOrderVO.class);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return refundOrder;
    }

    private Long resolveSubjectId(CreatePaymentOpenOrderCommand command, PaymentCashierConfigEntity cashierConfig) {
        List<Long> allowedSubjectIds = parseIds(cashierConfig.getEnterpriseSubjectIds());
        Require.notEmpty(allowedSubjectIds, PaymentCode.PAYMENT_OPENAPI_CASHIER_UNAVAILABLE, "默认收银台缺少企业主体");
        Long subjectId = command.getSubjectId() == null ? allowedSubjectIds.get(0) : command.getSubjectId();
        Require.isTrue(allowedSubjectIds.contains(subjectId), PaymentCode.PAYMENT_OPENAPI_CASHIER_UNAVAILABLE, "企业主体不在默认收银台允许范围内");
        return subjectId;
    }

    private boolean isSameOrder(PaymentBusinessOrderEntity existing, CreatePaymentOpenOrderCommand command) {
        return Objects.equals(existing.getTitle(), command.getTitle().trim())
                && Objects.equals(existing.getAmount(), command.getAmount())
                && Objects.equals(existing.getCurrency(), command.getCurrency().trim().toUpperCase(Locale.ROOT))
                && Objects.equals(existing.getNotifyUrl(), command.getNotifyUrl().trim())
                && Objects.equals(existing.getReturnUrl(), command.getReturnUrl().trim());
    }

    private PaymentOpenBusinessOrderVO toBusinessOrderVO(PaymentBusinessOrderEntity entity) {
        PaymentOpenBusinessOrderVO vo = new PaymentOpenBusinessOrderVO();
        vo.setId(entity.getId());
        vo.setBizOrderNo(entity.getBizOrderNo());
        vo.setAppId(entity.getAppCode());
        vo.setTitle(entity.getTitle());
        vo.setSubjectId(entity.getSubjectId());
        vo.setAmount(entity.getAmount());
        vo.setPaidAmount(entity.getPaidAmount());
        vo.setRefundedAmount(entity.getRefundedAmount());
        vo.setCurrency(entity.getCurrency());
        vo.setStatus(entity.getStatus());
        vo.setExpireTime(entity.getExpireTime());
        vo.setNotifyUrl(entity.getNotifyUrl());
        vo.setReturnUrl(entity.getReturnUrl());
        vo.setExtendInfo(entity.getExtendInfo());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private PaymentOpenPaymentOrderVO toOpenPaymentOrderVO(PaymentApplicationEntity application, PaymentCashierPayResultVO payResult) {
        PaymentOrderVO paymentOrder = selectRequiredOpenPaymentOrder(application, payResult.getPayOrderNo());
        return toOpenPaymentOrderVO(paymentOrder, payResult);
    }

    private PaymentOpenPaymentOrderVO toOpenPaymentOrderVO(PaymentOrderVO paymentOrder, PaymentCashierPayResultVO payResult) {
        PaymentOpenPaymentOrderVO vo = new PaymentOpenPaymentOrderVO();
        vo.setId(paymentOrder.getId());
        vo.setPayOrderNo(paymentOrder.getPayOrderNo());
        vo.setBusinessOrderId(paymentOrder.getBusinessOrderId());
        vo.setBizOrderNo(paymentOrder.getBizOrderNo());
        vo.setAppId(paymentOrder.getAppId());
        vo.setTitle(paymentOrder.getTitle());
        vo.setAmount(paymentOrder.getAmount());
        vo.setCurrency(paymentOrder.getCurrency());
        vo.setStatus(paymentOrder.getStatus());
        vo.setMethodCode(paymentOrder.getMethodCode());
        vo.setMethodName(paymentOrder.getMethodName());
        vo.setChannelCode(paymentOrder.getChannelCode());
        vo.setChannelName(paymentOrder.getChannelName());
        vo.setChannelMerchantNo(paymentOrder.getChannelMerchantNo());
        vo.setContractId(paymentOrder.getContractId());
        vo.setContractCapabilityId(paymentOrder.getContractCapabilityId());
        vo.setRouteRuleId(paymentOrder.getRouteRuleId());
        vo.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        vo.setSuccessFlag(paymentOrder.getSuccessFlag());
        vo.setPayTime(paymentOrder.getPayTime());
        vo.setExpireTime(paymentOrder.getExpireTime());
        vo.setFlowNo(paymentOrder.getFlowNo());
        if (payResult != null) {
            vo.setMaterial(payResult.getMaterial());
        }
        return vo;
    }

    private PaymentOpenRefundOrderVO toOpenRefundOrderVO(PaymentRefundOrderVO refundOrder) {
        PaymentOpenRefundOrderVO vo = new PaymentOpenRefundOrderVO();
        vo.setId(refundOrder.getId());
        vo.setRefundOrderNo(refundOrder.getRefundOrderNo());
        vo.setBizRefundNo(refundOrder.getBizRefundNo());
        vo.setPaymentOrderId(refundOrder.getPaymentOrderId());
        vo.setPayOrderNo(refundOrder.getPayOrderNo());
        vo.setBizOrderNo(refundOrder.getBizOrderNo());
        vo.setAppId(refundOrder.getAppId());
        vo.setRefundAmount(refundOrder.getRefundAmount());
        vo.setCurrency(refundOrder.getCurrency());
        vo.setReason(refundOrder.getReason());
        vo.setStatus(refundOrder.getStatus());
        vo.setMethodCode(refundOrder.getMethodCode());
        vo.setChannelCode(refundOrder.getChannelCode());
        vo.setChannelTradeNo(refundOrder.getChannelTradeNo());
        vo.setChannelRefundNo(refundOrder.getChannelRefundNo());
        vo.setRefundTime(refundOrder.getRefundTime());
        vo.setFlowNo(refundOrder.getFlowNo());
        return vo;
    }

    private PaymentOpenReceiptVO toOpenReceiptVO(PaymentOrderVO paymentOrder) {
        PaymentOpenReceiptVO vo = new PaymentOpenReceiptVO();
        vo.setReceiptNo(receiptNo(paymentOrder));
        vo.setBizOrderNo(paymentOrder.getBizOrderNo());
        vo.setPayOrderNo(paymentOrder.getPayOrderNo());
        vo.setAppId(paymentOrder.getAppId());
        vo.setTitle(paymentOrder.getTitle());
        vo.setAmount(paymentOrder.getAmount());
        vo.setCurrency(paymentOrder.getCurrency());
        vo.setStatus(paymentOrder.getStatus());
        vo.setMethodCode(paymentOrder.getMethodCode());
        vo.setMethodName(paymentOrder.getMethodName());
        vo.setChannelCode(paymentOrder.getChannelCode());
        vo.setChannelName(paymentOrder.getChannelName());
        vo.setChannelMerchantNo(paymentOrder.getChannelMerchantNo());
        vo.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        vo.setFlowNo(paymentOrder.getFlowNo());
        vo.setPayTime(paymentOrder.getPayTime());
        vo.setCreateTime(paymentOrder.getCreateTime());
        vo.setIssuedTime(paymentOrder.getPayTime() == null ? paymentOrder.getUpdateTime() : paymentOrder.getPayTime());
        return vo;
    }

    private String receiptNo(PaymentOrderVO paymentOrder) {
        return "RCPT-" + paymentOrder.getBizOrderNo() + "-" + paymentOrder.getPayOrderNo();
    }

    private String writeExtendInfo(Object extendInfo) {
        if (extendInfo == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(extendInfo);
        } catch (JsonProcessingException ex) {
            return Require.fail(PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID, "业务扩展信息不是有效 JSON", ex);
        }
    }

    private List<Long> parseIds(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return List.of();
        }
        return java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private long parseTimestamp(String timestamp) {
        try {
            return Long.parseLong(timestamp.trim());
        } catch (NumberFormatException ex) {
            return Require.fail(PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "timestamp 不是有效数字", ex);
        }
    }

    private void bindOpenApiContext(PaymentApplicationEntity application) {
        MangoContextHolder.update(snapshot -> snapshot
                .withTenantId(String.valueOf(application.getTenantId()))
                .withRequest(null, null, String.valueOf(application.getTenantId()), application.getAppId(), null));
    }

    private String normalizeBody(String body) {
        return body == null ? EMPTY_BODY : body;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
