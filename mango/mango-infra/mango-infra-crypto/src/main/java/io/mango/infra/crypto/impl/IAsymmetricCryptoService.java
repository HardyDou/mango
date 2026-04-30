package io.mango.infra.crypto.impl;

/**
 * 非对称公钥加密接口。
 * <p>
 * 该接口只表达公钥加密能力，不承诺私钥解密能力。私钥解密需要独立接口和明确的密钥托管策略。
 */
public interface IAsymmetricCryptoService {

    /**
     * 加密明文。
     *
     * @param plaintext 明文
     * @return Base64 编码的密文
     */
    String encrypt(String plaintext);
}
