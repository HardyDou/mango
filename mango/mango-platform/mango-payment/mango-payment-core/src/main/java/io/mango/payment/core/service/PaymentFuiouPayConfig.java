package io.mango.payment.core.service;

/**
 * 富友支付签约运行配置。
 *
 * @param insCd 机构号
 * @param merchantNo 富友商户号
 * @param termId 终端号
 * @param gatewayBaseUrl 网关基础地址
 * @param notifyUrl 异步通知地址
 * @param privateKey 商户私钥
 * @param fuiouPublicKey 富友公钥
 * @param termIp 终端 IP
 * @param operatorId 操作员
 */
public record PaymentFuiouPayConfig(
        String insCd,
        String merchantNo,
        String termId,
        String gatewayBaseUrl,
        String notifyUrl,
        String privateKey,
        String fuiouPublicKey,
        String termIp,
        String operatorId) {
}
