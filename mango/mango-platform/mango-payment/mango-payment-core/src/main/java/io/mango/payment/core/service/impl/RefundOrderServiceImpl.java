package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.payment.api.query.RefundOrderPageQuery;
import io.mango.payment.api.vo.RefundOrderRecordVO;
import io.mango.payment.core.entity.PayRefundOrder;
import io.mango.payment.core.mapper.PayRefundOrderMapper;
import io.mango.payment.core.service.IRefundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款单服务实现。
 */
@Service
@RequiredArgsConstructor
public class RefundOrderServiceImpl implements IRefundOrderService {

    private final PayRefundOrderMapper refundOrderMapper;

    @Override
    public R<PageResult<RefundOrderRecordVO>> pageRefunds(RefundOrderPageQuery query) {
        RefundOrderPageQuery resolved = query == null ? new RefundOrderPageQuery() : query;
        IPage<PayRefundOrder> page = refundOrderMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), refundWrapper(resolved));
        List<RefundOrderRecordVO> records = page.getRecords().stream().map(this::toRefundVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<RefundOrderRecordVO> detailRefund(Long id) {
        PayRefundOrder entity = refundOrderMapper.selectById(id);
        Require.notNull(entity, "退款单不存在");
        Require.isTrue(currentTenantId().equals(entity.getTenantId()), "退款单不存在");
        return R.ok(toRefundVO(entity));
    }

    private LambdaQueryWrapper<PayRefundOrder> refundWrapper(RefundOrderPageQuery query) {
        return new LambdaQueryWrapper<PayRefundOrder>()
                .eq(query.getBizOrderId() != null, PayRefundOrder::getBizOrderId, query.getBizOrderId())
                .like(StringUtils.hasText(query.getMerchantRefundNo()), PayRefundOrder::getMerchantRefundNo, query.getMerchantRefundNo())
                .eq(StringUtils.hasText(query.getStatus()), PayRefundOrder::getStatus, query.getStatus())
                .eq(PayRefundOrder::getTenantId, currentTenantId())
                .orderByDesc(PayRefundOrder::getCreateTime);
    }

    private RefundOrderRecordVO toRefundVO(PayRefundOrder entity) {
        RefundOrderRecordVO vo = new RefundOrderRecordVO();
        vo.setId(entity.getId());
        vo.setBizOrderId(entity.getBizOrderId());
        vo.setPaymentOrderId(entity.getPaymentOrderId());
        vo.setMerchantRefundNo(entity.getMerchantRefundNo());
        vo.setChannelRefundNo(entity.getChannelRefundNo());
        vo.setIdempotencyKey(entity.getIdempotencyKey());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setStatus(entity.getStatus());
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
