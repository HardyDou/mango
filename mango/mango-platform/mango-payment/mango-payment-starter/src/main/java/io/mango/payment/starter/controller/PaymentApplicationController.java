package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentApplicationApi;
import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import io.mango.payment.api.command.UpdatePaymentApplicationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentApplicationSaveResultVO;
import io.mango.payment.api.vo.PaymentApplicationVO;
import io.mango.payment.core.service.IPaymentApplicationService;
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
@RequestMapping("/payment/applications")
@RequiredArgsConstructor
@Tag(name = "支付应用管理", description = "支付接入应用后台管理接口")
public class PaymentApplicationController implements PaymentApplicationApi {

    private final IPaymentApplicationService applicationService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:application:list")
    @Operation(summary = "分页查询支付应用", description = "按当前租户查询支付接入应用")
    public R<PageResult<PaymentApplicationVO>> pageApplications(@ParameterObject PaymentConfigPageQuery query) {
        return applicationService.pageApplications(query);
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:application:query")
    @Operation(summary = "查询支付应用详情", description = "按应用 ID 查询支付接入应用详情")
    public R<PaymentApplicationVO> detailApplication(@Parameter(description = "应用 ID", required = true) @RequestParam Long id) {
        return applicationService.detailApplication(id);
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:application:add")
    @Operation(summary = "新增支付应用", description = "创建支付接入应用")
    public R<PaymentApplicationSaveResultVO> createApplication(@Valid @RequestBody CreatePaymentApplicationCommand command) {
        return applicationService.createApplication(command);
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:application:edit")
    @Operation(summary = "修改支付应用", description = "更新支付接入应用")
    public R<PaymentApplicationSaveResultVO> updateApplication(@Valid @RequestBody UpdatePaymentApplicationCommand command) {
        return applicationService.updateApplication(command);
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:application:delete")
    @Operation(summary = "删除支付应用", description = "受控逻辑删除支付接入应用；存在收银台、订单、流水、通知、差异等关联数据时拒绝删除")
    public R<Boolean> deleteApplication(@Parameter(description = "应用 ID", required = true) @RequestParam Long id) {
        return applicationService.deleteApplication(id);
    }
}
