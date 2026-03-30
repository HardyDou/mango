package io.mango.crypto;

import io.mango.crypto.base.Base64Utils;
import io.mango.crypto.digest.Sha256Digester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Crypto module unit tests
 */
class CryptoTest {

    @Test
    void testBase64EncodeDecode() {
        String original = "Hello, Mango Crypto!";
        String encoded = Base64Utils.encode(original);
        String decoded = Base64Utils.decodeToString(encoded);

        assertEquals(original, decoded);
        assertNotEquals(original, encoded);
    }

    @Test
    void testSha256Digest() {
        Sha256Digester digester = new Sha256Digester();

        String data = "Hello, Mango!";
        String digest = digester.digest(data);

        assertNotNull(digest);
        assertEquals(64, digest.length()); // SHA-256 produces 32 bytes = 64 hex chars
        assertEquals(digester.digest(data), digest); // Same input = same output
    }

    @Test
    void testSha256DifferentInputs() {
        Sha256Digester digester = new Sha256Digester();

        String digest1 = digester.digest("Hello");
        String digest2 = digester.digest("World");

        assertNotEquals(digest1, digest2);
    }

    @Test
    void testBase64DecodeInvalid() {
        assertThrows(Exception.class, () -> {
            Base64Utils.decode("not-valid-base64!!!");
        });
    }
}
