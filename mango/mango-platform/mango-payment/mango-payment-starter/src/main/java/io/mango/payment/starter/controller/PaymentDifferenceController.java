package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentDifferenceApi;
import io.mango.payment.api.command.HandlePaymentDifferenceCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentDifferenceActionVO;
import io.mango.payment.api.vo.PaymentDifferenceStatusVO;
import io.mango.payment.api.vo.PaymentDifferenceVO;
import io.mango.payment.core.service.PaymentDifferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/payment/differences")
@RequiredArgsConstructor
@Tag(name = "对账差异", description = "通道账单差异查询和受控处理接口")
public class PaymentDifferenceController implements PaymentDifferenceApi {

    private final PaymentDifferenceService differenceService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:list")
    @Operation(summary = "分页查询对账差异", description = "查询我方与通道账单不一致的差异单和处理状态")
    public R<PageResult<PaymentDifferenceVO>> pageDifferences(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(differenceService.pageDifferences(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:query")
    @Operation(summary = "查询对账差异详情", description = "按差异单 ID 查询差异原因、处理动作、处理人和凭据")
    public R<PaymentDifferenceVO> detailDifference(@Parameter(description = "对账差异 ID", required = true) @RequestParam Long id) {
        return R.ok(differenceService.detailDifference(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:list")
    @Operation(summary = "查询对账差异处理状态选项", description = "返回差异处理后台筛选使用的处理状态契约")
    public R<List<PaymentDifferenceStatusVO>> listDifferenceStatuses() {
        return R.ok(differenceService.listDifferenceStatuses());
    }

    @Override
    @GetMapping("/actions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:handle")
    @Operation(summary = "查询对账差异处理动作选项", description = "返回对账差异后台受控处理动作契约")
    public R<List<PaymentDifferenceActionVO>> listDifferenceActions() {
        return R.ok(differenceService.listDifferenceActions());
    }

    @Override
    @PostMapping("/handle")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:handle")
    @Operation(summary = "处理对账差异", description = "受控处理对账差异，记录动作、原因、处理人、时间和凭据，不直接修改支付或退款成功状态")
    public R<PaymentDifferenceVO> handleDifference(@Valid @RequestBody HandlePaymentDifferenceCommand command) {
        return R.ok(differenceService.handleDifference(command));
    }
}
