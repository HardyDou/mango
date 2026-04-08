package io.mango.infra.crypto.impl.sm;

import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.crypto.starter.CryptoProperties;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * SM4 symmetric encryption/decryption service implementation.
 * Supports ECB and CBC modes with configurable padding.
 * CBC mode prepends the 16-byte IV to ciphertext for self-contained decryption.
 */
public class Sm4CryptoService implements ICryptoService {

    private static final String ALGORITHM = "SM4";
    private static final int IV_SIZE = 16;

    private final CryptoProperties.Sm4Config config;

    static {
        BouncyCastleLoader.ensure();
    }

    public Sm4CryptoService(CryptoProperties properties) {
        this.config = properties.getSm4();
        validateConfig();
    }

    private void validateConfig() {
        String mode = config.getMode();
        if (mode == null || (!mode.equalsIgnoreCase("CBC") && !mode.equalsIgnoreCase("ECB"))) {
            throw new IllegalStateException("SM4 mode must be CBC or ECB, got: " + mode);
        }
    }

    @Override
    public String encrypt(String plaintext) {
        return encrypt(plaintext, (String) null);
    }

    @Override
    public String encrypt(String plaintext, String iv) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext cannot be null");
        }
        try {
            boolean isCbc = "CBC".equalsIgnoreCase(config.getMode());
            byte[] ivBytes;

            if (isCbc) {
                if (iv != null) {
                    ivBytes = decodeKey(iv);
                    validateIvLength(ivBytes);
                } else {
                    // Auto-generate IV and prepend to ciphertext
                    ivBytes = generateIv();
                }
            } else {
                // ECB mode: no IV needed
                ivBytes = null;
            }

            SecretKeySpec keySpec = new SecretKeySpec(decodeKey(config.getSecretKey()), ALGORITHM);
            String transformation = buildTransformation();

            Cipher cipher = Cipher.getInstance(transformation, BouncyCastleLoader.PROVIDER_NAME);
            if (isCbc) {
                IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            }

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            if (isCbc) {
                // Prepend IV to ciphertext: [IV(16 bytes)][ciphertext]
                ByteBuffer buffer = ByteBuffer.allocate(IV_SIZE + encrypted.length);
                buffer.put(ivBytes);
                buffer.put(encrypted);
                return Base64.toBase64String(buffer.array());
            } else {
                return Base64.toBase64String(encrypted);
            }
        } catch (Exception e) {
            throw new RuntimeException("SM4 encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        return decrypt(ciphertext, (String) null);
    }

    @Override
    public String decrypt(String ciphertext, String iv) {
        if (ciphertext == null) {
            throw new IllegalArgumentException("ciphertext cannot be null");
        }
        try {
            boolean isCbc = "CBC".equalsIgnoreCase(config.getMode());
            byte[] ivBytes = null;

            if (isCbc) {
                // Extract IV from the first 16 bytes of ciphertext
                byte[] decoded = Base64.decode(ciphertext);
                if (decoded.length < IV_SIZE) {
                    throw new IllegalArgumentException("ciphertext is too short, expected at least " + (IV_SIZE + 1) + " bytes for CBC mode");
                }
                ivBytes = new byte[IV_SIZE];
                byte[] actualCiphertext = new byte[decoded.length - IV_SIZE];
                ByteBuffer buffer = ByteBuffer.wrap(decoded);
                buffer.get(ivBytes);
                buffer.get(actualCiphertext);
                ciphertext = Base64.toBase64String(actualCiphertext);
            }

            SecretKeySpec keySpec = new SecretKeySpec(decodeKey(config.getSecretKey()), ALGORITHM);
            String transformation = buildTransformation();

            Cipher cipher = Cipher.getInstance(transformation, BouncyCastleLoader.PROVIDER_NAME);
            if (isCbc) {
                IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
            }

            byte[] decrypted = cipher.doFinal(Base64.decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4 decryption failed", e);
        }
    }

    private String buildTransformation() {
        String mode = config.getMode().toUpperCase();
        String padding = config.getPadding();
        return ALGORITHM + "/" + mode + "/" + padding;
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private byte[] decodeKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        // Detect encoding: hex strings contain only 0-9a-fA-F and have even length.
        // If valid hex, decode as hex. Otherwise try Base64.
        if (isHexString(key)) {
            try {
                return Hex.decode(key);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Key is not valid hex: " +
                        (key.length() <= 64 ? key : key.substring(0, 64) + "..."), e);
            }
        }
        // Try Base64
        try {
            return Base64.decode(key);
        } catch (Exception e) {
            // Not Base64 either, try Hex as last resort
            try {
                return Hex.decode(key);
            } catch (Exception hexEx) {
                throw new IllegalArgumentException(
                        "Key is neither valid Base64 nor Hex: " +
                        (key.length() <= 64 ? key : key.substring(0, 64) + "..."), hexEx);
            }
        }
    }

    private static boolean isHexString(String s) {
        if (s.length() % 2 != 0) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private void validateIvLength(byte[] iv) {
        if (iv == null || iv.length != IV_SIZE) {
            throw new IllegalArgumentException(
                    "IV must be " + IV_SIZE + " bytes for SM4/CBC, got: " +
                    (iv == null ? "null" : iv.length + " bytes"));
        }
    }
}
