package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.payment.api.query.PaymentOrderPageQuery;
import io.mango.payment.api.vo.PaymentOrderRecordVO;
import io.mango.payment.core.entity.PayPaymentOrder;
import io.mango.payment.core.mapper.PayPaymentOrderMapper;
import io.mango.payment.core.service.IPaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 支付单服务实现。
 */
@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl implements IPaymentOrderService {

    private final PayPaymentOrderMapper paymentOrderMapper;

    @Override
    public R<PageResult<PaymentOrderRecordVO>> pagePayments(PaymentOrderPageQuery query) {
        PaymentOrderPageQuery resolved = query == null ? new PaymentOrderPageQuery() : query;
        IPage<PayPaymentOrder> page = paymentOrderMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), paymentWrapper(resolved));
        List<PaymentOrderRecordVO> records = page.getRecords().stream().map(this::toPaymentVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<PaymentOrderRecordVO> detailPayment(Long id) {
        PayPaymentOrder entity = paymentOrderMapper.selectById(id);
        Require.notNull(entity, "支付单不存在");
        Require.isTrue(currentTenantId().equals(entity.getTenantId()), "支付单不存在");
        return R.ok(toPaymentVO(entity));
    }

    private LambdaQueryWrapper<PayPaymentOrder> paymentWrapper(PaymentOrderPageQuery query) {
        return new LambdaQueryWrapper<PayPaymentOrder>()
                .eq(query.getBizOrderId() != null, PayPaymentOrder::getBizOrderId, query.getBizOrderId())
                .eq(StringUtils.hasText(query.getChannelCode()), PayPaymentOrder::getChannelCode, query.getChannelCode())
                .eq(StringUtils.hasText(query.getStatus()), PayPaymentOrder::getStatus, query.getStatus())
                .eq(PayPaymentOrder::getTenantId, currentTenantId())
                .orderByDesc(PayPaymentOrder::getCreateTime);
    }

    private PaymentOrderRecordVO toPaymentVO(PayPaymentOrder entity) {
        PaymentOrderRecordVO vo = new PaymentOrderRecordVO();
        vo.setId(entity.getId());
        vo.setBizOrderId(entity.getBizOrderId());
        vo.setChannelCode(entity.getChannelCode());
        vo.setChannelOrderNo(entity.getChannelOrderNo());
        vo.setPayMethod(entity.getPayMethod());
        vo.setIdempotencyKey(entity.getIdempotencyKey());
        vo.setAmount(entity.getAmount());
        vo.setStatus(entity.getStatus());
        vo.setMaterialType(entity.getMaterialType());
        vo.setMaterialContent(entity.getMaterialContent());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
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
