package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.api.command.SavePaymentApplicationCommand;
import io.mango.payment.api.command.UpdatePaymentApplicationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentApplicationSaveResultVO;
import io.mango.payment.api.vo.PaymentApplicationVO;

public interface IPaymentApplicationService {

    PageResult<PaymentApplicationVO> pageApplications(PaymentConfigPageQuery query);

    PaymentApplicationVO detailApplication(Long id);

    PaymentApplicationSaveResultVO createApplication(CreatePaymentApplicationCommand command);

    PaymentApplicationSaveResultVO updateApplication(UpdatePaymentApplicationCommand command);

    Boolean deleteApplication(Long id);
}
