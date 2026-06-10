package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentOpenApi {

    R<PaymentOpenBusinessOrderVO> createOrder(
            @NotBlank(message = "请求体不能为空") String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);

    R<PaymentOpenBusinessOrderVO> detailOrder(
            @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);

    R<PaymentOpenCashierVO> cashier(
            @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);

    R<PaymentOpenPaymentOrderVO> pay(
            @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            @NotBlank(message = "请求体不能为空") String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);

    R<PaymentOpenPaymentOrderVO> detailPaymentOrder(
            @NotBlank(message = "支付订单号不能为空") String payOrderNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);

    R<PaymentOpenRefundOrderVO> refund(
            @NotBlank(message = "请求体不能为空") String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);

    R<PaymentOpenRefundOrderVO> detailRefund(
            @NotBlank(message = "业务退款单号不能为空") String bizRefundNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);

    R<PaymentOpenReceiptVO> receipt(
            @NotBlank(message = "业务订单号不能为空") String bizOrderNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            HttpServletRequest servletRequest);
}
