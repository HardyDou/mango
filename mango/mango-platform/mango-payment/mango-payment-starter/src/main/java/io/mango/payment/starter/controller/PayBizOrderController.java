package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PayBizOrderApi;
import io.mango.payment.api.query.PayBizOrderPageQuery;
import io.mango.payment.api.vo.PayBizOrderRecordVO;
import io.mango.payment.core.service.IPayBizOrderService;
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
 * 业务支付单接口。
 */
@Validated
@RestController
@RequestMapping("/payment/biz-orders")
@RequiredArgsConstructor
@Tag(name = "业务支付单", description = "业务支付单数据接口")
public class PayBizOrderController implements PayBizOrderApi {

    private final IPayBizOrderService payBizOrderService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询业务支付单", description = "分页查询业务支付单数据")
    public R<PageResult<PayBizOrderRecordVO>> pageBizOrders(@ParameterObject PayBizOrderPageQuery query) {
        return payBizOrderService.pageBizOrders(query);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询业务支付单详情", description = "按业务支付单 ID 查询详情")
    public R<PayBizOrderRecordVO> detailBizOrder(
            @Parameter(description = "业务支付单 ID", required = true)
            @NotNull(message = "业务支付单 ID 不能为空")
            @RequestParam Long id) {
        return payBizOrderService.detailBizOrder(id);
    }
}
