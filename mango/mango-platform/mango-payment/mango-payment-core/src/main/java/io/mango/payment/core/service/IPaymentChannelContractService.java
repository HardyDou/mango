package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.api.vo.PaymentChannelCertificateRotationRecordVO;
import io.mango.payment.api.vo.PaymentChannelContractVO;

import java.util.List;

public interface IPaymentChannelContractService {

    PageResult<PaymentChannelContractVO> pageChannelContracts(PaymentConfigPageQuery query);

    PaymentChannelContractVO detailChannelContract(Long id);

    Long createChannelContract(SavePaymentChannelContractCommand command);

    Boolean updateChannelContract(SavePaymentChannelContractCommand command);

    Boolean deleteChannelContract(Long id);

    List<PaymentChannelCertificateExpiryVO> listExpiringCertificates(Integer warningDays);

    PaymentChannelCertificateRotationRecordVO rotateCertificate(RotatePaymentChannelContractCertificateCommand command);
}
