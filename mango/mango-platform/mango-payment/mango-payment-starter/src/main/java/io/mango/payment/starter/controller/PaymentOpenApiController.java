package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.payment.api.PaymentOpenApi;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.core.service.IPaymentOpenApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Validated
@RestController
@RequestMapping("/openapi/pay")
@RequiredArgsConstructor
@Tag(name = "支付开放接口", description = "业务系统接入支付中心的签名开放接口")
public class PaymentOpenApiController implements PaymentOpenApi {

    private final IPaymentOpenApiService openApiService;

    @Override
    @PostMapping("/orders/create")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口创建业务订单")
    @Operation(summary = "创建业务订单", description = "校验签名与幂等键后创建支付业务订单")
    public R<PaymentOpenBusinessOrderVO> createOrder(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.createOrder(enrich(command, "/openapi/pay/orders/create", true)));
    }

    @Override
    @PostMapping("/orders/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口查询业务订单")
    @Operation(summary = "查询业务订单", description = "校验签名后按业务订单号查询支付业务订单")
    public R<PaymentOpenBusinessOrderVO> detailOrder(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.detailOrder(enrich(command, "/openapi/pay/orders/detail", true)));
    }

    @Override
    @PostMapping("/cashier/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口获取收银台")
    @Operation(summary = "获取收银台", description = "校验签名后获取业务订单对应的收银台入口")
    public R<PaymentOpenCashierVO> cashier(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.cashier(enrich(command, "/openapi/pay/cashier/detail", true)));
    }

    @Override
    @PostMapping("/payments/create")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口发起支付")
    @Operation(summary = "发起支付", description = "校验签名后按业务订单和支付方式发起支付")
    public R<PaymentOpenPaymentOrderVO> pay(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.pay(enrich(command, "/openapi/pay/payments/create", true)));
    }

    @Override
    @PostMapping("/payments/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口查询支付订单")
    @Operation(summary = "查询支付订单", description = "校验签名后按支付订单号查询支付结果")
    public R<PaymentOpenPaymentOrderVO> detailPaymentOrder(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.detailPaymentOrder(enrich(command, "/openapi/pay/payments/detail", true)));
    }

    @Override
    @PostMapping("/refunds/create")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口发起退款")
    @Operation(summary = "发起退款", description = "校验签名与幂等键后发起支付退款")
    public R<PaymentOpenRefundOrderVO> refund(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.refund(enrich(command, "/openapi/pay/refunds/create", true)));
    }

    @Override
    @PostMapping("/refunds/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口查询退款")
    @Operation(summary = "查询退款", description = "校验签名后按业务退款单号查询退款结果")
    public R<PaymentOpenRefundOrderVO> detailRefund(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.detailRefund(enrich(command, "/openapi/pay/refunds/detail", true)));
    }

    @Override
    @PostMapping("/receipts/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口获取支付凭证")
    @Operation(summary = "获取支付凭证", description = "校验签名后按业务订单号获取支付凭证")
    public R<PaymentOpenReceiptVO> receipt(
            @Valid @RequestBody PaymentOpenRequestCommand command) {
        return R.ok(openApiService.receipt(enrich(command, "/openapi/pay/receipts/detail", true)));
    }

    private PaymentOpenRequestCommand enrich(
            PaymentOpenRequestCommand command, String requestPath, boolean resolveClientIp) {
        command.setRequestPath(requestPath);
        if (resolveClientIp) {
            command.setClientIp(clientIp(currentRequest()));
        }
        return command;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            return forwarded;
        }
        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        return realIp == null ? trimToNull(request.getRemoteAddr()) : realIp;
    }

    private String firstHeaderValue(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(',');
        return separator < 0 ? normalized : trimToNull(normalized.substring(0, separator));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
