package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOfflineRefundStatusVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentOfflineRefundApi {

    R<PageResult<PaymentOfflineRefundVO>> pageOfflineRefunds(@Valid PaymentConfigPageQuery query);

    R<PaymentOfflineRefundVO> detailOfflineRefund(@NotNull(message = "线下退款 ID 不能为空") Long id);

    R<List<PaymentOfflineRefundStatusVO>> listOfflineRefundStatuses();
}
