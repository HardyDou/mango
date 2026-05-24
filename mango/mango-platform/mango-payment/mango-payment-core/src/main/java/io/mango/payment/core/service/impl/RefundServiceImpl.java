package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.ILocker;
import io.mango.payment.api.command.QueryRefundOrderCommand;
import io.mango.payment.api.command.RefreshRefundStatusCommand;
import io.mango.payment.api.command.RefundCommand;
import io.mango.payment.api.enums.PayBizOrderStatus;
import io.mango.payment.api.enums.RefundOrderStatus;
import io.mango.payment.api.vo.RefundOrderVO;
import io.mango.payment.channel.spi.model.RefundChannelStatus;
import io.mango.payment.core.config.PaymentKvProperties;
import io.mango.payment.core.entity.PayBizOrder;
import io.mango.payment.core.entity.PayPaymentOrder;
import io.mango.payment.core.entity.PayRefundOrder;
import io.mango.payment.core.mapper.PayBizOrderMapper;
import io.mango.payment.core.mapper.PayPaymentOrderMapper;
import io.mango.payment.core.mapper.PayRefundOrderMapper;
import io.mango.payment.core.service.IRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(PaymentKvProperties.class)
public class RefundServiceImpl implements IRefundService {

    private final PayBizOrderMapper bizOrderMapper;
    private final PayPaymentOrderMapper paymentOrderMapper;
    private final PayRefundOrderMapper refundOrderMapper;
    private final PaymentChannelRegistry channelRegistry;
    private final ObjectProvider<IIdempotent> idempotentProvider;
    private final ObjectProvider<ILocker> lockerProvider;
    private final PaymentKvProperties kvProperties;

    @Override
    @Transactional
    public RefundOrderVO refund(RefundCommand command) {
        IIdempotent idempotent = idempotentProvider.getIfAvailable();
        String idempotentKey = "payment:refund:" + command.getBizOrderId() + ":" + command.getIdempotencyKey();
        PayRefundOrder existing = refundOrderMapper.selectByIdempotencyKey(command.getBizOrderId(), command.getIdempotencyKey());
        if (existing != null) {
            return toVO(existing);
        }
        existing = refundOrderMapper.selectByMerchantRefundNo(command.getBizOrderId(), command.getMerchantRefundNo());
        if (existing != null) {
            return toVO(existing);
        }
        if (idempotent != null && idempotent.checkAndMark(idempotentKey, kvProperties.getIdempotentWindowSeconds())) {
            existing = refundOrderMapper.selectByIdempotencyKey(command.getBizOrderId(), command.getIdempotencyKey());
            Require.notNull(existing, 409, "退款请求正在处理中，请稍后查询结果");
            return toVO(existing);
        }
        return withBizOrderLock(command.getBizOrderId(), () -> doRefund(command));
    }

    private RefundOrderVO doRefund(RefundCommand command) {
        PayRefundOrder existing = refundOrderMapper.selectByIdempotencyKey(command.getBizOrderId(), command.getIdempotencyKey());
        if (existing != null) {
            return toVO(existing);
        }
        existing = refundOrderMapper.selectByMerchantRefundNo(command.getBizOrderId(), command.getMerchantRefundNo());
        if (existing != null) {
            return toVO(existing);
        }
        PayBizOrder bizOrder = bizOrderMapper.selectById(command.getBizOrderId());
        Require.notNull(bizOrder, "业务支付单不存在");
        Require.isTrue(currentTenantId().equals(bizOrder.getTenantId()), "业务支付单不存在");
        Require.isTrue(PayBizOrderStatus.PAID.name().equals(bizOrder.getStatus())
                || PayBizOrderStatus.PARTIAL_REFUNDED.name().equals(bizOrder.getStatus()), "只有已支付业务单可以退款");
        Require.isTrue(bizOrder.getRefundedAmount() + command.getRefundAmount() <= bizOrder.getAmount(), "退款累计金额不能超过已支付金额");
        PayPaymentOrder paymentOrder = paymentOrderMapper.selectSuccessByBizOrderId(bizOrder.getId());
        Require.notNull(paymentOrder, "成功支付单不存在");
        PayRefundOrder refundOrder = new PayRefundOrder();
        refundOrder.setId(IdWorker.getId());
        refundOrder.setBizOrderId(bizOrder.getId());
        refundOrder.setPaymentOrderId(paymentOrder.getId());
        refundOrder.setMerchantRefundNo(command.getMerchantRefundNo());
        refundOrder.setIdempotencyKey(command.getIdempotencyKey());
        refundOrder.setRefundAmount(command.getRefundAmount());
        refundOrder.setStatus(RefundOrderStatus.PROCESSING.name());
        refundOrder.setTenantId(bizOrder.getTenantId());
        refundOrder.setCreateTime(LocalDateTime.now());
        refundOrder.setUpdateTime(LocalDateTime.now());
        refundOrderMapper.insert(refundOrder);
        RefundChannelStatus status = channelRegistry.refundProvider(paymentOrder.getChannelCode())
                .refund(refundOrder.getId(), paymentOrder.getId(), command.getRefundAmount());
        refundOrderMapper.updateStatus(refundOrder.getId(), status.status().name(), status.channelRefundNo());
        if (RefundOrderStatus.SUCCESS == status.status()) {
            String bizStatus = bizOrder.getRefundedAmount() + command.getRefundAmount() >= bizOrder.getAmount()
                    ? PayBizOrderStatus.REFUNDED.name() : PayBizOrderStatus.PARTIAL_REFUNDED.name();
            int updated = bizOrderMapper.addRefundedAmount(bizOrder.getId(), command.getRefundAmount(), bizStatus);
            Require.isTrue(updated == 1, "退款累计金额不能超过已支付金额");
        }
        return toVO(refundOrderMapper.selectById(refundOrder.getId()));
    }

    @Override
    public RefundOrderVO queryRefundOrder(QueryRefundOrderCommand command) {
        PayRefundOrder refundOrder = refundOrderMapper.selectById(command.getRefundOrderId());
        if (refundOrder == null || !currentTenantId().equals(refundOrder.getTenantId())) {
            return null;
        }
        return toVO(refundOrder);
    }

    @Override
    @Transactional
    public RefundOrderVO refreshRefundStatus(RefreshRefundStatusCommand command) {
        PayRefundOrder refundOrder = refundOrderMapper.selectById(command.getRefundOrderId());
        Require.notNull(refundOrder, "退款单不存在");
        Require.isTrue(currentTenantId().equals(refundOrder.getTenantId()), "退款单不存在");
        return withBizOrderLock(refundOrder.getBizOrderId(), () -> doRefreshRefundStatus(refundOrder));
    }

    private RefundOrderVO doRefreshRefundStatus(PayRefundOrder refundOrder) {
        PayPaymentOrder paymentOrder = paymentOrderMapper.selectById(refundOrder.getPaymentOrderId());
        Require.notNull(paymentOrder, "支付单不存在");
        RefundChannelStatus status = channelRegistry.refundProvider(paymentOrder.getChannelCode())
                .queryRefund(refundOrder.getId(), refundOrder.getChannelRefundNo());
        refundOrderMapper.updateStatus(refundOrder.getId(), status.status().name(), status.channelRefundNo());
        return toVO(refundOrderMapper.selectById(refundOrder.getId()));
    }

    private <T> T withBizOrderLock(Long bizOrderId, Supplier<T> supplier) {
        ILocker locker = lockerProvider.getIfAvailable();
        if (locker == null) {
            return supplier.get();
        }
        String lockKey = "payment:biz-order:" + bizOrderId;
        Require.isTrue(locker.tryLock(lockKey, kvProperties.getBizOrderLockTtlSeconds()), 409, "业务支付单处理中，请稍后重试");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            registerUnlock(locker, lockKey);
            return supplier.get();
        }
        try {
            return supplier.get();
        } finally {
            locker.unlock(lockKey);
        }
    }

    private void registerUnlock(ILocker locker, String lockKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                locker.unlock(lockKey);
            }
        });
    }

    private RefundOrderVO toVO(PayRefundOrder order) {
        RefundOrderVO vo = new RefundOrderVO();
        vo.setRefundOrderId(order.getId());
        vo.setBizOrderId(order.getBizOrderId());
        vo.setPaymentOrderId(order.getPaymentOrderId());
        vo.setMerchantRefundNo(order.getMerchantRefundNo());
        vo.setRefundAmount(order.getRefundAmount());
        vo.setStatus(RefundOrderStatus.valueOf(order.getStatus()));
        return vo;
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前机构上下文");
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            return Require.fail(400, "当前机构上下文不是有效数字: " + tenantId);
        }
    }
}
