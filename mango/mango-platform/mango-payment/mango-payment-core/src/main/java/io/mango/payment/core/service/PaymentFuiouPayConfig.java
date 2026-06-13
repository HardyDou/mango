package io.mango.payment.core.service;

/**
 * 富友支付签约运行配置。
 *
 * @param insCd 机构号
 * @param merchantNo 富友商户号
 * @param scanpayGatewayBaseUrl 扫码接口基础地址
 * @param notifyUrl 扫码异步通知地址
 * @param privateKey 商户私钥
 * @param fuiouPublicKey 富友公钥
 * @param operatorId 操作员
 * @param gatewayMerchantNo PC 网关商户号
 * @param gatewayMerchantKey PC 网关商户密钥
 * @param gatewayPayUrl PC 网关支付地址
 * @param gatewayQueryUrl PC 网关查单地址
 * @param gatewayPageNotifyUrl PC 网关页面跳转地址
 * @param gatewayBackNotifyUrl PC 网关后台通知地址
 */
public record PaymentFuiouPayConfig(
        String insCd,
        String merchantNo,
        String scanpayGatewayBaseUrl,
        String notifyUrl,
        String privateKey,
        String fuiouPublicKey,
        String operatorId,
        String gatewayMerchantNo,
        String gatewayMerchantKey,
        String gatewayPayUrl,
        String gatewayQueryUrl,
        String gatewayPageNotifyUrl,
        String gatewayBackNotifyUrl) {
}
