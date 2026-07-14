package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;

public interface IPaymentEnterpriseSubjectService {

    PageResult<PaymentEnterpriseSubjectVO> pageEnterpriseSubjects(PaymentConfigPageQuery query);

    PaymentEnterpriseSubjectVO detailEnterpriseSubject(Long id);

    Long createEnterpriseSubject(SavePaymentEnterpriseSubjectCommand command);

    Boolean updateEnterpriseSubject(SavePaymentEnterpriseSubjectCommand command);

    Boolean deleteEnterpriseSubject(Long id);
}
