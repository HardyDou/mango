package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.api.vo.PaymentChannelCertificateRotationRecordVO;
import io.mango.payment.api.vo.PaymentChannelContractVO;

import java.util.List;

public interface IPaymentChannelContractService {

    R<PageResult<PaymentChannelContractVO>> pageChannelContracts(PaymentConfigPageQuery query);

    R<PaymentChannelContractVO> detailChannelContract(Long id);

    R<Long> createChannelContract(SavePaymentChannelContractCommand command);

    R<Boolean> updateChannelContract(SavePaymentChannelContractCommand command);

    R<Boolean> deleteChannelContract(Long id);

    R<List<PaymentChannelCertificateExpiryVO>> listExpiringCertificates(Integer warningDays);

    R<PaymentChannelCertificateRotationRecordVO> rotateCertificate(RotatePaymentChannelContractCertificateCommand command);
}
