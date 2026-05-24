package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.RefundOrderApi;
import io.mango.payment.api.query.RefundOrderPageQuery;
import io.mango.payment.api.vo.RefundOrderRecordVO;
import io.mango.payment.core.service.IRefundOrderService;
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
 * 退款单接口。
 */
@Validated
@RestController
@RequestMapping("/payment/refund-orders")
@RequiredArgsConstructor
@Tag(name = "退款单", description = "退款单数据接口")
public class RefundOrderController implements RefundOrderApi {

    private final IRefundOrderService refundOrderService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询退款单", description = "分页查询退款单数据")
    public R<PageResult<RefundOrderRecordVO>> pageRefunds(@ParameterObject RefundOrderPageQuery query) {
        return refundOrderService.pageRefunds(query);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询退款单详情", description = "按退款单 ID 查询详情")
    public R<RefundOrderRecordVO> detailRefund(
            @Parameter(description = "退款单 ID", required = true)
            @NotNull(message = "退款单 ID 不能为空")
            @RequestParam Long id) {
        return refundOrderService.detailRefund(id);
    }
}
