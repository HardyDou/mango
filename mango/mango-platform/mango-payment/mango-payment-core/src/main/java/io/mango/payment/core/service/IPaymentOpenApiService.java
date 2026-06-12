package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;

public interface IPaymentOpenApiService {

    R<PaymentOpenBusinessOrderVO> createOrder(
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath);

    R<PaymentOpenBusinessOrderVO> detailOrder(
            String bizOrderNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath,
            String clientIp);

    R<PaymentOpenCashierVO> cashier(
            String bizOrderNo,
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath);

    R<PaymentOpenPaymentOrderVO> pay(
            String bizOrderNo,
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath,
            String clientIp);

    R<PaymentOpenPaymentOrderVO> detailPaymentOrder(
            String payOrderNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath);

    R<PaymentOpenRefundOrderVO> refund(
            String body,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath);

    R<PaymentOpenRefundOrderVO> detailRefund(
            String bizRefundNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath);

    R<PaymentOpenReceiptVO> receipt(
            String bizOrderNo,
            String appId,
            String tenantId,
            String timestamp,
            String nonce,
            String signature,
            String requestPath);
}
