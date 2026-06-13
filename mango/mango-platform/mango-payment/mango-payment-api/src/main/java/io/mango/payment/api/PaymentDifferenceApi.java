package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.HandlePaymentDifferenceCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentDifferenceActionVO;
import io.mango.payment.api.vo.PaymentDifferenceStatusVO;
import io.mango.payment.api.vo.PaymentDifferenceVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentDifferenceApi {

    R<PageResult<PaymentDifferenceVO>> pageDifferences(@Valid PaymentConfigPageQuery query);

    R<PaymentDifferenceVO> detailDifference(@NotNull(message = "对账差异 ID 不能为空") Long id);

    R<List<PaymentDifferenceStatusVO>> listDifferenceStatuses();

    R<List<PaymentDifferenceActionVO>> listDifferenceActions();

    R<PaymentDifferenceVO> handleDifference(@Valid HandlePaymentDifferenceCommand command);
}
