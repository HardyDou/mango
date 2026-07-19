package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCashierConfigApi;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentCashierConfigVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentCashierConfigFeignClient", path = "/payment/cashier-configs")
public interface PaymentCashierConfigFeignClient extends PaymentCashierConfigApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentCashierConfigVO>> pageCashierConfigs(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentCashierConfigVO> detailCashierConfig(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> createCashierConfig(@RequestBody SavePaymentCashierConfigCommand command);

    @Override
    @PutMapping
    R<Boolean> updateCashierConfig(@RequestBody SavePaymentCashierConfigCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteCashierConfig(@RequestParam("id") Long id);
}
