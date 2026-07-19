package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentMethodApi;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.api.vo.PaymentMethodVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentMethodFeignClient", path = "/payment/methods")
public interface PaymentMethodFeignClient extends PaymentMethodApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentMethodVO>> pageMethods(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/categories")
    R<java.util.List<PaymentMethodCategoryVO>> listMethodCategories();

    @Override
    @GetMapping("/detail")
    R<PaymentMethodVO> detailMethod(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> createMethod(@RequestBody SavePaymentMethodCommand command);

    @Override
    @PutMapping
    R<Boolean> updateMethod(@RequestBody SavePaymentMethodCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteMethod(@RequestParam("id") Long id);
}
