package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOfflineRefundApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOfflineRefundStatusVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;
import io.mango.payment.core.service.PaymentOfflineChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/payment/offline-refunds")
@RequiredArgsConstructor
@Tag(name = "线下退款", description = "线下收款通道退款订单接口")
public class PaymentOfflineRefundController implements PaymentOfflineRefundApi {

    private final PaymentOfflineChannelService offlineChannelService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-refund:list")
    @Operation(summary = "分页查询线下退款订单", description = "查询线下收款通道独立退款订单、退款账户、退款凭证和退款状态")
    public R<PageResult<PaymentOfflineRefundVO>> pageOfflineRefunds(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(offlineChannelService.pageOfflineRefunds(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-refund:query")
    @Operation(summary = "查询线下退款详情", description = "按线下退款 ID 查询退款金额、账户、凭证和关联线下收款单")
    public R<PaymentOfflineRefundVO> detailOfflineRefund(@Parameter(description = "线下退款 ID", required = true) @RequestParam Long id) {
        return R.ok(offlineChannelService.detailOfflineRefund(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-refund:list")
    @Operation(summary = "查询线下退款状态选项", description = "返回线下退款后台筛选使用的状态契约")
    public R<List<PaymentOfflineRefundStatusVO>> listOfflineRefundStatuses() {
        return R.ok(offlineChannelService.listOfflineRefundStatuses());
    }
}
