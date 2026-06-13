package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.ConfirmPaymentSettlementSummaryCommand;
import io.mango.payment.api.command.GeneratePaymentSettlementSummaryCommand;
import io.mango.payment.api.command.VoidPaymentSettlementSummaryCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentSettlementSummaryStatusVO;
import io.mango.payment.api.vo.PaymentSettlementSummaryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentSettlementSummaryApi {

    R<PageResult<PaymentSettlementSummaryVO>> pageSettlementSummaries(@Valid PaymentConfigPageQuery query);

    R<PaymentSettlementSummaryVO> detailSettlementSummary(@NotNull(message = "结算汇总 ID 不能为空") Long id);

    R<List<PaymentSettlementSummaryStatusVO>> listSettlementSummaryStatuses();

    R<PaymentSettlementSummaryVO> generateSettlementSummary(@Valid GeneratePaymentSettlementSummaryCommand command);

    R<PaymentSettlementSummaryVO> confirmSettlementSummary(@Valid ConfirmPaymentSettlementSummaryCommand command);

    R<PaymentSettlementSummaryVO> voidSettlementSummary(@Valid VoidPaymentSettlementSummaryCommand command);
}
