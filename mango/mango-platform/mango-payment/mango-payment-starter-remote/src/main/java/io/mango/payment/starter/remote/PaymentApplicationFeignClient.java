package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentApplicationApi;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.api.command.UpdatePaymentApplicationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentApplicationSaveResultVO;
import io.mango.payment.api.vo.PaymentApplicationVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentApplicationFeignClient", path = "/payment/applications")
public interface PaymentApplicationFeignClient extends PaymentApplicationApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentApplicationVO>> pageApplications(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentApplicationVO> detailApplication(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<PaymentApplicationSaveResultVO> createApplication(@RequestBody CreatePaymentApplicationCommand command);

    @Override
    @PutMapping
    R<PaymentApplicationSaveResultVO> updateApplication(@RequestBody UpdatePaymentApplicationCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteApplication(@RequestParam("id") Long id);
}
