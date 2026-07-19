package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentRefundApprovalApi;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentRefundApprovalStatusVO;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentRefundApprovalFeignClient", path = "/payment/refund-approvals")
public interface PaymentRefundApprovalFeignClient extends PaymentRefundApprovalApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentRefundApprovalVO>> pageRefundApprovals(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentRefundApprovalVO> detailRefundApproval(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentRefundApprovalStatusVO>> listRefundApprovalStatuses();

    @Override
    @PostMapping
    R<PaymentRefundApprovalVO> createRefundApproval(@RequestBody CreatePaymentRefundApprovalCommand command);
}
