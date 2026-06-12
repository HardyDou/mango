package io.mango.infra.crypto.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 基础密码能力配置。
 * <p>
 * 配置前缀：mango.crypto。当前自动配置只覆盖 SM2/SM3/SM4 的基础能力。
 */
@ConfigurationProperties(prefix = "mango.crypto")
public class CryptoProperties {

    private static final String LEGACY_SM2_SAMPLE_AS_SM4_KEY = "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgA=";

    /**
     * 是否启用 crypto 自动配置，默认启用。
     */
    private boolean enabled = true;

    /**
     * SM4 配置。
     */
    private Sm4Config sm4 = new Sm4Config();

    /**
     * SM2 配置。
     */
    private Sm2Config sm2 = new Sm2Config();

    /**
     * 兼容旧配置 {@code mango.crypto.sm4-key}。
     */
    private String legacySm4Key;

    /**
     * 兼容旧配置 {@code mango.crypto.sm4-iv}。
     * 当前 SM4 CBC 实现使用密文前置 IV，不再读取固定 IV。
     */
    private String legacySm4Iv;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Sm4Config getSm4() {
        return sm4;
    }

    public void setSm4(Sm4Config sm4) {
        this.sm4 = sm4;
    }

    public Sm2Config getSm2() {
        return sm2;
    }

    public void setSm2(Sm2Config sm2) {
        this.sm2 = sm2;
    }

    public String getSm4Key() {
        return legacySm4Key;
    }

    public void setSm4Key(String sm4Key) {
        this.legacySm4Key = sm4Key;
        if (this.sm4.getSecretKey() == null || this.sm4.getSecretKey().isBlank()) {
            if (LEGACY_SM2_SAMPLE_AS_SM4_KEY.equals(sm4Key)) {
                throw new IllegalStateException(
                        "历史配置 mango.crypto.sm4-key 使用了 SM2 示例值，不能作为 SM4 密钥。"
                                + "请迁移为 mango.crypto.sm4.secret-key，并配置 16 字节 SM4 密钥，"
                                + "例如 00112233445566778899aabbccddeeff。");
            }
            this.sm4.setSecretKey(sm4Key);
        }
    }

    public String getSm4Iv() {
        return legacySm4Iv;
    }

    public void setSm4Iv(String sm4Iv) {
        this.legacySm4Iv = sm4Iv;
    }

    /**
     * SM4 对称加密配置。
     */
    public static class Sm4Config {
        /**
         * SM4 密钥，支持 Base64 或十六进制编码。
         * 128 位密钥为 16 字节。
         */
        private String secretKey;

        /**
         * 加密模式：CBC 或 ECB，默认 CBC。
         */
        private String mode = "CBC";

        /**
         * 填充模式：PKCS5Padding、PKCS7Padding、ZeroPadding、NoPadding，默认 PKCS5Padding。
         */
        private String padding = "PKCS5Padding";

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getPadding() {
            return padding;
        }

        public void setPadding(String padding) {
            this.padding = padding;
        }
    }

    /**
     * SM2 签名配置。
     */
    public static class Sm2Config {
        /**
         * SM2 私钥，Base64 编码的 PKCS#8 格式，用于签名。
         */
        private String privateKey;

        /**
         * SM2 公钥，Base64 编码，用于验签。
         */
        private String publicKey;

        /**
         * SM2 签名用户 ID，默认国密示例值 1234567812345678。
         */
        private String userId = "1234567812345678";

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
