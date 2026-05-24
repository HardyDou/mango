package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.RefundOrderPageQuery;
import io.mango.payment.api.vo.RefundOrderRecordVO;

/**
 * 退款单服务。
 */
public interface IRefundOrderService {

    R<PageResult<RefundOrderRecordVO>> pageRefunds(RefundOrderPageQuery query);

    R<RefundOrderRecordVO> detailRefund(Long id);
}
