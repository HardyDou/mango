package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelBillSourceCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.api.vo.PaymentChannelCertificateRotationRecordVO;
import io.mango.payment.api.vo.PaymentChannelBillSourceVO;
import io.mango.payment.api.vo.PaymentChannelContractVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentChannelContractApi {

    R<PageResult<PaymentChannelContractVO>> pageChannelContracts(@Valid PaymentConfigPageQuery query);

    R<PaymentChannelContractVO> detailChannelContract(@NotNull(message = "签约配置 ID 不能为空") Long id);

    R<Long> createChannelContract(@Valid SavePaymentChannelContractCommand command);

    R<Boolean> updateChannelContract(@Valid SavePaymentChannelContractCommand command);

    R<Boolean> deleteChannelContract(@NotNull(message = "签约配置 ID 不能为空") Long id);

    R<PageResult<PaymentChannelBillSourceVO>> pageBillSources(@Valid PaymentConfigPageQuery query);

    R<PaymentChannelBillSourceVO> saveBillSource(@Valid SavePaymentChannelBillSourceCommand command);

    R<List<PaymentChannelCertificateExpiryVO>> listExpiringCertificates(
            @Min(value = 1, message = "预警天数不能小于 1")
            @Max(value = 365, message = "预警天数不能大于 365") Integer warningDays);

    R<PaymentChannelCertificateRotationRecordVO> rotateCertificate(
            @Valid RotatePaymentChannelContractCertificateCommand command);
}
