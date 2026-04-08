package io.mango.infra.crypto.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Crypto configuration properties.
 * Configuration prefix: mango.crypto
 */
@ConfigurationProperties(prefix = "mango.crypto")
public class CryptoProperties {

    /**
     * Whether to enable crypto module.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * SM4 configuration.
     */
    private Sm4Config sm4 = new Sm4Config();

    /**
     * SM2 configuration.
     */
    private Sm2Config sm2 = new Sm2Config();

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

    /**
     * SM4 symmetric encryption configuration.
     */
    public static class Sm4Config {
        /**
         * Secret key for SM4 (Base64 encoded).
         * 128-bit key = 16 bytes = 24 Base64 chars.
         */
        private String secretKey;

        /**
         * Cipher mode: CBC or ECB.
         * Default: CBC
         */
        private String mode = "CBC";

        /**
         * Padding mode: PKCS5Padding, PKCS7Padding, ZeroPadding, NoPadding.
         * Default: PKCS5Padding
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
     * SM2 asymmetric encryption configuration.
     */
    public static class Sm2Config {
        /**
         * SM2 private key (Base64 encoded, PKCS#8 format).
         * Used for signing.
         */
        private String privateKey;

        /**
         * SM2 public key (Base64 encoded).
         * Used for signature verification.
         */
        private String publicKey;

        /**
         * User ID for SM2 signature (default Chinese GM user ID).
         * Default: 1234567812345678
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
