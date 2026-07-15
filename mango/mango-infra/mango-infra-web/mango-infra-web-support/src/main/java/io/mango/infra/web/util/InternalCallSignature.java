package io.mango.infra.web.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Internal HTTP call signature contract shared by clients and servers. */
public final class InternalCallSignature {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String QUERY_SEPARATOR = "&";

    private InternalCallSignature() {
    }

    public static String sign(String timestamp, String nonce, String method, String path,
                              String canonicalQuery, String secret) {
        String payload = String.join(":", timestamp, nonce, method, path, canonicalQuery);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to calculate internal call signature", exception);
        }
    }

    public static boolean matches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII));
    }

    public static String canonicalizeRawQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        return Stream.of(query.split(QUERY_SEPARATOR)).sorted().collect(Collectors.joining(QUERY_SEPARATOR));
    }

    public static String canonicalizeQueries(Map<String, ? extends Collection<String>> queries) {
        if (queries == null || queries.isEmpty()) {
            return "";
        }
        return queries.entrySet().stream()
                .flatMap(entry -> values(entry.getKey(), entry.getValue()))
                .sorted()
                .collect(Collectors.joining(QUERY_SEPARATOR));
    }

    private static Stream<String> values(String key, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Stream.of(key);
        }
        return values.stream().map(value -> key + "=" + value);
    }
}
