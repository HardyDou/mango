package io.mango.infra.crypto.impl.sm;

import io.mango.infra.crypto.starter.CryptoProperties;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

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

    // Invalid non-secret test input for decodeKey error path testing.
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
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.sign("data"));
        String messages = exceptionChain(exception);
        assertFalse(messages.contains(INVALID_KEY));
        assertFalse(messages.contains("not-valid-base64"));
    }

    @Test
    void sign_output_preservesBase64DerSignatureContract() throws Exception {
        byte[] signature = Base64.decode(signServiceUnderTest.sign("DER contract"));
        try (org.bouncycastle.asn1.ASN1InputStream input =
                     new org.bouncycastle.asn1.ASN1InputStream(signature)) {
            org.bouncycastle.asn1.ASN1Sequence sequence =
                    org.bouncycastle.asn1.ASN1Sequence.getInstance(input.readObject());
            assertEquals(2, sequence.size());
            assertInstanceOf(org.bouncycastle.asn1.ASN1Integer.class, sequence.getObjectAt(0));
            assertInstanceOf(org.bouncycastle.asn1.ASN1Integer.class, sequence.getObjectAt(1));
            assertNull(input.readObject());
        }
    }

    @Test
    void verify_matchesBouncyCastleSm2KnownSignatureVector() throws Exception {
        BigInteger privateScalar = new BigInteger(
                "110E7973206F68C19EE5F7328C036F26911C8C73B4E4F36AE3291097F8984FFC", 16);
        org.bouncycastle.asn1.x9.X9ECParameters curve =
                org.bouncycastle.asn1.gm.GMNamedCurves.getByName("sm2p256v1");
        org.bouncycastle.jce.spec.ECParameterSpec parameters =
                org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("sm2p256v1");
        org.bouncycastle.jce.spec.ECPublicKeySpec publicKeySpec =
                new org.bouncycastle.jce.spec.ECPublicKeySpec(
                        curve.getG().multiply(privateScalar).normalize(), parameters);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(
                "EC", BouncyCastleLoader.PROVIDER_NAME);
        String publicKey = Base64.toBase64String(keyFactory.generatePublic(publicKeySpec).getEncoded());

        org.bouncycastle.asn1.ASN1EncodableVector signatureValues =
                new org.bouncycastle.asn1.ASN1EncodableVector();
        signatureValues.add(new org.bouncycastle.asn1.ASN1Integer(new BigInteger(
                "05890B9077B92E47B17A1FF42A814280E556AFD92B4A98B9670BF8B1A274C2FA", 16)));
        signatureValues.add(new org.bouncycastle.asn1.ASN1Integer(new BigInteger(
                "E3ABBB8DB2B6ECD9B24ECCEA7F679FB9A4B1DB52F4AA985E443AD73237FA1993", 16)));
        String signature = Base64.toBase64String(
                new org.bouncycastle.asn1.DERSequence(signatureValues).getEncoded());

        CryptoProperties properties = new CryptoProperties();
        CryptoProperties.Sm2Config sm2 = new CryptoProperties.Sm2Config();
        sm2.setPrivateKey(TEST_PRIVATE_KEY);
        sm2.setPublicKey(publicKey);
        sm2.setUserId("sm2test@example.com");
        properties.setSm2(sm2);

        assertTrue(new Sm2SignService(properties).verify("hi chappy", signature));
    }

    private String exceptionChain(Throwable throwable) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
