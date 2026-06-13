package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentEnterpriseSubjectApi {

    R<PageResult<PaymentEnterpriseSubjectVO>> pageEnterpriseSubjects(@Valid PaymentConfigPageQuery query);

    R<PaymentEnterpriseSubjectVO> detailEnterpriseSubject(@NotNull(message = "主体 ID 不能为空") Long id);

    R<Long> createEnterpriseSubject(@Valid SavePaymentEnterpriseSubjectCommand command);

    R<Boolean> updateEnterpriseSubject(@Valid SavePaymentEnterpriseSubjectCommand command);

    R<Boolean> deleteEnterpriseSubject(@NotNull(message = "主体 ID 不能为空") Long id);
}
