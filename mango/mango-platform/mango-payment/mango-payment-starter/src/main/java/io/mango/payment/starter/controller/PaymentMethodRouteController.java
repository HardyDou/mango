package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentMethodRouteApi;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.query.PaymentMethodRoutePageQuery;
import io.mango.payment.api.vo.PaymentMethodRouteRuleVO;
import io.mango.payment.api.vo.PaymentMethodRouteTrialVO;
import io.mango.payment.core.service.IPaymentMethodRouteService;
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
@RequestMapping("/payment/method-routes")
@RequiredArgsConstructor
@Tag(name = "支付方式路由策略", description = "支付方式跨通道路由策略和试算接口")
public class PaymentMethodRouteController implements PaymentMethodRouteApi {

    private final IPaymentMethodRouteService routeService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method-route:list")
    @Operation(summary = "分页查询支付方式路由规则", description = "按当前租户查询支付方式跨通道路由规则")
    public R<PageResult<PaymentMethodRouteRuleVO>> pageRouteRules(@ParameterObject PaymentMethodRoutePageQuery query) {
        return routeService.pageRouteRules(query);
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method-route:query")
    @Operation(summary = "查询支付方式路由规则详情", description = "按路由规则 ID 查询明细")
    public R<PaymentMethodRouteRuleVO> detailRouteRule(@Parameter(description = "路由规则 ID", required = true) @RequestParam Long id) {
        return routeService.detailRouteRule(id);
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method-route:add")
    @Operation(summary = "新增支付方式路由规则", description = "创建支付方式跨通道路由规则")
    public R<Long> createRouteRule(@Valid @RequestBody SavePaymentMethodRouteRuleCommand command) {
        return routeService.createRouteRule(command);
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method-route:edit")
    @Operation(summary = "修改支付方式路由规则", description = "更新支付方式跨通道路由规则")
    public R<Boolean> updateRouteRule(@Valid @RequestBody SavePaymentMethodRouteRuleCommand command) {
        return routeService.updateRouteRule(command);
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method-route:delete")
    @Operation(summary = "删除支付方式路由规则", description = "受控删除支付方式跨通道路由规则")
    public R<Boolean> deleteRouteRule(@Parameter(description = "路由规则 ID", required = true) @RequestParam Long id) {
        return routeService.deleteRouteRule(id);
    }

    @Override
    @PostMapping("/trial")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method-route:trial")
    @Operation(summary = "支付方式路由试算", description = "输入应用、主体、金额、终端、场景和支付方式后返回命中能力及过滤原因")
    public R<PaymentMethodRouteTrialVO> trialRoute(@Valid @RequestBody PaymentMethodRouteTrialCommand command) {
        return routeService.trialRoute(command);
    }
}
