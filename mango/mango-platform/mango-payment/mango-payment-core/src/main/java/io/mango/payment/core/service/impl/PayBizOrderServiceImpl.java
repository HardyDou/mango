package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.payment.api.query.PayBizOrderPageQuery;
import io.mango.payment.api.vo.PayBizOrderRecordVO;
import io.mango.payment.core.entity.PayBizOrder;
import io.mango.payment.core.mapper.PayBizOrderMapper;
import io.mango.payment.core.service.IPayBizOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务支付单服务实现。
 */
@Service
@RequiredArgsConstructor
public class PayBizOrderServiceImpl implements IPayBizOrderService {

    private final PayBizOrderMapper bizOrderMapper;

    @Override
    public R<PageResult<PayBizOrderRecordVO>> pageBizOrders(PayBizOrderPageQuery query) {
        PayBizOrderPageQuery resolved = query == null ? new PayBizOrderPageQuery() : query;
        IPage<PayBizOrder> page = bizOrderMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), bizOrderWrapper(resolved));
        List<PayBizOrderRecordVO> records = page.getRecords().stream().map(this::toBizOrderVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<PayBizOrderRecordVO> detailBizOrder(Long id) {
        PayBizOrder entity = bizOrderMapper.selectById(id);
        Require.notNull(entity, "业务支付单不存在");
        Require.isTrue(currentTenantId().equals(entity.getTenantId()), "业务支付单不存在");
        return R.ok(toBizOrderVO(entity));
    }

    private LambdaQueryWrapper<PayBizOrder> bizOrderWrapper(PayBizOrderPageQuery query) {
        return new LambdaQueryWrapper<PayBizOrder>()
                .eq(StringUtils.hasText(query.getAppCode()), PayBizOrder::getAppCode, query.getAppCode())
                .like(StringUtils.hasText(query.getMerchantOrderNo()), PayBizOrder::getMerchantOrderNo, query.getMerchantOrderNo())
                .eq(StringUtils.hasText(query.getStatus()), PayBizOrder::getStatus, query.getStatus())
                .eq(PayBizOrder::getTenantId, currentTenantId())
                .orderByDesc(PayBizOrder::getCreateTime);
    }

    private PayBizOrderRecordVO toBizOrderVO(PayBizOrder entity) {
        PayBizOrderRecordVO vo = new PayBizOrderRecordVO();
        vo.setId(entity.getId());
        vo.setAppCode(entity.getAppCode());
        vo.setMerchantOrderNo(entity.getMerchantOrderNo());
        vo.setSubject(entity.getSubject());
        vo.setAmount(entity.getAmount());
        vo.setRefundedAmount(entity.getRefundedAmount());
        vo.setCurrency(entity.getCurrency());
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
