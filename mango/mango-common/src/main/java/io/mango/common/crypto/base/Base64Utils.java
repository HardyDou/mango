package io.mango.common.crypto.base;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 utility class.
 *
 * @author Mango
 * @deprecated Use {@link io.mango.infra.crypto.impl.base.Base64Utils} instead.
 *             This class remains here for backward compatibility.
 *             Will be removed in a future version.
 */
@Deprecated
public final class Base64Utils {

    private Base64Utils() {
    }

    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static String encode(String data) {
        return encode(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public static String decodeToString(String data) {
        return new String(decode(data), StandardCharsets.UTF_8);
    }
}
