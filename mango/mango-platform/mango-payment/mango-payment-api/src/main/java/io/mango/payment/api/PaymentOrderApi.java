package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PaymentOrderPageQuery;
import io.mango.payment.api.vo.PaymentOrderRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * 支付单 API 契约。
 */
@Validated
public interface PaymentOrderApi {

    R<PageResult<PaymentOrderRecordVO>> pagePayments(@Valid PaymentOrderPageQuery query);

    R<PaymentOrderRecordVO> detailPayment(@NotNull(message = "支付单 ID 不能为空") Long id);
}
