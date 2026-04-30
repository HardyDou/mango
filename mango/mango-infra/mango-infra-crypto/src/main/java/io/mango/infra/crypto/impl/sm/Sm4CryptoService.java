package io.mango.infra.crypto.impl.sm;

import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.crypto.starter.CryptoProperties;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * SM4 对称加解密基础实现。
 * <p>
 * 支持 ECB 和 CBC 模式。CBC 模式会把 16 字节 IV 前置到密文中，便于自包含解密。
 */
public class Sm4CryptoService implements ICryptoService {

    private static final Logger log = LoggerFactory.getLogger(Sm4CryptoService.class);
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
            throw new IllegalStateException("SM4 mode 只能是 CBC 或 ECB，当前值：" + mode);
        }
        validatePadding();
        validateSecretKey();
    }

    private void validatePadding() {
        String padding = config.getPadding();
        if (padding == null) {
            throw new IllegalStateException("SM4 padding 不能为空");
        }
        String upper = padding.toUpperCase();
        if (!upper.equals("PKCS5PADDING") && !upper.equals("PKCS7PADDING")
                && !upper.equals("ZEROPADDING") && !upper.equals("NOPADDING")) {
            throw new IllegalStateException(
                    "SM4 padding 只能是 PKCS5Padding、PKCS7Padding、ZeroPadding、NoPadding，当前值：" + padding);
        }
    }

    private void validateSecretKey() {
        byte[] keyBytes = decodeKey(config.getSecretKey());
        if (keyBytes == null || keyBytes.length != 16) {
            throw new IllegalStateException(
                    "SM4 secretKey 必须是 16 字节（128 位），当前长度：" +
                    (keyBytes == null ? "null" : keyBytes.length + " 字节"));
        }
    }

    @Override
    public String encrypt(String plaintext) {
        return encrypt(plaintext, (String) null);
    }

    @Override
    public String encrypt(String plaintext, String iv) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext 不能为空");
        }
        try {
            boolean isCbc = "CBC".equalsIgnoreCase(config.getMode());
            boolean isEcb = "ECB".equalsIgnoreCase(config.getMode());
            byte[] ivBytes;

            if (isEcb) {
                log.warn("SM4 ECB 模式不安全，只应在测试或非敏感数据场景使用");
                ivBytes = null;
            } else if (isCbc) {
                if (iv != null) {
                    ivBytes = decodeKey(iv);
                    validateIvLength(ivBytes);
                } else {
                    ivBytes = generateIv();
                }
            } else {
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
                ByteBuffer buffer = ByteBuffer.allocate(IV_SIZE + encrypted.length);
                buffer.put(ivBytes);
                buffer.put(encrypted);
                return Base64.toBase64String(buffer.array());
            } else {
                return Base64.toBase64String(encrypted);
            }
        } catch (Exception e) {
            throw new RuntimeException("SM4 加密失败", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        return decrypt(ciphertext, (String) null);
    }

    @Override
    public String decrypt(String ciphertext, String iv) {
        if (ciphertext == null) {
            throw new IllegalArgumentException("ciphertext 不能为空");
        }
        try {
            boolean isCbc = "CBC".equalsIgnoreCase(config.getMode());

            if (isCbc) {
                byte[] decoded = Base64.decode(ciphertext);
                if (decoded.length < IV_SIZE) {
                    throw new IllegalArgumentException(
                            "CBC 模式密文长度过短，至少需要 " + (IV_SIZE + 1) + " 字节");
                }
                byte[] ivBytes = new byte[IV_SIZE];
                byte[] actualCiphertext = new byte[decoded.length - IV_SIZE];
                ByteBuffer buffer = ByteBuffer.wrap(decoded);
                buffer.get(ivBytes);
                buffer.get(actualCiphertext);
                ciphertext = Base64.toBase64String(actualCiphertext);

                SecretKeySpec keySpec = new SecretKeySpec(decodeKey(config.getSecretKey()), ALGORITHM);
                Cipher cipher = Cipher.getInstance(buildTransformation(), BouncyCastleLoader.PROVIDER_NAME);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ivBytes));
                byte[] decrypted = cipher.doFinal(Base64.decode(ciphertext));
                return new String(decrypted, StandardCharsets.UTF_8);
            } else {
                SecretKeySpec keySpec = new SecretKeySpec(decodeKey(config.getSecretKey()), ALGORITHM);
                Cipher cipher = Cipher.getInstance(buildTransformation(), BouncyCastleLoader.PROVIDER_NAME);
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                byte[] decrypted = cipher.doFinal(Base64.decode(ciphertext));
                return new String(decrypted, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("SM4 解密失败", e);
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
            throw new IllegalArgumentException("key 不能为空");
        }
        if (isHexString(key)) {
            try {
                return Hex.decode(key);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "key 不是有效十六进制：" +
                        (key.length() <= 64 ? key : key.substring(0, 64) + "..."), e);
            }
        }
        try {
            return Base64.decode(key);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "key 既不是有效 Base64，也不是有效十六进制：" +
                    (key.length() <= 64 ? key : key.substring(0, 64) + "..."), e);
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
                    "SM4/CBC 的 IV 必须是 " + IV_SIZE + " 字节，当前长度：" +
                    (iv == null ? "null" : iv.length + " 字节"));
        }
    }
}
