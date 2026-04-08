package io.mango.infra.crypto.impl.sm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Sm3CryptoService.
 */
class Sm3CryptoServiceTest {

    private final Sm3CryptoService sm3Service = new Sm3CryptoService();

    @Test
    void hash_stringData_returnsHexEncodedHash() {
        String data = "hello world";
        String hash = sm3Service.hash(data);

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SM3 produces 256-bit = 32 bytes = 64 hex chars
    }

    @Test
    void hash_sameData_returnsSameHash() {
        String data = "test data";

        String hash1 = sm3Service.hash(data);
        String hash2 = sm3Service.hash(data);

        assertEquals(hash1, hash2);
    }

    @Test
    void hash_differentData_returnsDifferentHash() {
        String hash1 = sm3Service.hash("data1");
        String hash2 = sm3Service.hash("data2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void hash_emptyString_returnsValidHash() {
        String hash = sm3Service.hash("");

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void hash_knownValue_returnsExpectedHash() {
        // SM3("abc") computed via BouncyCastle
        String hash = sm3Service.hash("abc");
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0", hash);
    }
}
