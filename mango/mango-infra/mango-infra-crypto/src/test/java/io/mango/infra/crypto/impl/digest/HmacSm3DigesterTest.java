package io.mango.infra.crypto.impl.digest;

import io.mango.infra.crypto.impl.IKeyedDigester;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HmacSm3DigesterTest {

    private final IKeyedDigester digester = new HmacSm3Digester();

    @Test
    void digest_shouldBeStableWithSameDataAndKey() {
        byte[] key = "secret-key".getBytes(StandardCharsets.UTF_8);

        byte[] first = digester.digest("hello".getBytes(StandardCharsets.UTF_8), key);
        byte[] second = digester.digest("hello".getBytes(StandardCharsets.UTF_8), key);

        assertArrayEquals(first, second);
        assertEquals(32, first.length);
    }

    @Test
    void digest_shouldRejectEmptyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> digester.digest("hello", new byte[0]));
    }

    @Test
    void digest_shouldMatchBouncyCastleKnownVector() {
        byte[] key = Hex.decode("0b".repeat(20));
        byte[] message = Hex.decode("4869205468657265");

        assertEquals(
                "51b00d1fb49832bfb01c3ce27848e59f871d9ba938dc563b338ca964755cce70",
                Hex.toHexString(digester.digest(message, key)));
    }
}
