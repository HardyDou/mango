package io.mango.infra.crypto.impl.base;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 编解码工具。
 *
 * @author Mango
 */
public final class Base64Utils {

    private Base64Utils() {
    }

    /**
     * 将字节数组编码为 Base64 字符串。
     */
    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * 将字符串编码为 Base64 字符串。
     */
    public static String encode(String data) {
        return encode(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将 Base64 字符串解码为字节数组。
     */
    public static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    /**
     * 将 Base64 字符串解码为字符串。
     */
    public static String decodeToString(String data) {
        return new String(decode(data), StandardCharsets.UTF_8);
    }
}
