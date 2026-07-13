package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOrderStatusVO;
import io.mango.payment.api.vo.PaymentOrderSyncStatusVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.mango.payment.core.model.PaymentProjectionConverter.toApi;
import static io.mango.payment.core.model.PaymentProjectionConverter.toApiList;

@Service
@RequiredArgsConstructor
public class PaymentOrderService implements IPaymentOrderService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentChannelSynchronizer channelSyncService;
    private final PaymentOperationAuditService auditService;
    private final PaymentOrderViewSupport viewSupport;

    public PageResult<PaymentOrderVO> pagePaymentOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        String tenantId = PaymentContextSupport.currentTenantId();
        long total = paymentOrderMapper.countPaymentOrders(tenantId, keyword, statusCode, resolved.getChannelId());
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentOrderVO> rows = toApiList(paymentOrderMapper.selectPaymentOrderPage(
                tenantId, keyword, statusCode, resolved.getChannelId(), size, (page - 1) * size), PaymentOrderVO.class);
        rows.forEach(this::fillPaymentOrderSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentOrderVO detailPaymentOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID, "支付订单 ID 不能为空");
        PaymentOrderVO vo = toApi(paymentOrderMapper.selectPaymentOrderDetail(
                PaymentContextSupport.currentTenantId(), id), PaymentOrderVO.class);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillPaymentOrderSummary(vo);
        vo.setStatusFlows(viewSupport.listPaymentStatusFlows(id));
        return vo;
    }

    public PaymentOrderSyncStatusVO syncPaymentOrderStatus(String payOrderNo) {
        String resolvedPayOrderNo = PaymentContextSupport.trimToNull(payOrderNo);
        Require.notBlank(resolvedPayOrderNo, PaymentCode.PAYMENT_ORDER_NOT_FOUND, "支付订单号不能为空");
        PaymentChannelSynchronizer.PaymentSyncResult queryResult = channelSyncService.syncPaymentStatus(resolvedPayOrderNo);
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_SYNC_PAYMENT_ORDER_STATUS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                resolvedPayOrderNo,
                PaymentOperationAuditService.RESULT_SUCCESS));
        PaymentOrderSyncStatusVO vo = new PaymentOrderSyncStatusVO();
        vo.setPayOrderNo(queryResult.payOrderNo());
        vo.setStatus(queryResult.status());
        vo.setStatusName(PaymentOrderStatusEnum.labelOf(queryResult.status()));
        vo.setFlowNo(queryResult.flowNo());
        vo.setChanged(queryResult.changed());
        vo.setQueryCount(queryResult.queryCount());
        vo.setLastQueryResult(queryResult.lastQueryResult());
        return vo;
    }

    public List<PaymentOrderStatusVO> listPaymentOrderStatuses() {
        return PaymentOrderStatusEnum.options().stream().map(status -> {
            PaymentOrderStatusVO vo = new PaymentOrderStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

    private void fillPaymentOrderSummary(PaymentOrderVO vo) {
        if (viewSupport.isExpiredOpenPaymentOrder(vo.getStatus(), vo.getExpireTime())) {
            vo.setStatusName("已过期");
        } else {
            vo.setStatusName(PaymentOrderStatusEnum.labelOf(vo.getStatus()));
        }
        vo.setFlowNo(paymentOrderMapper.selectLatestFlowNo(PaymentContextSupport.currentTenantId(), vo.getId()));
    }
}
