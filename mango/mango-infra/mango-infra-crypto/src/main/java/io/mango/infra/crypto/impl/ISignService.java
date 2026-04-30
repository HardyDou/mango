package io.mango.infra.crypto.impl;

/**
 * 签名验签服务接口。
 */
public interface ISignService {

    /**
     * 对数据签名。
     *
     * @param data 待签名数据
     * @return Base64 编码的签名
     */
    String sign(String data);

    /**
     * 验证签名。
     *
     * @param data      原始数据
     * @param signature Base64 编码的签名
     * @return true if signature is valid
     */
    boolean verify(String data, String signature);
}
