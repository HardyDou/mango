package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCashierConfigApi;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentCashierConfigVO;
import io.mango.payment.core.service.IPaymentCashierConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/cashier-configs")
@RequiredArgsConstructor
@Tag(name = "收银台配置", description = "收银台配置后台管理接口")
public class PaymentCashierConfigController implements PaymentCashierConfigApi {

    private final IPaymentCashierConfigService cashierConfigService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:cashier-config:list")
    @Operation(summary = "分页查询收银台配置", description = "按当前租户查询收银台配置")
    public R<PageResult<PaymentCashierConfigVO>> pageCashierConfigs(@ParameterObject PaymentConfigPageQuery query) {
        return cashierConfigService.pageCashierConfigs(query);
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:cashier-config:query")
    @Operation(summary = "查询收银台配置详情", description = "按收银台配置 ID 查询详情")
    public R<PaymentCashierConfigVO> detailCashierConfig(@Parameter(description = "收银台配置 ID", required = true) @RequestParam Long id) {
        return cashierConfigService.detailCashierConfig(id);
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:cashier-config:add")
    @Operation(summary = "新增收银台配置", description = "创建收银台配置")
    public R<Long> createCashierConfig(@Valid @RequestBody SavePaymentCashierConfigCommand command) {
        return cashierConfigService.createCashierConfig(command);
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:cashier-config:edit")
    @Operation(summary = "修改收银台配置", description = "更新收银台配置")
    public R<Boolean> updateCashierConfig(@Valid @RequestBody SavePaymentCashierConfigCommand command) {
        return cashierConfigService.updateCashierConfig(command);
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:cashier-config:delete")
    @Operation(summary = "删除收银台配置", description = "受控逻辑删除收银台配置；存在支付订单或芒果支付记录时拒绝删除")
    public R<Boolean> deleteCashierConfig(@Parameter(description = "收银台配置 ID", required = true) @RequestParam Long id) {
        return cashierConfigService.deleteCashierConfig(id);
    }
}
