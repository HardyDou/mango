package io.mango.payment.core.service;

import static io.mango.payment.core.model.PaymentProjectionConverter.toApi;

import io.mango.common.result.Require;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.command.CreatePaymentOpenRefundCommand;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplicationEntity;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundApplyCoordinator {

    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentOrderStatePolicy orderStateService;
    private final PaymentOrderStatusFlowRecorder statusFlowService;
    private final PaymentChannelAdapterRegistry channelAdapterRegistry;
    private final PaymentNumberGenerator numberService;
    private final PlatformTransactionManager transactionManager;

    public PaymentOpenRefundOrderVO applyRefund(
            PaymentApplicationEntity application,
            CreatePaymentOpenRefundCommand command,
            String triggerSource,
            String triggerNo,
            boolean notifyBusiness) {
        RefundApplyContext context = inTransaction(() -> prepareRefund(application, command, triggerSource, triggerNo));
        if (context.existingRefundOrder() != null) {
            return toOpenRefundOrderVO(context.existingRefundOrder());
        }
        IPaymentChannelAdapter.RefundApplyResult channelResult;
        try {
            channelResult = applyChannelRefund(
                    application.getTenantId(),
                    context.paymentOrder(),
                    command,
                    context.refundOrderNo());
        } catch (RuntimeException ex) {
            log.warn("退款通道申请结果不确定，保留本地退款单为退款中以便后续查单/回调/对账推进，tenantId={}, appId={}, bizRefundNo={}, refundOrderNo={}",
                    application.getTenantId(), application.getAppId(), command.getBizRefundNo(), context.refundOrderNo(), ex);
            return Require.rethrow(ex);
        }
        String initialStatus = resolveInitialRefundStatus(channelResult);
        orderStateService.requireNewRefundResultStatus(initialStatus);
        inTransaction(() -> {
            int updated = refundOrderMapper.updateRefundApplyResult(
                    application.getTenantId(),
                    context.refundOrder().getId(),
                    channelResult.channelRefundNo(),
                    initialStatus);
            Require.isTrue(updated == 1, PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID, "退款订单状态已变化");
            return null;
        });

        PaymentRefundOrderVO detail = selectRequiredOpenRefundOrder(application, command.getBizRefundNo());
        return toOpenRefundOrderVO(detail);
    }

    private RefundApplyContext prepareRefund(
            PaymentApplicationEntity application,
            CreatePaymentOpenRefundCommand command,
            String triggerSource,
            String triggerNo) {
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        validateRefundCommand(command, application);
        PaymentRefundOrderVO existing = toApi(refundOrderMapper.selectOpenRefundOrder(
                application.getTenantId(), application.getAppId(), command.getBizRefundNo().trim()),
                PaymentRefundOrderVO.class);
        if (existing != null) {
            Require.isTrue(isSameRefund(existing, command), PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT);
            existing.setFlowNo(refundOrderMapper.selectLatestFlowNo(application.getTenantId(), existing.getId()));
            return RefundApplyContext.existing(existing);
        }
        PaymentBusinessOrderEntity businessOrder = selectRequiredBusinessOrder(application, command.getBizOrderNo());
        orderStateService.requireBusinessOrderRefundable(businessOrder.getStatus());
        PaymentOrderVO paymentOrder = selectRequiredSuccessfulPaymentOrder(application, command.getBizOrderNo());
        Long lockedPaymentOrderId = paymentOrderMapper.lockSuccessfulOpenPaymentOrder(application.getTenantId(), paymentOrder.getId());
        Require.notNull(lockedPaymentOrderId, PaymentCode.PAYMENT_ORDER_NOT_FOUND, "原成功支付订单不存在");
        long occupyingRefundAmount = normalizedAmount(refundOrderMapper.sumOccupyingRefundAmount(application.getTenantId(), paymentOrder.getId()));
        orderStateService.requireRefundAmount(command.getRefundAmount(), paymentOrder.getAmount(), occupyingRefundAmount);

        LocalDateTime now = LocalDateTime.now();
        String refundOrderNo = numberService.next(PaymentNumberGenerator.PAY_REFUND_ORDER_NO);
        PaymentRefundOrderEntity refundOrder = new PaymentRefundOrderEntity();
        refundOrder.setRefundOrderNo(refundOrderNo);
        refundOrder.setBizRefundNo(command.getBizRefundNo().trim());
        refundOrder.setPaymentOrderId(paymentOrder.getId());
        refundOrder.setChannelRefundNo(null);
        refundOrder.setRefundAmount(command.getRefundAmount());
        refundOrder.setReason(command.getReason().trim());
        refundOrder.setStatus(PaymentRefundOrderStatusEnum.REFUNDING.getCode());
        refundOrder.setRefundTime(null);
        refundOrder.setTenantId(application.getTenantId());
        refundOrderMapper.insert(refundOrder);
        statusFlowService.record(
                application.getTenantId(),
                PaymentOrderStatusFlowRecorder.ORDER_TYPE_REFUND,
                refundOrder.getId(),
                refundOrderNo,
                null,
                PaymentRefundOrderStatusEnum.REFUNDING.getCode(),
                triggerSource,
                triggerNo,
                now,
                statusFlowRemark(triggerSource));
        return RefundApplyContext.created(paymentOrder, refundOrder, refundOrderNo);
    }

    private void validateRefundCommand(CreatePaymentOpenRefundCommand command, PaymentApplicationEntity application) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID);
        Require.notNull(command.getTenantId(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "租户 ID 不能为空");
        Require.isTrue(application.getTenantId().equals(command.getTenantId()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "请求租户与签名租户不一致");
        Require.notBlank(command.getAppId(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "AppId 不能为空");
        Require.isTrue(application.getAppId().equals(command.getAppId().trim()), PaymentCode.PAYMENT_OPENAPI_AUTH_INVALID, "请求 AppId 与签名 AppId 不一致");
        Require.notBlank(command.getBizOrderNo(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "业务订单号不能为空");
        Require.notBlank(command.getBizRefundNo(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "业务退款单号不能为空");
        Require.notNull(command.getRefundAmount(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "退款金额不能为空");
        Require.isTrue(command.getRefundAmount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID);
        Require.notBlank(command.getReason(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "退款原因不能为空");
    }

    private PaymentBusinessOrderEntity selectRequiredBusinessOrder(PaymentApplicationEntity application, String bizOrderNo) {
        PaymentBusinessOrderEntity entity = businessOrderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentBusinessOrderEntity>()
                .eq(PaymentBusinessOrderEntity::getTenantId, application.getTenantId())
                .eq(PaymentBusinessOrderEntity::getAppCode, application.getAppId())
                .eq(PaymentBusinessOrderEntity::getBizOrderNo, bizOrderNo.trim()));
        Require.notNull(entity, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        return entity;
    }

    private PaymentOrderVO selectRequiredSuccessfulPaymentOrder(PaymentApplicationEntity application, String bizOrderNo) {
        PaymentOrderVO paymentOrder = toApi(paymentOrderMapper.selectSuccessfulOpenPaymentOrder(
                application.getTenantId(), application.getAppId(), bizOrderNo.trim()), PaymentOrderVO.class);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND, "原成功支付订单不存在");
        return paymentOrder;
    }

    private PaymentRefundOrderVO selectRequiredOpenRefundOrder(PaymentApplicationEntity application, String bizRefundNo) {
        PaymentRefundOrderVO refundOrder = toApi(refundOrderMapper.selectOpenRefundOrder(
                application.getTenantId(), application.getAppId(), bizRefundNo.trim()), PaymentRefundOrderVO.class);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return refundOrder;
    }

    private IPaymentChannelAdapter.RefundApplyResult applyChannelRefund(
            String tenantId,
            PaymentOrderVO paymentOrder,
            CreatePaymentOpenRefundCommand command,
            String refundOrderNo) {
        Require.notNull(paymentOrder.getContractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        IPaymentChannelAdapter.RefundApplyResult result = channelAdapterRegistry.requireAdapter(paymentOrder.getChannelCode())
                .applyRefund(new IPaymentChannelAdapter.RefundApplyInput(
                        tenantId,
                        paymentOrder.getChannelCode(),
                        paymentOrder.getContractId(),
                        refundOrderNo,
                        command.getBizRefundNo().trim(),
                        paymentOrder.getPayOrderNo(),
                        paymentOrder.getBizOrderNo(),
                        paymentOrder.getMethodCode(),
                        paymentOrder.getChannelTradeNo(),
                        paymentOrder.getAmount(),
                        command.getRefundAmount(),
                        paymentOrder.getCurrency(),
                        command.getReason().trim()));
        Require.notNull(result, PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID, "通道退款结果不能为空");
        Require.notBlank(result.channelRefundNo(), PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID, "通道退款单号不能为空");
        return result;
    }

    private String resolveInitialRefundStatus(IPaymentChannelAdapter.RefundApplyResult result) {
        String channelStatus = result.status();
        if (PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(channelStatus)) {
            return PaymentRefundOrderStatusEnum.REFUNDING.getCode();
        }
        return channelStatus;
    }

    private boolean isSameRefund(PaymentRefundOrderVO existing, CreatePaymentOpenRefundCommand command) {
        return Objects.equals(existing.getBizOrderNo(), command.getBizOrderNo().trim())
                && Objects.equals(existing.getRefundAmount(), command.getRefundAmount())
                && Objects.equals(PaymentContextSupport.trimToNull(existing.getReason()), command.getReason().trim());
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

    private String statusFlowRemark(String triggerSource) {
        if (PaymentOrderStatusFlowRecorder.SOURCE_MANUAL_REFUND_APPROVAL.equals(triggerSource)) {
            return "后台退款审批通过后创建退款订单";
        }
        return "开放接口创建退款订单";
    }

    private long normalizedAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

    private <T> T inTransaction(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> action.get());
    }

    private record RefundApplyContext(
            PaymentRefundOrderVO existingRefundOrder,
            PaymentOrderVO paymentOrder,
            PaymentRefundOrderEntity refundOrder,
            String refundOrderNo) {

        static RefundApplyContext existing(PaymentRefundOrderVO refundOrder) {
            return new RefundApplyContext(refundOrder, null, null, null);
        }

        static RefundApplyContext created(
                PaymentOrderVO paymentOrder,
                PaymentRefundOrderEntity refundOrder,
                String refundOrderNo) {
            return new RefundApplyContext(null, paymentOrder, refundOrder, refundOrderNo);
        }
    }

}
