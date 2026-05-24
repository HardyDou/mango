package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentManagementApi;
import io.mango.payment.api.vo.PaymentManageDomainVO;
import io.mango.payment.api.vo.PaymentManageItemVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import io.mango.payment.api.vo.PaymentTenantCashierVO;
import io.mango.payment.core.service.IPaymentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/management")
@RequiredArgsConstructor
@Tag(name = "支付后台管理", description = "支付中心后台管理域、配置项、租户收银台和沙箱支付方式接口")
public class PaymentManagementController implements PaymentManagementApi {

    private final IPaymentManagementService paymentManagementService;

    @Override
    @GetMapping("/domains")
    @Operation(summary = "查询支付管理域", description = "查询设计文档要求的支付中心后台管理页面域")
    public R<List<PaymentManageDomainVO>> listDomains() {
        return R.ok(paymentManagementService.listDomains());
    }

    @Override
    @GetMapping("/items")
    @Operation(summary = "查询支付管理配置项", description = "按管理域查询应用、主体、通道、方式、收银台、对账、结算和审计配置项")
    public R<List<PaymentManageItemVO>> listItems(
            @Parameter(description = "管理域编码", required = true)
            @NotBlank(message = "管理域不能为空")
            @RequestParam String domain) {
        return R.ok(paymentManagementService.listItems(domain));
    }

    @Override
    @GetMapping("/tenant-cashiers")
    @Operation(summary = "查询租户收银台", description = "查询每个租户可使用的沙箱收银台配置")
    public R<List<PaymentTenantCashierVO>> listTenantCashiers() {
        return R.ok(paymentManagementService.listTenantCashiers());
    }

    @Override
    @GetMapping("/sandbox-methods")
    @Operation(summary = "查询沙箱支付方式", description = "查询独立沙箱环境支持的支付方式")
    public R<List<PaymentMethodVO>> listSandboxMethods() {
        return R.ok(paymentManagementService.listSandboxMethods());
    }
}
