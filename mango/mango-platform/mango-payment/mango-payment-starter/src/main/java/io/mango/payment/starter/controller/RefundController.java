package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.payment.api.RefundApi;
import io.mango.payment.api.command.QueryRefundOrderCommand;
import io.mango.payment.api.command.RefreshRefundStatusCommand;
import io.mango.payment.api.command.RefundCommand;
import io.mango.payment.api.vo.RefundOrderVO;
import io.mango.payment.core.service.IRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment/refunds")
@RequiredArgsConstructor
@Tag(name = "支付退款", description = "退款发起、退款查询和退款状态刷新接口")
public class RefundController implements RefundApi {

    private final IRefundService refundService;

    @Override
    @PostMapping
    @Operation(summary = "发起退款", description = "对已支付业务单发起部分或全额退款")
    public R<RefundOrderVO> refund(@Valid @RequestBody RefundCommand command) {
        return R.ok(refundService.refund(command));
    }

    @Override
    @PostMapping("/query")
    @Operation(summary = "查询退款单", description = "查询退款单状态和渠道退款号")
    public R<RefundOrderVO> queryRefundOrder(@Valid @RequestBody QueryRefundOrderCommand command) {
        return R.ok(refundService.queryRefundOrder(command));
    }

    @Override
    @PostMapping("/refresh")
    @Operation(summary = "刷新退款状态", description = "主动查询渠道并刷新退款单状态")
    public R<RefundOrderVO> refreshRefundStatus(@Valid @RequestBody RefreshRefundStatusCommand command) {
        return R.ok(refundService.refreshRefundStatus(command));
    }
}
