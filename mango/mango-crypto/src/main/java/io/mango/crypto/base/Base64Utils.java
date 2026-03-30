package io.mango.crypto.base;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 utility class
 *
 * @author Mango
 */
public class Base64Utils {

    private Base64Utils() {
    }

    /**
     * Encode bytes to Base64 string
     */
    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Encode string to Base64 string
     */
    public static String encode(String data) {
        return encode(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode Base64 string to bytes
     */
    public static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    /**
     * Decode Base64 string to string
     */
    public static String decodeToString(String data) {
        return new String(decode(data), StandardCharsets.UTF_8);
    }
}
