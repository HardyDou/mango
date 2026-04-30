package io.mango.infra.crypto.impl;

/**
 * 对称加解密服务接口。
 */
public interface ICryptoService {

    /**
     * 加密明文。
     *
     * @param plaintext 明文
     * @return Base64 编码的密文
     */
    String encrypt(String plaintext);

    /**
     * 使用指定 IV 加密明文。
     *
     * @param plaintext 明文
     * @param iv        Base64 编码的 IV
     * @return Base64 编码的密文
     */
    String encrypt(String plaintext, String iv);

    /**
     * 解密密文。
     *
     * @param ciphertext Base64 编码的密文
     * @return 明文
     */
    String decrypt(String ciphertext);

    /**
     * 解密密文。
     * <p>
     * 当前 SM4/CBC 实现会从密文前缀读取 16 字节 IV；{@code iv} 参数仅为接口兼容保留。
     *
     * @param ciphertext Base64 编码的密文；CBC 模式下格式为 Base64(IV(16 bytes) || encryptedPayload)
     * @param iv         为兼容保留，当前 SM4/CBC 实现不使用该参数
     * @return 明文
     */
    String decrypt(String ciphertext, String iv);
}
