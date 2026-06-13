package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.core.service.IPaymentOpenApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Validated
@RestController
@RequestMapping("/openapi/pay")
@RequiredArgsConstructor
@Tag(name = "支付开放接口", description = "业务系统接入支付中心的签名开放接口")
public class PaymentOpenApiController {

    private final IPaymentOpenApiService openApiService;

    @PostMapping("/orders")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口创建业务订单")
    @Operation(summary = "创建业务订单", description = "业务系统通过 AppId、tenantId、timestamp、nonce、signature 创建支付业务订单")
    public R<PaymentOpenBusinessOrderVO> createOrder(
            @RequestBody @NotBlank(message = "请求体不能为空") String body,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.createOrder(openRequest(
                body,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                null,
                null,
                null,
                null));
    }

    @GetMapping("/orders/{bizOrderNo}")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口查询业务订单")
    @Operation(summary = "查询业务订单", description = "业务系统按业务订单号查询本应用下的支付业务订单")
    public R<PaymentOpenBusinessOrderVO> detailOrder(
            @Parameter(description = "业务订单号", required = true) @PathVariable @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.detailOrder(openRequest(
                null,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                resolveClientIp(servletRequest),
                bizOrderNo,
                null,
                null));
    }

    @PostMapping("/orders/{bizOrderNo}/cashier")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口获取收银台地址")
    @Operation(summary = "获取收银台地址", description = "业务系统按未过期业务订单获取 Web/H5 收银台入口")
    public R<PaymentOpenCashierVO> cashier(
            @Parameter(description = "业务订单号", required = true) @PathVariable @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            @RequestBody(required = false) String body,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.cashier(openRequest(
                body,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                null,
                bizOrderNo,
                null,
                null));
    }

    @PostMapping("/orders/{bizOrderNo}/pay")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口发起支付")
    @Operation(summary = "发起支付", description = "业务系统按未过期业务订单和标准支付方式发起真实支付订单")
    public R<PaymentOpenPaymentOrderVO> pay(
            @Parameter(description = "业务订单号", required = true) @PathVariable @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            @RequestBody @NotBlank(message = "请求体不能为空") String body,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.pay(openRequest(
                body,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                resolveClientIp(servletRequest),
                bizOrderNo,
                null,
                null));
    }

    @GetMapping("/payment-orders/{payOrderNo}")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口查询支付订单")
    @Operation(summary = "查询支付订单", description = "业务系统按支付订单号查询本应用下的支付订单")
    public R<PaymentOpenPaymentOrderVO> detailPaymentOrder(
            @Parameter(description = "支付订单号", required = true) @PathVariable @NotBlank(message = "支付订单号不能为空") String payOrderNo,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.detailPaymentOrder(openRequest(
                null,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                null,
                null,
                payOrderNo,
                null));
    }

    @PostMapping("/refunds")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口发起退款")
    @Operation(summary = "发起退款", description = "业务系统按业务退款单号幂等发起原支付订单退款")
    public R<PaymentOpenRefundOrderVO> refund(
            @RequestBody @NotBlank(message = "请求体不能为空") String body,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.refund(openRequest(
                body,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                null,
                null,
                null,
                null));
    }

    @GetMapping("/refunds/{bizRefundNo}")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口查询退款")
    @Operation(summary = "查询退款", description = "业务系统按业务退款单号查询本应用下的退款订单")
    public R<PaymentOpenRefundOrderVO> detailRefund(
            @Parameter(description = "业务退款单号", required = true) @PathVariable @NotBlank(message = "业务退款单号不能为空") String bizRefundNo,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.detailRefund(openRequest(
                null,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                null,
                null,
                null,
                bizRefundNo));
    }

    @GetMapping("/receipts/{bizOrderNo}")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付开放接口获取支付凭证")
    @Operation(summary = "获取支付凭证", description = "业务系统按业务订单号获取本应用成功支付订单的交易流水、通道交易号和付款时间等支付域凭证信息")
    public R<PaymentOpenReceiptVO> receipt(
            @Parameter(description = "业务订单号", required = true) @PathVariable @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            @Parameter(description = "支付应用 AppId", required = true) @RequestHeader(value = "AppId", required = false) String appId,
            @Parameter(description = "租户 ID", required = true) @RequestHeader(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "请求时间戳，Unix 秒", required = true) @RequestHeader(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机串", required = true) @RequestHeader(value = "nonce", required = false) String nonce,
            @Parameter(description = "Base64 HMAC-SHA256 签名", required = true) @RequestHeader(value = "signature", required = false) String signature) {
        HttpServletRequest servletRequest = currentRequest();
        return openApiService.receipt(openRequest(
                null,
                firstText(appId, servletRequest.getHeader("X-Mango-Payment-App-Id")),
                firstText(tenantId, servletRequest.getHeader("X-Mango-Payment-Tenant-Id")),
                firstText(timestamp, servletRequest.getHeader("X-Mango-Payment-Timestamp")),
                firstText(nonce, servletRequest.getHeader("X-Mango-Payment-Nonce")),
                firstText(signature, servletRequest.getHeader("X-Mango-Payment-Signature")),
                servletRequest.getRequestURI(),
                null,
                bizOrderNo,
                null,
                null));
    }

    private PaymentOpenRequestCommand openRequest(
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath,
            String clientIp,
            String bizOrderNo,
            String payOrderNo,
            String bizRefundNo) {
        PaymentOpenRequestCommand command = new PaymentOpenRequestCommand();
        command.setBody(body);
        command.setAppId(appId);
        command.setTenantId(tenantId);
        command.setTimestamp(timestamp);
        command.setNonce(nonce);
        command.setSignature(signature);
        command.setRequestPath(requestPath);
        command.setClientIp(clientIp);
        command.setBizOrderNo(bizOrderNo);
        command.setPayOrderNo(payOrderNo);
        command.setBizRefundNo(bizRefundNo);
        return command;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private String firstText(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            return forwarded;
        }
        String realIp = firstText(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return firstText(request.getRemoteAddr());
    }

    private String firstHeaderValue(String value) {
        String text = firstText(value);
        if (text == null) {
            return null;
        }
        int commaIndex = text.indexOf(',');
        if (commaIndex < 0) {
            return text;
        }
        return firstText(text.substring(0, commaIndex));
    }

    private String firstText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
