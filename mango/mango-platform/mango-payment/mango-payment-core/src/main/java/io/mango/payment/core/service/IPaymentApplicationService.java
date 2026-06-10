package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.api.command.SavePaymentApplicationCommand;
import io.mango.payment.api.command.UpdatePaymentApplicationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentApplicationSaveResultVO;
import io.mango.payment.api.vo.PaymentApplicationVO;

public interface IPaymentApplicationService {

    R<PageResult<PaymentApplicationVO>> pageApplications(PaymentConfigPageQuery query);

    R<PaymentApplicationVO> detailApplication(Long id);

    R<PaymentApplicationSaveResultVO> createApplication(CreatePaymentApplicationCommand command);

    R<PaymentApplicationSaveResultVO> updateApplication(UpdatePaymentApplicationCommand command);

    R<Boolean> deleteApplication(Long id);
}
