package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.RefundOrderPageQuery;
import io.mango.payment.api.vo.RefundOrderRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * 退款单 API 契约。
 */
@Validated
public interface RefundOrderApi {

    R<PageResult<RefundOrderRecordVO>> pageRefunds(@Valid RefundOrderPageQuery query);

    R<RefundOrderRecordVO> detailRefund(@NotNull(message = "退款单 ID 不能为空") Long id);
}
