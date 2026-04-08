package io.mango.infra.crypto.impl.sm;

import io.mango.infra.crypto.starter.CryptoProperties;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Sm2SignService.
 *
 * Test keys are pre-generated SM2 keypairs in PKCS8 (private) and
 * SubjectPublicKeyInfo (public) format, generated via OpenSSL:
 *   openssl ecparam -name SM2 -genkey -noout -outform PEM
 *   openssl ec -pubout -outform DER
 */
class Sm2SignServiceTest {

    // PKCS8-encoded SM2 private key (Base64 of DER) - BC-generated via ECGenParameterSpec("sm2p256v1")
    private static final String TEST_PRIVATE_KEY =
            "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgkTjxMZICO/dGjUA4amgE3GcKrX//t+FE+Qixo/c94v6gCgYIKoEcz1UBgi2hRANCAAQIiMjCY9xAdHHdFzYuREvfpKDC+P2E+ywZhszVwZWmsPBgtLH9Ta2WZW514NXH6PQV410ZPxsBoFiT/KhE/+/n";

    // SubjectPublicKeyInfo-encoded SM2 public key (Base64 of DER)
    private static final String TEST_PUBLIC_KEY =
            "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAECIjIwmPcQHRx3Rc2LkRL36Sgwvj9hPssGYbM1cGVprDwYLSx/U2tlmVudeDVx+j0FeNdGT8bAaBYk/yoRP/v5w==";

    // Invalid format key for decodeKey error path testing
    private static final String INVALID_KEY = "not-valid-base64-nor-hex!!!";

    @BeforeEach
    void setUp() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm2Config sm2 = new CryptoProperties.Sm2Config();
        sm2.setPrivateKey(TEST_PRIVATE_KEY);
        sm2.setPublicKey(TEST_PUBLIC_KEY);
        sm2.setUserId("1234567812345678");
        props.setSm2(sm2);
        signServiceUnderTest = new Sm2SignService(props);
    }

    private Sm2SignService signServiceUnderTest;

    // --- Sign/Verify roundtrip tests ---

    @Test
    void sign_verify_roundtrip_returns_true() {
        String data = "Hello, SM2 signature!";
        String signature = signServiceUnderTest.sign(data);
        assertNotNull(signature);
        assertTrue(signServiceUnderTest.verify(data, signature));
    }

    @Test
    void verify_tampered_data_returns_false() {
        String data = "original data";
        String signature = signServiceUnderTest.sign(data);
        String tamperedData = data + "tampered";
        assertFalse(signServiceUnderTest.verify(tamperedData, signature));
    }

    @Test
    void verify_wrong_signature_returns_false() {
        String data = "some data";
        String wrongSignature = Base64.toBase64String(new byte[70]);
        assertFalse(signServiceUnderTest.verify(data, wrongSignature));
    }

    @Test
    void sign_verify_chinese_characters() {
        String data = "中文签名测试 data 123 !@#";
        String signature = signServiceUnderTest.sign(data);
        assertTrue(signServiceUnderTest.verify(data, signature));
    }

    @Test
    void sign_verify_long_text() {
        String data = "A".repeat(1000);
        String signature = signServiceUnderTest.sign(data);
        assertTrue(signServiceUnderTest.verify(data, signature));
    }

    // --- Null/input validation tests ---

    @Test
    void sign_null_data_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class, () -> signServiceUnderTest.sign(null));
    }

    @Test
    void verify_null_data_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class, () -> signServiceUnderTest.verify(null, "sig"));
    }

    @Test
    void verify_null_signature_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class, () -> signServiceUnderTest.verify("data", null));
    }

    // --- Constructor validation tests ---

    @Test
    void constructor_null_userId_throws_illegal_state() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm2Config sm2 = new CryptoProperties.Sm2Config();
        sm2.setPrivateKey(TEST_PRIVATE_KEY);
        sm2.setPublicKey(TEST_PUBLIC_KEY);
        sm2.setUserId(null);
        props.setSm2(sm2);
        assertThrows(IllegalStateException.class, () -> new Sm2SignService(props));
    }

    @Test
    void constructor_empty_userId_throws_illegal_state() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm2Config sm2 = new CryptoProperties.Sm2Config();
        sm2.setPrivateKey(TEST_PRIVATE_KEY);
        sm2.setPublicKey(TEST_PUBLIC_KEY);
        sm2.setUserId("");
        props.setSm2(sm2);
        assertThrows(IllegalStateException.class, () -> new Sm2SignService(props));
    }

    @Test
    void constructor_null_privateKey_throws_illegal_state() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm2Config sm2 = new CryptoProperties.Sm2Config();
        sm2.setPrivateKey(null);
        sm2.setPublicKey(TEST_PUBLIC_KEY);
        sm2.setUserId("1234567812345678");
        props.setSm2(sm2);
        assertThrows(IllegalStateException.class, () -> new Sm2SignService(props));
    }

    @Test
    void constructor_null_publicKey_throws_illegal_state() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm2Config sm2 = new CryptoProperties.Sm2Config();
        sm2.setPrivateKey(TEST_PRIVATE_KEY);
        sm2.setPublicKey(null);
        sm2.setUserId("1234567812345678");
        props.setSm2(sm2);
        assertThrows(IllegalStateException.class, () -> new Sm2SignService(props));
    }

    // --- decodeKey error path tests ---

    @Test
    void decodeKey_invalid_key_throws_illegal_argument() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Sm2Config sm2 = new CryptoProperties.Sm2Config();
        sm2.setPrivateKey(INVALID_KEY);
        sm2.setPublicKey(TEST_PUBLIC_KEY);
        sm2.setUserId("1234567812345678");
        props.setSm2(sm2);
        Sm2SignService service = new Sm2SignService(props);
        // decodeKey IllegalArgumentException is wrapped in RuntimeException by sign()
        assertThrows(RuntimeException.class, () -> service.sign("data"));
    }
}
