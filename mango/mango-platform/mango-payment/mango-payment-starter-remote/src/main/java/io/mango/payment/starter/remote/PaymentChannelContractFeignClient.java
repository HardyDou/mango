package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentChannelContractApi;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelBillSourceCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelBillSourceVO;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.api.vo.PaymentChannelCertificateRotationRecordVO;
import io.mango.payment.api.vo.PaymentChannelContractVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentChannelContractFeignClient", path = "/payment/channel-contracts")
public interface PaymentChannelContractFeignClient extends PaymentChannelContractApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentChannelContractVO>> pageChannelContracts(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentChannelContractVO> detailChannelContract(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> createChannelContract(@RequestBody SavePaymentChannelContractCommand command);

    @Override
    @PutMapping
    R<Boolean> updateChannelContract(@RequestBody SavePaymentChannelContractCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteChannelContract(@RequestParam("id") Long id);

    @Override
    @GetMapping("/bill-sources/page")
    R<PageResult<PaymentChannelBillSourceVO>> pageBillSources(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @PostMapping("/bill-sources")
    R<PaymentChannelBillSourceVO> saveBillSource(@RequestBody SavePaymentChannelBillSourceCommand command);

    @Override
    @GetMapping("/certificates/expiring")
    R<List<PaymentChannelCertificateExpiryVO>> listExpiringCertificates(
            @RequestParam(value = "warningDays", required = false) Integer warningDays);

    @Override
    @PostMapping("/certificates/rotate")
    R<PaymentChannelCertificateRotationRecordVO> rotateCertificate(
            @RequestBody RotatePaymentChannelContractCertificateCommand command);
}
