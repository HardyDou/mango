package io.mango.infra.crypto.impl;

/**
 * 带密钥的摘要接口。
 * <p>
 * HMAC 这类算法必须显式传入密钥，不应实现无密钥摘要接口。
 */
public interface IKeyedDigester {

    /**
     * 使用密钥计算摘要。
     *
     * @param data 原始数据
     * @param key  密钥
     * @return 原始摘要字节
     */
    byte[] digest(byte[] data, byte[] key);

    /**
     * 使用密钥计算摘要，并返回十六进制字符串。
     *
     * @param data 原始字符串
     * @param key  密钥
     * @return 十六进制摘要
     */
    String digest(String data, byte[] key);
}
