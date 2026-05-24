package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOrderApi;
import io.mango.payment.api.query.PaymentOrderPageQuery;
import io.mango.payment.api.vo.PaymentOrderRecordVO;
import io.mango.payment.core.service.IPaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付单接口。
 */
@Validated
@RestController
@RequestMapping("/payment/orders")
@RequiredArgsConstructor
@Tag(name = "支付单", description = "支付单数据接口")
public class PaymentOrderController implements PaymentOrderApi {

    private final IPaymentOrderService paymentOrderService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询支付单", description = "分页查询支付单数据")
    public R<PageResult<PaymentOrderRecordVO>> pagePayments(@ParameterObject PaymentOrderPageQuery query) {
        return paymentOrderService.pagePayments(query);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询支付单详情", description = "按支付单 ID 查询详情")
    public R<PaymentOrderRecordVO> detailPayment(
            @Parameter(description = "支付单 ID", required = true)
            @NotNull(message = "支付单 ID 不能为空")
            @RequestParam Long id) {
        return paymentOrderService.detailPayment(id);
    }
}
