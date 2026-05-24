package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PaymentOrderPageQuery;
import io.mango.payment.api.vo.PaymentOrderRecordVO;

/**
 * 支付单服务。
 */
public interface IPaymentOrderService {

    R<PageResult<PaymentOrderRecordVO>> pagePayments(PaymentOrderPageQuery query);

    R<PaymentOrderRecordVO> detailPayment(Long id);
}
