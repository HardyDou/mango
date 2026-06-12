package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.vo.PaymentCashierApplicationVO;
import io.mango.payment.api.vo.PaymentCashierDisplayVO;
import io.mango.payment.api.vo.PaymentCashierMethodVO;
import io.mango.payment.api.vo.PaymentCashierOrderVO;
import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentCashierSessionVO;
import io.mango.payment.api.vo.PaymentCashierSubjectVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentCashierConfig;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentCashierConfigMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelContractCapabilityMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.model.PaymentCashierPayResultRow;
import io.mango.payment.core.model.PaymentCashierRouteMatch;
import io.mango.payment.core.model.Money;
import io.mango.payment.core.service.IPaymentCashierService;
import io.mango.payment.core.service.IPaymentChannelAdapter;
import io.mango.payment.core.service.PaymentChannelAdapterRegistry;
import io.mango.payment.core.service.PaymentChannelOrderQueryService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentNumberService;
import io.mango.payment.core.service.PaymentOrderStatusFlowService;
import io.mango.payment.core.service.PaymentOrderStateService;
import io.mango.payment.core.service.PaymentSensitiveValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PaymentCashierServiceImpl implements IPaymentCashierService {

    private final PaymentCashierConfigMapper cashierConfigMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentEnterpriseSubjectMapper enterpriseSubjectMapper;
    private final PaymentMethodMapper methodMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentChannelContractMapper channelContractMapper;
    private final PaymentChannelContractCapabilityMapper contractCapabilityMapper;
    private final PaymentChannelAdapterRegistry channelAdapterRegistry;
    private final PaymentOrderStateService orderStateService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentChannelOrderQueryService channelOrderQueryService;
    private final ObjectMapper objectMapper;
    private final PaymentSensitiveValueService sensitiveValueService;
    private final PaymentNumberService numberService;

    @Override
    public R<PaymentCashierSessionVO> detailSession(Long cashierConfigId, Long businessOrderId) {
        return withTenantContext(resolveTenantIdByCashierConfig(cashierConfigId), () -> detailSessionInContext(cashierConfigId, businessOrderId));
    }

    private R<PaymentCashierSessionVO> detailSessionInContext(Long cashierConfigId, Long businessOrderId) {
        PaymentCashierConfig config = selectCashierConfig(cashierConfigId);
        PaymentApplication application = selectApplication(config.getApplicationId());
        BusinessOrderRow order = businessOrderId == null ? selectLatestPayableOrder(application, config) : selectBusinessOrder(businessOrderId);
        PaymentEnterpriseSubject subject = selectSubject(config, order);
        List<PaymentCashierMethodVO> methods = availableMethods(config, application, subject, order == null ? null : order.amount());
        return R.ok(toSession(config, application, subject, order, methods));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<PaymentCashierPayResultVO> pay(PaymentCashierPayCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "收银台支付命令不能为空");
        return withTenantContext(resolveTenantIdByCashierConfig(command.getCashierConfigId()), () -> payInContext(command));
    }

    private R<PaymentCashierPayResultVO> payInContext(PaymentCashierPayCommand command) {
        PaymentCashierConfig config = selectCashierConfig(command.getCashierConfigId());
        PaymentApplication application = selectApplication(config.getApplicationId());
        BusinessOrderRow order = command.getBusinessOrderId() == null ? selectLatestPayableOrder(application, config) : selectBusinessOrder(command.getBusinessOrderId());
        Require.notNull(order, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_PAYABLE.getCode(), "缺少可支付业务订单");
        long paymentAmount = Money.cents(order.amount()).toPositiveCents("付款金额");
        orderStateService.requireBusinessOrderPayable(order.status(), order.expireTime());
        PaymentEnterpriseSubject subject = selectSubject(config, order);
        List<PaymentCashierMethodVO> methods = availableMethods(config, application, subject, order.amount());
        PaymentCashierMethodVO method = methods.stream()
                .filter(item -> item.getMethodCode().equals(PaymentContextSupport.trimToNull(command.getMethodCode())))
                .findFirst()
                .orElse(null);
        Require.notNull(method, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "当前订单不可使用该支付方式");
        Long methodId = methodId(method.getMethodCode());
        Require.isTrue(countSuccessfulPaymentOrders(order.id()) == 0, PaymentCode.PAYMENT_BUSINESS_ORDER_ALREADY_PAID);
        String processingPayOrderNo = selectProcessingPayOrderNo(order.id(), methodId);
        if (processingPayOrderNo != null) {
            return payResultInContext(processingPayOrderNo);
        }

        LocalDateTime requestTime = LocalDateTime.now();
        String payOrderNo = numberService.next(PaymentNumberService.PAY_ORDER_NO);
        PaymentOrderEntity paymentOrder = newPaymentOrder(config, method, order, methodId, paymentAmount, payOrderNo);
        insertPaymentOrder(paymentOrder);
        recordPaymentCreatedFlow(paymentOrder, payOrderNo, requestTime);

        ChannelPaymentApply channelApply;
        try {
            channelApply = applyChannelPayment(command, method, subject, order, payOrderNo);
        } catch (BizException ex) {
            markPaymentApplyFailed(paymentOrder, payOrderNo, requestTime, ex.getMessage());
            return R.fail(ex.getCode(), ex.getMessage());
        }
        IPaymentChannelAdapter.PaymentApplyResult channelResult = channelApply.result();
        String status = normalizeInitialPaymentStatus(channelResult.status());
        orderStateService.requireNewPaymentResultStatus(status);
        PaymentCashierPayMaterialVO material = channelResult.material();
        Require.notNull(material, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "通道支付物料不能为空");
        updatePaymentApplyResult(paymentOrder, payOrderNo, channelResult, status, material);
        channelApply.adapter().afterPaymentOrderCreated(channelApply.command(), channelResult, paymentOrder);
        recordPaymentApplyStatusFlow(paymentOrder, payOrderNo, requestTime, status);
        int updated = businessOrderMapper.touchCashierPayingOrder(PaymentContextSupport.currentTenantId(), order.id());
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED);

        PaymentCashierPayResultVO result = new PaymentCashierPayResultVO();
        result.setPayOrderNo(payOrderNo);
        result.setFlowNo(null);
        result.setStatus(status);
        result.setChannelCode(method.getChannelCode());
        result.setChannelName(method.getChannelName());
        result.setMethodCode(method.getMethodCode());
        result.setMethodName(method.getMethodName());
        result.setAmount(paymentAmount);
        result.setPaidTime(null);
        result.setMaterial(material);
        return R.ok(result);
    }

    private PaymentOrderEntity newPaymentOrder(
            PaymentCashierConfig config,
            PaymentCashierMethodVO method,
            BusinessOrderRow order,
            Long methodId,
            long paymentAmount,
            String payOrderNo) {
        PaymentOrderEntity paymentOrder = new PaymentOrderEntity();
        paymentOrder.setPayOrderNo(payOrderNo);
        paymentOrder.setBusinessOrderId(order.id());
        paymentOrder.setCashierConfigId(config.getId());
        paymentOrder.setChannelId(method.getChannelId());
        paymentOrder.setChannelCode(method.getChannelCode());
        paymentOrder.setChannelMerchantNo(method.getChannelMerchantNo());
        paymentOrder.setContractId(method.getContractId());
        paymentOrder.setContractCapabilityId(method.getContractCapabilityId());
        paymentOrder.setRouteRuleId(method.getRouteRuleId());
        paymentOrder.setMethodId(methodId);
        paymentOrder.setAmount(paymentAmount);
        paymentOrder.setStatus(PaymentOrderStatusEnum.CREATED.getCode());
        paymentOrder.setSuccessFlag(0);
        paymentOrder.setPayTime(null);
        paymentOrder.setExpireTime(order.expireTime());
        paymentOrder.setTenantId(PaymentContextSupport.currentTenantId());
        return paymentOrder;
    }

    private void updatePaymentApplyResult(
            PaymentOrderEntity paymentOrder,
            String payOrderNo,
            IPaymentChannelAdapter.PaymentApplyResult channelResult,
            String status,
            PaymentCashierPayMaterialVO material) {
        int updated = paymentOrderMapper.updateCreatedApplyResult(
                PaymentContextSupport.currentTenantId(),
                paymentOrder.getId(),
                status,
                channelResult.channelTradeNo(),
                writeMaterial(material));
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付订单状态已变化");
        paymentOrder.setStatus(status);
        paymentOrder.setChannelTradeNo(channelResult.channelTradeNo());
        paymentOrder.setPaymentMaterialJson(writeMaterial(material));
    }

    private void markPaymentApplyFailed(PaymentOrderEntity paymentOrder, String payOrderNo, LocalDateTime requestTime, String message) {
        int updated = paymentOrderMapper.markCreatedApplyFailed(
                PaymentContextSupport.currentTenantId(),
                paymentOrder.getId());
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付订单状态已变化");
        statusFlowService.record(
                PaymentContextSupport.currentTenantId(),
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                paymentOrder.getId(),
                payOrderNo,
                PaymentOrderStatusEnum.CREATED.getCode(),
                PaymentOrderStatusEnum.FAILED.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CASHIER_PAY,
                payOrderNo,
                requestTime,
                "收银台支付请求提交通道失败：" + PaymentContextSupport.trimToNull(message));
    }

    private void recordPaymentCreatedFlow(
            PaymentOrderEntity paymentOrder,
            String payOrderNo,
            LocalDateTime requestTime) {
        statusFlowService.record(
                PaymentContextSupport.currentTenantId(),
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                paymentOrder.getId(),
                payOrderNo,
                null,
                PaymentOrderStatusEnum.CREATED.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CASHIER_PAY,
                payOrderNo,
                requestTime,
                "收银台生成支付订单");
    }

    private void recordPaymentApplyStatusFlow(
            PaymentOrderEntity paymentOrder,
            String payOrderNo,
            LocalDateTime requestTime,
            String status) {
        if (!PaymentOrderStatusEnum.CREATED.getCode().equals(status)) {
            statusFlowService.record(
                    PaymentContextSupport.currentTenantId(),
                    PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                    paymentOrder.getId(),
                    payOrderNo,
                    PaymentOrderStatusEnum.CREATED.getCode(),
                    PaymentOrderStatusEnum.PAYING.getCode(),
                    PaymentOrderStatusFlowService.SOURCE_CASHIER_PAY,
                    payOrderNo,
                    requestTime,
                    "收银台支付请求已提交，等待通道回调、主动查单或对账补偿推进");
        }
    }

    @Override
    public R<PaymentCashierPayResultVO> payResult(String payOrderNo) {
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        return withTenantContext(resolveTenantIdByPayOrderNo(payOrderNo), () -> payResultInContext(payOrderNo));
    }

    private R<PaymentCashierPayResultVO> payResultInContext(String payOrderNo) {
        PaymentCashierPayResultRow row = paymentOrderMapper.selectCashierPayResult(PaymentContextSupport.currentTenantId(), payOrderNo);
        Require.notNull(row, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        PaymentCashierMethodVO method = new PaymentCashierMethodVO();
        method.setMethodCode(row.getMethodCode());
        method.setMethodName(row.getMethodName());
        method.setPaymentMaterialType(row.getPaymentMaterialType());
        PaymentCashierPayResultVO result = new PaymentCashierPayResultVO();
        Long paymentOrderId = row.getPaymentOrderId();
        result.setPayOrderNo(row.getPayOrderNo());
        result.setFlowNo(selectFlowNo(paymentOrderId));
        result.setStatus(row.getStatus());
        result.setChannelCode(row.getChannelCode());
        result.setChannelName(row.getChannelName());
        result.setMethodCode(method.getMethodCode());
        result.setMethodName(method.getMethodName());
        result.setAmount(row.getAmount());
        if ("SUCCESS".equals(result.getStatus())) {
            result.setPaidTime(row.getUpdatedAt());
        }
        result.setMaterial(readMaterial(row.getPaymentMaterialJson()));
        return R.ok(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<PaymentCashierPayResultVO> syncPayResult(String payOrderNo) {
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        return withTenantContext(resolveTenantIdByPayOrderNo(payOrderNo), () -> syncPayResultInContext(payOrderNo));
    }

    private R<PaymentCashierPayResultVO> syncPayResultInContext(String payOrderNo) {
        PaymentCashierPayResultVO current = payResultInContext(payOrderNo).getData();
        if (!PaymentOrderStatusEnum.SUCCESS.getCode().equals(current.getStatus())
                && !PaymentOrderStatusEnum.FAILED.getCode().equals(current.getStatus())
                && !PaymentOrderStatusEnum.CLOSED.getCode().equals(current.getStatus())) {
            channelOrderQueryService.queryChannelPayment(payOrderNo);
        }
        return payResultInContext(payOrderNo);
    }

    private <T> T withTenantContext(Long tenantId, Supplier<T> supplier) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_CASHIER_CONFIG_NOT_FOUND.getCode(), "收银台租户上下文不存在");
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(String.valueOf(tenantId)));
            return supplier.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private Long resolveTenantIdByCashierConfig(Long cashierConfigId) {
        Require.notNull(cashierConfigId, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台配置 ID 不能为空");
        PaymentCashierConfig config = cashierConfigMapper.selectByIdIgnoreTenant(cashierConfigId);
        Require.notNull(config, PaymentCode.PAYMENT_CASHIER_CONFIG_NOT_FOUND);
        return config.getTenantId();
    }

    private Long resolveTenantIdByPayOrderNo(String payOrderNo) {
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        PaymentOrderEntity order = paymentOrderMapper.selectByPayOrderNo(payOrderNo);
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        return order.getTenantId();
    }

    private PaymentCashierConfig selectCashierConfig(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台配置 ID 不能为空");
        PaymentCashierConfig config = cashierConfigMapper.selectById(id);
        Require.notNull(config, PaymentCode.PAYMENT_CASHIER_CONFIG_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(config.getTenantId()), PaymentCode.PAYMENT_CASHIER_CONFIG_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(config.getStatus()), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台配置未启用");
        return config;
    }

    private PaymentApplication selectApplication(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "支付应用 ID 不能为空");
        PaymentApplication application = applicationMapper.selectById(id);
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(application.getTenantId()), PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(application.getStatus()), PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "支付应用未启用");
        return application;
    }

    private PaymentEnterpriseSubject selectSubject(PaymentCashierConfig config, BusinessOrderRow order) {
        List<Long> allowedSubjectIds = parseIds(config.getEnterpriseSubjectIds());
        Long subjectId = order == null ? allowedSubjectIds.stream().findFirst().orElse(null) : order.subjectId();
        Require.notNull(subjectId, PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台缺少收款主体");
        Require.isTrue(allowedSubjectIds.contains(subjectId), PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收款主体不在收银台允许范围内");
        PaymentEnterpriseSubject subject = enterpriseSubjectMapper.selectById(subjectId);
        Require.notNull(subject, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND.getCode(), "收款主体不存在");
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(subject.getTenantId()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND.getCode(), "收款主体不存在");
        Require.isTrue(Integer.valueOf(1).equals(subject.getStatus()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID.getCode(), "收款主体未启用");
        return subject;
    }

    private BusinessOrderRow selectBusinessOrder(Long businessOrderId) {
        Require.notNull(businessOrderId, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "业务订单 ID 不能为空");
        PaymentBusinessOrderEntity entity = businessOrderMapper.selectCashierBusinessOrder(PaymentContextSupport.currentTenantId(), businessOrderId);
        Require.notNull(entity, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        return businessOrderRow(entity);
    }

    private BusinessOrderRow selectLatestPayableOrder(PaymentApplication application, PaymentCashierConfig config) {
        List<Long> subjectIds = parseIds(config.getEnterpriseSubjectIds());
        if (subjectIds.isEmpty()) {
            return null;
        }
        List<PaymentBusinessOrderEntity> rows = businessOrderMapper.selectLatestPayableCashierOrder(
                PaymentContextSupport.currentTenantId(),
                application.getAppId(),
                legacyAppCode(application.getAppId()),
                subjectIds);
        return rows.isEmpty() ? null : businessOrderRow(rows.get(0));
    }

    private BusinessOrderRow businessOrderRow(PaymentBusinessOrderEntity entity) {
        return new BusinessOrderRow(
                entity.getId(),
                entity.getBizOrderNo(),
                entity.getAppCode(),
                entity.getSubjectId(),
                entity.getTitle(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getReturnUrl(),
                entity.getExpireTime());
    }

    private PaymentCashierSessionVO toSession(
            PaymentCashierConfig config,
            PaymentApplication application,
            PaymentEnterpriseSubject subject,
            BusinessOrderRow order,
            List<PaymentCashierMethodVO> methods) {
        PaymentCashierSessionVO session = new PaymentCashierSessionVO();
        session.setCashierConfigId(config.getId());
        session.setCashierName(config.getCashierName());
        session.setPreview(order == null);
        session.setStatus(order == null ? "PREVIEW" : order.status());
        session.setDefaultMethodCode(config.getDefaultMethodCode());
        session.setReturnUrl(resolveReturnUrl(order, config));
        session.setServerTime(LocalDateTime.now());
        session.setDisplay(display(config));
        session.setApplication(application(application));
        session.setSubject(subject(subject));
        session.setOrder(order(order, config));
        session.setMethods(methods);
        return session;
    }

    private PaymentCashierDisplayVO display(PaymentCashierConfig config) {
        PaymentCashierDisplayVO display = new PaymentCashierDisplayVO();
        Map<String, String> values = parseFlatJson(config.getDisplayConfig());
        display.setLogoFileId(parseLong(values.get("logoFileId")));
        display.setTitle(config.getCashierName());
        display.setSubtitle(values.get("subtitle"));
        display.setHelpText(values.get("helpText"));
        return display;
    }

    private PaymentCashierApplicationVO application(PaymentApplication application) {
        PaymentCashierApplicationVO vo = new PaymentCashierApplicationVO();
        vo.setId(application.getId());
        vo.setAppId(application.getAppId());
        vo.setAppName(application.getAppName());
        return vo;
    }

    private PaymentCashierSubjectVO subject(PaymentEnterpriseSubject subject) {
        PaymentCashierSubjectVO vo = new PaymentCashierSubjectVO();
        vo.setId(subject.getId());
        vo.setSubjectName(subject.getSubjectName());
        vo.setCreditCode(sensitiveValueService.mask(subject.getCreditCode(), 4, 4));
        vo.setBankAccountNo(sensitiveValueService.mask(subject.getBankAccountNo(), 4, 4));
        vo.setBankName(subject.getBankName());
        return vo;
    }

    private PaymentCashierOrderVO order(BusinessOrderRow order, PaymentCashierConfig config) {
        PaymentCashierOrderVO vo = new PaymentCashierOrderVO();
        if (order == null) {
            vo.setOrderTitle(config.getCashierName());
            vo.setStatus("PREVIEW");
            vo.setCurrency("CNY");
            return vo;
        }
        vo.setBusinessOrderId(order.id());
        vo.setBizOrderNo(order.bizOrderNo());
        vo.setOrderTitle(order.bizOrderNo());
        vo.setAmount(order.amount());
        vo.setCurrency(order.currency());
        vo.setStatus(order.status());
        vo.setReturnUrl(order.returnUrl());
        vo.setExpireTime(order.expireTime());
        return vo;
    }

    private String resolveReturnUrl(BusinessOrderRow order, PaymentCashierConfig config) {
        String orderReturnUrl = order == null ? null : PaymentContextSupport.trimToNull(order.returnUrl());
        if (orderReturnUrl != null) {
            return orderReturnUrl;
        }
        return PaymentContextSupport.trimToNull(config.getResultReturnUrl());
    }

    private List<PaymentCashierMethodVO> availableMethods(
            PaymentCashierConfig config,
            PaymentApplication application,
            PaymentEnterpriseSubject subject,
            Long amount) {
        List<String> methodCodes = orderedMethodCodes(config);
        Set<String> seen = new HashSet<>();
        List<PaymentCashierMethodVO> methods = new ArrayList<>();
        for (String methodCode : methodCodes) {
            if (!seen.add(methodCode)) {
                continue;
            }
            PaymentMethod method = selectMethod(methodCode);
            if (method == null || !Integer.valueOf(1).equals(method.getStatus())) {
                continue;
            }
            PaymentCashierRouteMatch route = routeMatch(application, subject, method, amount);
            if (route == null) {
                continue;
            }
            methods.add(toMethodVO(method, route, Objects.equals(method.getMethodCode(), config.getDefaultMethodCode())));
        }
        methods.sort(java.util.Comparator
                .comparing((PaymentCashierMethodVO item) -> item.getCategorySort() == null ? 0 : item.getCategorySort())
                .thenComparing(item -> methodCodes.indexOf(item.getMethodCode())));
        if (methods.stream().noneMatch(item -> Boolean.TRUE.equals(item.getSelected())) && !methods.isEmpty()) {
            methods.get(0).setSelected(true);
        }
        return methods;
    }

    private PaymentCashierMethodVO toMethodVO(PaymentMethod method, PaymentCashierRouteMatch route, boolean selected) {
        PaymentCashierMethodVO vo = new PaymentCashierMethodVO();
        vo.setMethodCode(method.getMethodCode());
        vo.setMethodName(method.getMethodName());
        vo.setCategoryCode(cashierGroupCode(method));
        vo.setCategoryName(cashierGroupName(method));
        vo.setCategorySort(method.getCashierGroupSort() == null ? 0 : method.getCashierGroupSort());
        vo.setAccountNature(method.getAccountNature());
        vo.setInstrumentType(method.getInstrumentType());
        vo.setInteractionType(method.getInteractionType());
        vo.setPaymentMaterialType(method.getPaymentMaterialType());
        vo.setIconFileId(method.getIconFileId());
        vo.setDescription(method.getDescription());
        vo.setChannelId(route.getChannelId());
        vo.setChannelCode(route.getChannelCode());
        vo.setChannelName(route.getChannelName());
        vo.setContractId(route.getContractId());
        vo.setContractName(route.getContractName());
        vo.setContractCapabilityId(route.getContractCapabilityId());
        vo.setRouteRuleId(route.getRouteRuleId());
        vo.setChannelMerchantNo(route.getChannelMerchantNo());
        vo.setSelected(selected);
        return vo;
    }

    private PaymentCashierRouteMatch routeMatch(PaymentApplication application, PaymentEnterpriseSubject subject, PaymentMethod method, Long amount) {
        Long resolvedAmount = amount == null ? 1L : amount;
        PaymentCashierRouteMatch routed = contractCapabilityMapper.selectRoutedCashierCapability(
                PaymentContextSupport.currentTenantId(),
                application.getId(),
                subject.getId(),
                method.getMethodCode(),
                terminalType(method),
                resolvedAmount);
        if (routed != null) {
            return routed;
        }
        if (!routeFallbackAllowed(application, subject, method)) {
            return null;
        }
        return contractCapabilityMapper.selectFallbackCashierCapability(
                PaymentContextSupport.currentTenantId(),
                subject.getId(),
                method.getMethodCode(),
                terminalType(method),
                resolvedAmount);
    }

    private boolean routeFallbackAllowed(PaymentApplication application, PaymentEnterpriseSubject subject, PaymentMethod method) {
        long count = contractCapabilityMapper.countFallbackDisabledRouteRules(
                PaymentContextSupport.currentTenantId(),
                application.getId(),
                subject.getId(),
                method.getMethodCode(),
                terminalType(method));
        return count == 0;
    }

    private String terminalType(PaymentMethod method) {
        List<String> terminals = parseCodes(method.getTerminalScope());
        return terminals.contains("WEB") ? "WEB" : terminals.stream().findFirst().orElse("WEB");
    }

    private List<String> orderedMethodCodes(PaymentCashierConfig config) {
        List<String> visible = parseCodes(config.getMethodCodes());
        List<String> order = parseCodes(config.getMethodDisplayOrder());
        if (order.isEmpty()) {
            return visible;
        }
        List<String> result = new ArrayList<>(order.stream().filter(visible::contains).toList());
        visible.stream().filter(item -> !result.contains(item)).forEach(result::add);
        return result;
    }

    private PaymentMethod selectMethod(String methodCode) {
        return methodMapper.selectOne(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethod::getMethodCode, methodCode));
    }

    private Long methodId(String methodCode) {
        PaymentMethod method = selectMethod(methodCode);
        Require.notNull(method, PaymentCode.PAYMENT_METHOD_NOT_FOUND);
        return method.getId();
    }

    private long countSuccessfulPaymentOrders(Long businessOrderId) {
        return paymentOrderMapper.countSuccessfulCashierOrders(PaymentContextSupport.currentTenantId(), businessOrderId);
    }

    private String selectProcessingPayOrderNo(Long businessOrderId, Long methodId) {
        return paymentOrderMapper.selectProcessingPayOrderNo(PaymentContextSupport.currentTenantId(), businessOrderId, methodId);
    }

    private String selectFlowNo(Long paymentOrderId) {
        return paymentOrderMapper.selectLatestFlowNo(PaymentContextSupport.currentTenantId(), paymentOrderId);
    }

    private void insertPaymentOrder(PaymentOrderEntity paymentOrder) {
        try {
            paymentOrderMapper.insert(paymentOrder);
        } catch (DuplicateKeyException ex) {
            throw new BizException(
                    PaymentCode.PAYMENT_BUSINESS_ORDER_ALREADY_PAID.getCode(),
                    PaymentCode.PAYMENT_BUSINESS_ORDER_ALREADY_PAID.getMessage(),
                    ex);
        }
    }

    private ChannelPaymentApply applyChannelPayment(
            PaymentCashierPayCommand cashierCommand,
            PaymentCashierMethodVO method,
            PaymentEnterpriseSubject subject,
            BusinessOrderRow order,
            String payOrderNo) {
        Require.notBlank(method.getChannelCode(), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道编码不能为空");
        Require.notNull(method.getContractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "支付通道签约 ID 不能为空");
        validateBankPaymentRequest(cashierCommand, method);
        IPaymentChannelAdapter adapter = channelAdapterRegistry.requireAdapter(method.getChannelCode());
        IPaymentChannelAdapter.PaymentApplyCommand command = new IPaymentChannelAdapter.PaymentApplyCommand(
                PaymentContextSupport.currentTenantId(),
                method.getChannelCode(),
                method.getContractId(),
                contractConfigValuesJson(method.getContractId()),
                payOrderNo,
                order.bizOrderNo(),
                method.getMethodCode(),
                method.getMethodName(),
                method.getPaymentMaterialType(),
                Money.cents(order.amount()).toPositiveCents("付款金额"),
                order.currency(),
                order.title(),
                order.expireTime(),
                subject.getId(),
                subject.getSubjectName(),
                isEbankMethod(method) ? PaymentContextSupport.trimToNull(cashierCommand.getBankCode()) : null,
                isEbankMethod(method) ? PaymentContextSupport.trimToNull(cashierCommand.getBankName()) : null,
                isEbankMethod(method) ? PaymentContextSupport.trimToNull(cashierCommand.getPayerAccountNo()) : null,
                isEbankMethod(method) ? PaymentContextSupport.trimToNull(cashierCommand.getPayerName()) : null,
                PaymentContextSupport.trimToNull(cashierCommand.getClientIp()));
        IPaymentChannelAdapter.PaymentApplyResult channelResult = adapter.applyPayment(command);
        Require.notNull(channelResult, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "通道支付结果不能为空");
        return new ChannelPaymentApply(adapter, command, channelResult);
    }

    private void validateBankPaymentRequest(PaymentCashierPayCommand command, PaymentCashierMethodVO method) {
        if (!isEbankMethod(method)) {
            return;
        }
        Require.notBlank(command.getBankCode(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "请选择支付银行");
        Require.notBlank(command.getBankName(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "请选择支付银行");
        Require.notBlank(command.getPayerAccountNo(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "请输入付款账号或卡号");
        Require.notBlank(command.getPayerName(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "请输入付款户名");
    }

    private String contractConfigValuesJson(Long contractId) {
        PaymentChannelContract contract = channelContractMapper.selectById(contractId);
        Require.notNull(contract, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(contract.getTenantId()),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        return contract.getConfigValuesJson();
    }

    private boolean isEbankMethod(PaymentCashierMethodVO method) {
        return "EBANK".equals(method.getInstrumentType()) || "BANK_GATEWAY".equals(method.getInteractionType());
    }

    private String normalizeInitialPaymentStatus(String channelStatus) {
        if (PaymentOrderStatusEnum.SUCCESS.getCode().equals(channelStatus)) {
            return PaymentOrderStatusEnum.PAYING.getCode();
        }
        return channelStatus;
    }

    private String writeMaterial(PaymentCashierPayMaterialVO material) {
        try {
            return objectMapper.writeValueAsString(material);
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付物料序列化失败", ex);
        }
    }

    private PaymentCashierPayMaterialVO readMaterial(String materialJson) {
        Require.notBlank(materialJson, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单缺少支付物料");
        try {
            return objectMapper.readValue(materialJson, PaymentCashierPayMaterialVO.class);
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付物料解析失败", ex);
        }
    }

    private String cashierGroupCode(PaymentMethod method) {
        Require.notBlank(
                method.getCashierGroupCode(),
                PaymentCode.PAYMENT_METHOD_INVALID.getCode(),
                "支付方式缺少收银台展示分组编码");
        return method.getCashierGroupCode().trim();
    }

    private String cashierGroupName(PaymentMethod method) {
        Require.notBlank(
                method.getCashierGroupName(),
                PaymentCode.PAYMENT_METHOD_INVALID.getCode(),
                "支付方式缺少收银台展示分组名称");
        return method.getCashierGroupName().trim();
    }

    private List<Long> parseIds(String value) {
        return parseCodes(value).stream().map(Long::valueOf).toList();
    }

    private List<String> parseCodes(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return List.of();
        }
        return java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private Map<String, String> parseFlatJson(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(normalized, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                if (entry.getValue() != null) {
                    result.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return result;
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_CASHIER_CONFIG_INVALID.getCode(), "收银台展示配置不是有效 JSON", ex);
        }
    }

    private Long parseLong(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        return normalized == null ? null : Long.valueOf(normalized);
    }

    private String legacyAppCode(String appId) {
        if ("app_order_center".equals(appId)) {
            return "ORDER_CENTER";
        }
        if ("app_member_center".equals(appId)) {
            return "MEMBER_CENTER";
        }
        return appId;
    }

    private record BusinessOrderRow(
            Long id,
            String bizOrderNo,
            String appCode,
            Long subjectId,
            String title,
            Long amount,
            String currency,
            String status,
            String returnUrl,
            LocalDateTime expireTime
    ) {
    }

    private record ChannelPaymentApply(
            IPaymentChannelAdapter adapter,
            IPaymentChannelAdapter.PaymentApplyCommand command,
            IPaymentChannelAdapter.PaymentApplyResult result
    ) {
    }

}
