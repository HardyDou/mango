package io.mango.infra.crypto.impl;

/**
 * 无密钥摘要接口。
 * <p>
 * 只适用于 SHA-256、SM3 这类普通哈希算法；HMAC 应使用 {@link IKeyedDigester}。
 */
public interface IDigester {

    /**
     * 计算字符串摘要。
     *
     * @param data 原始数据
     * @return 十六进制摘要
     */
    String digest(String data);

    /**
     * 计算字节数组摘要。
     *
     * @param data 原始数据
     * @return 原始摘要字节
     */
    byte[] digest(byte[] data);
}
