package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PayBizOrderPageQuery;
import io.mango.payment.api.vo.PayBizOrderRecordVO;

/**
 * 业务支付单服务。
 */
public interface IPayBizOrderService {

    R<PageResult<PayBizOrderRecordVO>> pageBizOrders(PayBizOrderPageQuery query);

    R<PayBizOrderRecordVO> detailBizOrder(Long id);
}
