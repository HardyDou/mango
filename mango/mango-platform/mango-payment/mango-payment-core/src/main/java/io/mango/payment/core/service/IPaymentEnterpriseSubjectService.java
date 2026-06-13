package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;

public interface IPaymentEnterpriseSubjectService {

    R<PageResult<PaymentEnterpriseSubjectVO>> pageEnterpriseSubjects(PaymentConfigPageQuery query);

    R<PaymentEnterpriseSubjectVO> detailEnterpriseSubject(Long id);

    R<Long> createEnterpriseSubject(SavePaymentEnterpriseSubjectCommand command);

    R<Boolean> updateEnterpriseSubject(SavePaymentEnterpriseSubjectCommand command);

    R<Boolean> deleteEnterpriseSubject(Long id);
}
