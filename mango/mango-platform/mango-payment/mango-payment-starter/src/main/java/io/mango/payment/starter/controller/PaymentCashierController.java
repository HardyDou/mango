package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.payment.api.PaymentCashierApi;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.command.SubmitOfflineTransferVoucherCommand;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentCashierSessionVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.core.service.IPaymentCashierService;
import io.mango.payment.core.service.PaymentOfflineChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Validated
@RestController
@RequestMapping("/payment/cashier")
@RequiredArgsConstructor
@Tag(name = "支付收银台", description = "投产收银台会话与支付接口")
public class PaymentCashierController implements PaymentCashierApi {

    private final IPaymentCashierService cashierService;
    private final PaymentOfflineChannelService offlineChannelService;

    @Override
    @GetMapping("/session")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询付款人收银台会话")
    @Operation(summary = "查询收银台会话", description = "按收银台配置和业务订单生成付款人可见的收银台会话视图")
    public R<PaymentCashierSessionVO> detailSession(
            @Parameter(description = "收银台配置 ID", required = true) @NotNull(message = "收银台配置 ID 不能为空") @RequestParam Long cashierConfigId,
            @Parameter(description = "业务订单 ID。后台预览可为空") @RequestParam(required = false) Long businessOrderId) {
        return cashierService.detailSession(cashierConfigId, businessOrderId);
    }

    @Override
    @PostMapping("/pay")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "付款人提交收银台支付")
    @Operation(summary = "提交收银台支付", description = "按收银台会话重新校验订单、主体、支付方式和签约能力后发起支付")
    public R<PaymentCashierPayResultVO> pay(@Valid @RequestBody PaymentCashierPayCommand command) {
        HttpServletRequest request = currentRequest();
        command.setClientIp(resolveClientIp(request));
        return cashierService.pay(command);
    }

    @Override
    @GetMapping("/pay-result")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询付款人收银台支付结果")
    @Operation(summary = "查询收银台支付结果", description = "按支付订单号查询真实支付订单状态，用于收银台等待回调或查单后刷新结果")
    public R<PaymentCashierPayResultVO> payResult(
            @Parameter(description = "支付订单号", required = true) @NotBlank(message = "支付订单号不能为空") @RequestParam String payOrderNo) {
        return cashierService.payResult(payOrderNo);
    }

    @Override
    @PostMapping("/pay-result/sync")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "付款人确认付款后同步支付结果")
    @Operation(summary = "同步收银台支付结果", description = "付款人确认已付款后调用支付通道查单，并返回最新收银台支付结果")
    public R<PaymentCashierPayResultVO> syncPayResult(
            @Parameter(description = "支付订单号", required = true) @NotBlank(message = "支付订单号不能为空") @RequestParam String payOrderNo) {
        return cashierService.syncPayResult(payOrderNo);
    }

    @Override
    @PostMapping("/offline-collections/transfer-voucher")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "付款人提交线下转账凭证")
    @Operation(summary = "提交线下转账凭证", description = "付款人在线下收款通道完成转账后提交实际转账金额和凭证文件 ID")
    public R<PaymentOfflineCollectionVO> submitOfflineTransferVoucher(
            @Valid @RequestBody SubmitOfflineTransferVoucherCommand command) {
        return R.ok(offlineChannelService.submitTransferVoucher(command));
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
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
