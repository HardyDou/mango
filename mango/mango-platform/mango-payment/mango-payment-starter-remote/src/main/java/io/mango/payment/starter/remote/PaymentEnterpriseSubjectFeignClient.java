package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentEnterpriseSubjectApi;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentEnterpriseSubjectFeignClient", path = "/payment/enterprise-subjects")
public interface PaymentEnterpriseSubjectFeignClient extends PaymentEnterpriseSubjectApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentEnterpriseSubjectVO>> pageEnterpriseSubjects(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentEnterpriseSubjectVO> detailEnterpriseSubject(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> createEnterpriseSubject(@RequestBody SavePaymentEnterpriseSubjectCommand command);

    @Override
    @PutMapping
    R<Boolean> updateEnterpriseSubject(@RequestBody SavePaymentEnterpriseSubjectCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteEnterpriseSubject(@RequestParam("id") Long id);
}
