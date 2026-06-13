package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.api.command.UpdatePaymentApplicationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentApplicationSaveResultVO;
import io.mango.payment.api.vo.PaymentApplicationVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentApplicationApi {

    R<PageResult<PaymentApplicationVO>> pageApplications(@Valid PaymentConfigPageQuery query);

    R<PaymentApplicationVO> detailApplication(@NotNull(message = "应用 ID 不能为空") Long id);

    R<PaymentApplicationSaveResultVO> createApplication(@Valid CreatePaymentApplicationCommand command);

    R<PaymentApplicationSaveResultVO> updateApplication(@Valid UpdatePaymentApplicationCommand command);

    R<Boolean> deleteApplication(@NotNull(message = "应用 ID 不能为空") Long id);
}
