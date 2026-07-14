package io.mango.payment.core.service.impl;

import io.mango.payment.core.service.IPaymentRefundOrderService;
import io.mango.payment.core.service.PaymentChannelSynchronizer;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOrderViewSupport;

import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.command.QueryPaymentRefundOrderCommand;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentRefundOrderStatusVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.mango.payment.core.model.PaymentProjectionConverter.toApi;
import static io.mango.payment.core.model.PaymentProjectionConverter.toApiList;

@Service
@RequiredArgsConstructor
public class PaymentRefundOrderService implements IPaymentRefundOrderService {

    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentChannelSynchronizer channelSyncService;
    private final PaymentOrderViewSupport viewSupport;

    public PageResult<PaymentRefundOrderVO> pageRefundOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        String tenantId = PaymentContextSupport.currentTenantId();
        long total = refundOrderMapper.countRefundOrders(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentRefundOrderVO> rows = toApiList(refundOrderMapper.selectRefundOrderPage(
                tenantId, keyword, statusCode, size, (page - 1) * size), PaymentRefundOrderVO.class);
        rows.forEach(this::fillRefundOrderSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentRefundOrderVO detailRefundOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID, "退款订单 ID 不能为空");
        PaymentRefundOrderVO vo = toApi(refundOrderMapper.selectRefundOrderDetail(
                PaymentContextSupport.currentTenantId(), id), PaymentRefundOrderVO.class);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillRefundOrderSummary(vo);
        vo.setStatusFlows(viewSupport.listRefundStatusFlows(id));
        return vo;
    }

    public List<PaymentRefundOrderStatusVO> listRefundOrderStatuses() {
        return PaymentRefundOrderStatusEnum.options().stream().map(status -> {
            PaymentRefundOrderStatusVO vo = new PaymentRefundOrderStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

    public PaymentRefundOrderVO queryRefundOrder(QueryPaymentRefundOrderCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "主动查询退款订单命令不能为空");
        Require.notNull(command.getId(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID, "退款订单 ID 不能为空");
        PaymentRefundOrderVO refundOrder = detailRefundOrder(command.getId());
        channelSyncService.syncRefundStatus(refundOrder.getRefundOrderNo());
        return detailRefundOrder(command.getId());
    }

    private void fillRefundOrderSummary(PaymentRefundOrderVO vo) {
        vo.setStatusName(viewSupport.refundStatusName(vo.getStatus()));
        vo.setFlowNo(refundOrderMapper.selectLatestFlowNo(PaymentContextSupport.currentTenantId(), vo.getId()));
    }
}
