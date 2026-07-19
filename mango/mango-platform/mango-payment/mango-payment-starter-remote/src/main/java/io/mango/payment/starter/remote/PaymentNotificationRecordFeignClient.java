package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentNotificationRecordApi;
import io.mango.payment.api.command.RetryPaymentNotificationRecordCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentNotificationRecordVO;
import io.mango.payment.api.vo.PaymentNotificationStatusVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentNotificationRecordFeignClient", path = "/payment/notification-records")
public interface PaymentNotificationRecordFeignClient extends PaymentNotificationRecordApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentNotificationRecordVO>> pageNotificationRecords(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentNotificationRecordVO> detailNotificationRecord(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentNotificationStatusVO>> listNotificationStatuses();

    @Override
    @PostMapping("/retry")
    R<PaymentNotificationRecordVO> retryNotificationRecord(@RequestBody RetryPaymentNotificationRecordCommand command);

    @Override
    @PostMapping("/deliver-due")
    R<Integer> deliverDueNotificationRecords(@RequestParam(value = "limit", defaultValue = "20") long limit);
}
