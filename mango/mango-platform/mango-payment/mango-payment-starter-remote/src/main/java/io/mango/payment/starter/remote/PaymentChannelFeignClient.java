package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentChannelApi;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentChannelVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentChannelFeignClient", path = "/payment/channels")
public interface PaymentChannelFeignClient extends PaymentChannelApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentChannelVO>> pageChannels(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentChannelVO> detailChannel(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> createChannel(@RequestBody SavePaymentChannelCommand command);

    @Override
    @PutMapping
    R<Boolean> updateChannel(@RequestBody SavePaymentChannelCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteChannel(@RequestParam("id") Long id);

    @Override
    @GetMapping("/capabilities/page")
    R<PageResult<PaymentChannelCapabilityVO>> pageChannelCapabilities(@SpringQueryMap PaymentConfigPageQuery query);
}
