package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PayBizOrderPageQuery;
import io.mango.payment.api.vo.PayBizOrderRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * 业务支付单 API 契约。
 */
@Validated
public interface PayBizOrderApi {

    R<PageResult<PayBizOrderRecordVO>> pageBizOrders(@Valid PayBizOrderPageQuery query);

    R<PayBizOrderRecordVO> detailBizOrder(@NotNull(message = "业务支付单 ID 不能为空") Long id);
}
