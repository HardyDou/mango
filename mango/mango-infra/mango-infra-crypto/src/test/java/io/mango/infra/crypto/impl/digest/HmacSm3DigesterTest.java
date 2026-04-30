package io.mango.infra.crypto.impl.digest;

import io.mango.infra.crypto.impl.IKeyedDigester;
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
}
