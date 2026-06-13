package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentTransactionFlowApi {

    R<PageResult<PaymentTransactionFlowVO>> pageTransactionFlows(@Valid PaymentConfigPageQuery query);

    R<PaymentTransactionFlowVO> detailTransactionFlow(@NotNull(message = "交易流水 ID 不能为空") Long id);
}
