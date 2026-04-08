package io.mango.infra.crypto.impl.sm;

import io.mango.infra.crypto.starter.CryptoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Sm4CryptoService.
 */
class Sm4CryptoServiceTest {

    // Valid 16-byte (128-bit) SM4 key in hex (32 hex chars = 16 bytes)
    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef";
    // A 16-byte IV in hex (used for explicit IV tests)
    private static final String TEST_IV = "1234567890abcdef1234567890abcdef";

    // Valid 16-byte SM4 key in Base64 (16 bytes = 22 Base64 chars with padding)
    // echo -n "0123456789abcdef" | base64 -> MDEyMzQ1Njc4OWFiY2RlZg==
    private static final String TEST_KEY_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZg==";

    private Sm4CryptoService cbcService;
    private Sm4CryptoService ecbService;

    @BeforeEach
    void setUp() {
        CryptoProperties propsCbc = new CryptoProperties();
        CryptoProperties.Sm4Config sm4Cbc = new CryptoProperties.Sm4Config();
        sm4Cbc.setSecretKey(TEST_KEY);
        sm4Cbc.setMode("CBC");
        sm4Cbc.setPadding("PKCS5Padding");
        propsCbc.setSm4(sm4Cbc);
        cbcService = new Sm4CryptoService(propsCbc);

        CryptoProperties propsEcb = new CryptoProperties();
        CryptoProperties.Sm4Config sm4Ecb = new CryptoProperties.Sm4Config();
        sm4Ecb.setSecretKey(TEST_KEY);
        sm4Ecb.setMode("ECB");
        sm4Ecb.setPadding("PKCS5Padding");
        propsEcb.setSm4(sm4Ecb);
        ecbService = new Sm4CryptoService(propsEcb);
    }

    // --- CBC mode tests ---

    @Test
    void encrypt_decrypt_roundtrip_with_auto_generated_iv() {
        String plaintext = "Hello, SM4 CBC!";
        String ciphertext = cbcService.encrypt(plaintext);
        String decrypted = cbcService.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_same_plaintext_different_ciphertext_due_to_random_iv() {
        String plaintext = "Hello, SM4!";
        String ciphertext1 = cbcService.encrypt(plaintext);
        String ciphertext2 = cbcService.encrypt(plaintext);
        // Different random IVs → different ciphertexts
        assertNotEquals(ciphertext1, ciphertext2);
        // But both decrypt to the same plaintext
        assertEquals(plaintext, cbcService.decrypt(ciphertext1));
        assertEquals(plaintext, cbcService.decrypt(ciphertext2));
    }

    @Test
    void encrypt_decrypt_empty_string() {
        String plaintext = "";
        String ciphertext = cbcService.encrypt(plaintext);
        assertNotNull(ciphertext);
        assertFalse(ciphertext.isEmpty());
        assertEquals(plaintext, cbcService.decrypt(ciphertext));
    }

    @Test
    void encrypt_decrypt_chinese_characters() {
        String plaintext = "中文测试数据 abc 123 !@#";
        String ciphertext = cbcService.encrypt(plaintext);
        assertEquals(plaintext, cbcService.decrypt(ciphertext));
    }

    @Test
    void encrypt_decrypt_long_text() {
        String plaintext = "A".repeat(1000);
        String ciphertext = cbcService.encrypt(plaintext);
        assertEquals(plaintext, cbcService.decrypt(ciphertext));
    }

    @Test
    void decrypt_tampered_ciphertext_throws_exception() {
        String plaintext = "secret data";
        String ciphertext = cbcService.encrypt(plaintext);
        // Tamper with the ciphertext
        String tampered = ciphertext.substring(0, ciphertext.length() - 2) + "XX";
        assertThrows(RuntimeException.class, () -> cbcService.decrypt(tampered));
    }

    // --- ECB mode tests ---

    @Test
    void ecb_encrypt_decrypt_roundtrip() {
        String plaintext = "Hello, SM4 ECB!";
        String ciphertext = ecbService.encrypt(plaintext);
        String decrypted = ecbService.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void ecb_same_plaintext_same_ciphertext() {
        String plaintext = " deterministic";
        String ciphertext1 = ecbService.encrypt(plaintext);
        String ciphertext2 = ecbService.encrypt(plaintext);
        // ECB is deterministic for same key
        assertEquals(ciphertext1, ciphertext2);
    }

    // --- Null/input validation tests ---

    @Test
    void encrypt_null_plaintext_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class, () -> cbcService.encrypt(null));
    }

    @Test
    void decrypt_null_ciphertext_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class, () -> cbcService.decrypt(null));
    }

    @Test
    void decrypt_short_ciphertext_throws_illegal_argument() {
        // CBC ciphertext must be at least IV (16 bytes) + 1 block (16 bytes) = 32 bytes
        // After Base64, at least ~44 chars
        assertThrows(RuntimeException.class, () -> cbcService.decrypt("dG9vLXNob3J0"));
    }

    // --- Explicit IV tests (P1 coverage) ---

    @Test
    void encrypt_decrypt_with_explicit_iv_roundtrip() {
        String plaintext = "explicit IV test data";
        String ciphertext = cbcService.encrypt(plaintext, TEST_IV);
        String decrypted = cbcService.decrypt(ciphertext, TEST_IV);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void decrypt_wrong_iv_throws_bad_padding() {
        String plaintext = "secret data";
        String ciphertext = cbcService.encrypt(plaintext, TEST_IV);
        // Use a different IV for decryption — padding check fails, throws exception
        String wrongIv = "00000000000000000000000000000000";
        assertThrows(RuntimeException.class, () -> cbcService.decrypt(ciphertext, wrongIv));
    }

    @Test
    void decrypt_with_explicit_iv_ignores_ciphertext_prefix() {
        // When iv is provided, the IV prepended to ciphertext is NOT used
        String plaintext = "explicit iv ignores prefix";
        String ciphertext = cbcService.encrypt(plaintext, TEST_IV);
        // Decrypt with explicit iv — should work correctly
        String decrypted = cbcService.decrypt(ciphertext, TEST_IV);
        assertEquals(plaintext, decrypted);
    }

    // --- validateIvLength tests (P1 coverage) ---

    @Test
    void decrypt_wrong_iv_length_throws() {
        String plaintext = "test";
        String ciphertext = cbcService.encrypt(plaintext);
        // 15-byte IV is invalid
        String shortIv = "1234567890abcdef1";
        assertThrows(RuntimeException.class, () -> cbcService.decrypt(ciphertext, shortIv));
    }

    @Test
    void encrypt_wrong_iv_length_throws() {
        String plaintext = "test";
        String shortIv = "1234567890abcdef1"; // 15 bytes
        assertThrows(RuntimeException.class, () -> cbcService.encrypt(plaintext, shortIv));
    }

    // --- decodeKey Base64 branch tests (P2 coverage) ---

    @Test
    void decodeKey_valid_base64_key_works() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm4Config sm4 = new CryptoProperties.Sm4Config();
        sm4.setSecretKey(TEST_KEY_BASE64); // Base64-encoded 16-byte key
        sm4.setMode("CBC");
        sm4.setPadding("PKCS5Padding");
        props.setSm4(sm4);
        Sm4CryptoService svc = new Sm4CryptoService(props);
        String ciphertext = svc.encrypt("hello");
        assertEquals("hello", svc.decrypt(ciphertext));
    }

    // --- Invalid config tests ---

    @Test
    void constructor_invalid_key_length_throws() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm4Config sm4 = new CryptoProperties.Sm4Config();
        sm4.setSecretKey("0123456789abcdef01"); // 34 hex chars = 17 bytes, not 16
        sm4.setMode("CBC");
        sm4.setPadding("PKCS5Padding");
        props.setSm4(sm4);
        assertThrows(IllegalStateException.class, () -> new Sm4CryptoService(props));
    }

    // --- ECB mode tests (no IV) ---

    @Test
    void ecb_encrypt_no_iv_parameter_uses_no_iv() {
        String plaintext = "no iv needed for ecb";
        String ciphertext = ecbService.encrypt(plaintext);
        assertNotNull(ciphertext);
        assertEquals(plaintext, ecbService.decrypt(ciphertext));
    }

    // --- Invalid config tests ---

    @Test
    void constructor_invalid_mode_throws_illegal_state() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm4Config sm4 = new CryptoProperties.Sm4Config();
        sm4.setSecretKey(TEST_KEY);
        sm4.setMode("INVALID");
        sm4.setPadding("PKCS5Padding");
        props.setSm4(sm4);
        assertThrows(IllegalStateException.class, () -> new Sm4CryptoService(props));
    }

    @Test
    void ecb_encrypt_decrypt_with_special_characters() {
        String plaintext = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String ciphertext = ecbService.encrypt(plaintext);
        assertEquals(plaintext, ecbService.decrypt(ciphertext));
    }
}
