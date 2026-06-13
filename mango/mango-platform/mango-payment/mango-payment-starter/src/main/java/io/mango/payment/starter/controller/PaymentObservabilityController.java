package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.payment.api.PaymentObservabilityApi;
import io.mango.payment.api.vo.PaymentObservabilitySnapshotVO;
import io.mango.payment.core.service.PaymentObservabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/observability")
@RequiredArgsConstructor
@Tag(name = "支付可观测性", description = "支付域运行状态和告警快照接口")
public class PaymentObservabilityController implements PaymentObservabilityApi {

    private final PaymentObservabilityService observabilityService;

    @Override
    @GetMapping("/snapshot")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:observability:query")
    @Operation(summary = "查询支付可观测性快照", description = "按当前租户从真实支付表统计支付、退款、回调、通知、对账、异常和证书到期指标，并返回告警项")
    public R<PaymentObservabilitySnapshotVO> observabilitySnapshot() {
        return R.ok(observabilityService.currentSnapshot());
    }
}
