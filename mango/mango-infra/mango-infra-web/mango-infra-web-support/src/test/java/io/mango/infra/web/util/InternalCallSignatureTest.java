package io.mango.infra.web.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalCallSignatureTest {

    @Test
    void canonicalizeQueries_rawAndStructuredMultiValue_shareOrder() {
        String raw = InternalCallSignature.canonicalizeRawQuery("z=9&a=2&a=1&flag");
        String structured = InternalCallSignature.canonicalizeQueries(Map.of(
                "z", List.of("9"), "a", List.of("2", "1"), "flag", List.of()));

        assertEquals("a=1&a=2&flag&z=9", raw);
        assertEquals(raw, structured);
    }

    @Test
    void matches_signedPayload_usesDeterministicContract() {
        String signature = InternalCallSignature.sign(
                "100", "nonce", "POST", "/internal/path", "a=1&b=2", "secret");

        assertEquals(64, signature.length());
        assertTrue(InternalCallSignature.matches(signature, signature));
        assertFalse(InternalCallSignature.matches(signature, signature.substring(1) + "0"));
        assertFalse(InternalCallSignature.matches(null, signature));
    }
}
