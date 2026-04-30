package io.mango.infra.crypto.impl.sm;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

/**
 * SM3 哈希基础实现。
 * <p>
 * 输出 256 位，也就是 32 字节摘要。
 */
public class Sm3CryptoService {

    /**
     * 使用 SM3 计算字符串哈希。
     *
     * @param data 原始数据
     * @return 64 字符十六进制摘要
     */
    public String hash(String data) {
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }
        return hash(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 使用 SM3 计算字节数组哈希。
     *
     * @param data 原始数据
     * @return 64 字符十六进制摘要
     */
    public String hash(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return Hex.toHexString(result);
    }
}
