package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.ILocker;
import io.mango.payment.api.command.ClosePayBizOrderCommand;
import io.mango.payment.api.command.CreatePayBizOrderCommand;
import io.mango.payment.api.command.PayCommand;
import io.mango.payment.api.command.PaymentNotifyCommand;
import io.mango.payment.api.command.QueryPayBizOrderCommand;
import io.mango.payment.api.command.QueryPaymentOrderCommand;
import io.mango.payment.api.command.RefreshPaymentStatusCommand;
import io.mango.payment.api.enums.PayBizOrderStatus;
import io.mango.payment.api.enums.PaymentOrderStatus;
import io.mango.payment.api.vo.PayBizOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.channel.spi.model.PaymentChannelResponse;
import io.mango.payment.channel.spi.model.PaymentChannelStatus;
import io.mango.payment.core.config.PaymentKvProperties;
import io.mango.payment.core.entity.PayBizOrder;
import io.mango.payment.core.entity.PayNotifyRecord;
import io.mango.payment.core.entity.PayPaymentOrder;
import io.mango.payment.core.mapper.PayBizOrderMapper;
import io.mango.payment.core.mapper.PayNotifyRecordMapper;
import io.mango.payment.core.mapper.PayPaymentOrderMapper;
import io.mango.payment.core.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(PaymentKvProperties.class)
public class PaymentServiceImpl implements IPaymentService {

    private final PayBizOrderMapper bizOrderMapper;
    private final PayPaymentOrderMapper paymentOrderMapper;
    private final PayNotifyRecordMapper notifyRecordMapper;
    private final PaymentChannelRegistry channelRegistry;
    private final ObjectProvider<IIdempotent> idempotentProvider;
    private final ObjectProvider<ILocker> lockerProvider;
    private final PaymentKvProperties kvProperties;

    @Override
    public Long createBizOrder(CreatePayBizOrderCommand command) {
        Long tenantId = currentTenantId();
        PayBizOrder existing = bizOrderMapper.selectByMerchantOrder(command.getAppCode(), command.getMerchantOrderNo(), tenantId);
        if (existing != null) {
            return existing.getId();
        }
        PayBizOrder order = new PayBizOrder();
        order.setId(IdWorker.getId());
        order.setAppCode(command.getAppCode());
        order.setMerchantOrderNo(command.getMerchantOrderNo());
        order.setSubject(command.getSubject());
        order.setAmount(command.getAmount());
        order.setRefundedAmount(0L);
        order.setCurrency(command.getCurrency() == null ? "CNY" : command.getCurrency());
        order.setStatus(PayBizOrderStatus.CREATED.name());
        order.setTenantId(tenantId);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        bizOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional
    public PaymentOrderVO pay(PayCommand command) {
        IIdempotent idempotent = idempotentProvider.getIfAvailable();
        String idempotentKey = "payment:pay:" + command.getBizOrderId() + ":" + command.getIdempotencyKey();
        PayPaymentOrder existing = paymentOrderMapper.selectByIdempotencyKey(command.getBizOrderId(), command.getIdempotencyKey());
        if (existing != null) {
            return toPaymentVO(existing);
        }
        if (idempotent != null && idempotent.checkAndMark(idempotentKey, kvProperties.getIdempotentWindowSeconds())) {
            existing = paymentOrderMapper.selectByIdempotencyKey(command.getBizOrderId(), command.getIdempotencyKey());
            Require.notNull(existing, 409, "支付请求正在处理中，请稍后查询结果");
            return toPaymentVO(existing);
        }
        return withBizOrderLock(command.getBizOrderId(), () -> doPay(command));
    }

    private PaymentOrderVO doPay(PayCommand command) {
        PayPaymentOrder existing = paymentOrderMapper.selectByIdempotencyKey(command.getBizOrderId(), command.getIdempotencyKey());
        if (existing != null) {
            return toPaymentVO(existing);
        }
        PayBizOrder bizOrder = bizOrderMapper.selectById(command.getBizOrderId());
        Require.notNull(bizOrder, "业务支付单不存在");
        Require.isTrue(currentTenantId().equals(bizOrder.getTenantId()), "业务支付单不存在");
        if (PayBizOrderStatus.PAID.name().equals(bizOrder.getStatus())) {
            PayPaymentOrder success = paymentOrderMapper.selectSuccessByBizOrderId(bizOrder.getId());
            if (success != null) {
                return toPaymentVO(success);
            }
        }
        Require.isFalse(PayBizOrderStatus.CLOSED.name().equals(bizOrder.getStatus()), "已关闭业务支付单不能发起支付");
        PayPaymentOrder processing = paymentOrderMapper.selectProcessingByBizOrderId(bizOrder.getId());
        if (processing != null) {
            return toPaymentVO(processing);
        }
        PayPaymentOrder payment = new PayPaymentOrder();
        payment.setId(IdWorker.getId());
        payment.setBizOrderId(bizOrder.getId());
        payment.setPayMethod(command.getPayMethod());
        payment.setIdempotencyKey(command.getIdempotencyKey());
        payment.setAmount(bizOrder.getAmount());
        payment.setStatus(PaymentOrderStatus.PROCESSING.name());
        payment.setTenantId(bizOrder.getTenantId());
        PaymentChannelResponse response = channelRegistry.paymentProvider(command.getPayMethod())
                .pay(payment.getId(), payment.getAmount(), bizOrder.getSubject());
        payment.setChannelCode(response.channelCode());
        payment.setChannelOrderNo(response.channelOrderNo());
        payment.setMaterialType(response.materialType().name());
        payment.setMaterialContent(response.materialContent());
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());
        paymentOrderMapper.insert(payment);
        bizOrderMapper.updateStatus(bizOrder.getId(), PayBizOrderStatus.PAYING.name());
        return toPaymentVO(payment);
    }

    @Override
    @Transactional
    public boolean closeBizOrder(ClosePayBizOrderCommand command) {
        return withBizOrderLock(command.getBizOrderId(), () -> doCloseBizOrder(command));
    }

    private boolean doCloseBizOrder(ClosePayBizOrderCommand command) {
        PayBizOrder bizOrder = bizOrderMapper.selectById(command.getBizOrderId());
        Require.notNull(bizOrder, "业务支付单不存在");
        Require.isTrue(currentTenantId().equals(bizOrder.getTenantId()), "业务支付单不存在");
        Require.isFalse(PayBizOrderStatus.PAID.name().equals(bizOrder.getStatus()), "已支付业务单不能关闭");
        bizOrderMapper.updateStatus(bizOrder.getId(), PayBizOrderStatus.CLOSED.name());
        paymentOrderMapper.closeOtherProcessing(bizOrder.getId(), -1L);
        return true;
    }

    @Override
    public PayBizOrderVO queryBizOrder(QueryPayBizOrderCommand command) {
        PayBizOrder bizOrder = bizOrderMapper.selectById(command.getBizOrderId());
        if (bizOrder == null) {
            return null;
        }
        if (!currentTenantId().equals(bizOrder.getTenantId())) {
            return null;
        }
        return toBizVO(bizOrder);
    }

    @Override
    public PaymentOrderVO queryPaymentOrder(QueryPaymentOrderCommand command) {
        PayPaymentOrder paymentOrder = paymentOrderMapper.selectById(command.getPaymentOrderId());
        if (paymentOrder == null) {
            return null;
        }
        if (!currentTenantId().equals(paymentOrder.getTenantId())) {
            return null;
        }
        return toPaymentVO(paymentOrder);
    }

    @Override
    @Transactional
    public PaymentOrderVO refreshPaymentStatus(RefreshPaymentStatusCommand command) {
        PayPaymentOrder paymentOrder = paymentOrderMapper.selectById(command.getPaymentOrderId());
        Require.notNull(paymentOrder, "支付单不存在");
        Require.isTrue(currentTenantId().equals(paymentOrder.getTenantId()), "支付单不存在");
        return withBizOrderLock(paymentOrder.getBizOrderId(), () -> doRefreshPaymentStatus(paymentOrder));
    }

    private PaymentOrderVO doRefreshPaymentStatus(PayPaymentOrder paymentOrder) {
        PaymentChannelStatus status = channelRegistry.paymentProviderByChannel(paymentOrder.getChannelCode())
                .queryPayment(paymentOrder.getId(), paymentOrder.getChannelOrderNo());
        if (PaymentOrderStatus.SUCCESS == status.status()) {
            markPaymentSuccess(paymentOrder, status.channelOrderNo());
        } else {
            paymentOrderMapper.updateStatus(paymentOrder.getId(), status.status().name(), status.channelOrderNo());
        }
        return toPaymentVO(paymentOrderMapper.selectById(paymentOrder.getId()));
    }

    @Override
    @Transactional
    public boolean paymentNotify(PaymentNotifyCommand command) {
        IIdempotent idempotent = idempotentProvider.getIfAvailable();
        String idempotentKey = "payment:notify:" + command.getNotifyEventId();
        PayNotifyRecord existing = notifyRecordMapper.selectByNotifyEventId(command.getNotifyEventId());
        if (existing != null) {
            return existing.getVerified() == 1;
        }
        if (idempotent != null && idempotent.checkAndMark(idempotentKey, kvProperties.getNotifyIdempotentWindowSeconds())) {
            existing = notifyRecordMapper.selectByNotifyEventId(command.getNotifyEventId());
            Require.notNull(existing, 409, "支付回调正在处理中，请稍后查询结果");
            return existing.getVerified() == 1;
        }
        PayPaymentOrder paymentOrder = paymentOrderMapper.selectById(command.getPaymentOrderId());
        Require.notNull(paymentOrder, "支付单不存在");
        Require.isTrue(currentTenantId().equals(paymentOrder.getTenantId()), "支付单不存在");
        return withBizOrderLock(paymentOrder.getBizOrderId(), () -> doPaymentNotify(command, paymentOrder));
    }

    private boolean doPaymentNotify(PaymentNotifyCommand command, PayPaymentOrder paymentOrder) {
        PayNotifyRecord existing = notifyRecordMapper.selectByNotifyEventId(command.getNotifyEventId());
        if (existing != null) {
            return existing.getVerified() == 1;
        }
        boolean verified = channelRegistry.notifyVerifier(paymentOrder.getChannelCode()).verifyPayment(
                command.getPaymentOrderId(), command.getChannelOrderNo(), command.getNotifyEventId(), command.getSignature());
        PayNotifyRecord record = new PayNotifyRecord();
        record.setId(IdWorker.getId());
        record.setNotifyEventId(command.getNotifyEventId());
        record.setPaymentOrderId(command.getPaymentOrderId());
        record.setChannelOrderNo(command.getChannelOrderNo());
        record.setRawRequest(command.toString());
        record.setVerified(verified ? 1 : 0);
        record.setTenantId(paymentOrder.getTenantId());
        record.setCreateTime(LocalDateTime.now());
        notifyRecordMapper.insert(record);
        if (!verified) {
            return false;
        }
        markPaymentSuccess(paymentOrder, command.getChannelOrderNo());
        return true;
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

    private void markPaymentSuccess(PayPaymentOrder paymentOrder, String channelOrderNo) {
        paymentOrderMapper.updateStatus(paymentOrder.getId(), PaymentOrderStatus.SUCCESS.name(), channelOrderNo);
        bizOrderMapper.updateStatus(paymentOrder.getBizOrderId(), PayBizOrderStatus.PAID.name());
        paymentOrderMapper.closeOtherProcessing(paymentOrder.getBizOrderId(), paymentOrder.getId());
    }

    private PayBizOrderVO toBizVO(PayBizOrder order) {
        PayBizOrderVO vo = new PayBizOrderVO();
        vo.setBizOrderId(order.getId());
        vo.setMerchantOrderNo(order.getMerchantOrderNo());
        vo.setAmount(order.getAmount());
        vo.setRefundedAmount(order.getRefundedAmount());
        vo.setCurrency(order.getCurrency());
        vo.setStatus(PayBizOrderStatus.valueOf(order.getStatus()));
        return vo;
    }

    private PaymentOrderVO toPaymentVO(PayPaymentOrder order) {
        PaymentOrderVO vo = new PaymentOrderVO();
        vo.setPaymentOrderId(order.getId());
        vo.setBizOrderId(order.getBizOrderId());
        vo.setChannelCode(order.getChannelCode());
        vo.setAmount(order.getAmount());
        vo.setStatus(PaymentOrderStatus.valueOf(order.getStatus()));
        vo.setMaterialType(io.mango.payment.api.enums.PaymentMaterialType.valueOf(order.getMaterialType()));
        vo.setMaterialContent(order.getMaterialContent());
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
