package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.mango.payment.core.model.PaymentProjectionConverter.toApi;
import static io.mango.payment.core.model.PaymentProjectionConverter.toApiList;

@Service
@RequiredArgsConstructor
public class PaymentTransactionFlowService implements IPaymentTransactionFlowService {

    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentOrderViewSupport viewSupport;

    public PageResult<PaymentTransactionFlowVO> pageTransactionFlows(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String tenantId = PaymentContextSupport.currentTenantId();
        long total = transactionFlowMapper.countTransactionFlows(tenantId, keyword);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentTransactionFlowVO> rows = toApiList(transactionFlowMapper.selectTransactionFlowPage(
                tenantId, keyword, size, (page - 1) * size), PaymentTransactionFlowVO.class);
        rows.forEach(this::fillTransactionFlowSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentTransactionFlowVO detailTransactionFlow(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID, "交易流水 ID 不能为空");
        PaymentTransactionFlowVO vo = toApi(transactionFlowMapper.selectTransactionFlowDetail(
                PaymentContextSupport.currentTenantId(), id), PaymentTransactionFlowVO.class);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillTransactionFlowSummary(vo);
        return vo;
    }

    private void fillTransactionFlowSummary(PaymentTransactionFlowVO vo) {
        vo.setFlowTypeName(viewSupport.transactionFlowTypeName(vo.getFlowType()));
    }
}
