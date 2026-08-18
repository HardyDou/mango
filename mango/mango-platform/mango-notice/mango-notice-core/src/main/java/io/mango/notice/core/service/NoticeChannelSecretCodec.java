package io.mango.notice.core.service;

import io.mango.common.result.Require;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.notice.api.enums.NoticeCode;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class NoticeChannelSecretCodec {
    static final String ENCRYPTED_PREFIX = "enc:";

    private final ObjectProvider<ICryptoService> cryptoServiceProvider;

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return null;
        }
        ICryptoService cryptoService =
                Require.nonNull(
                        cryptoServiceProvider.getIfAvailable(),
                        NoticeCode.NOTICE_CHANNEL_SECRET_INVALID,
                        "渠道 Secret 加密能力不可用");
        try {
            return ENCRYPTED_PREFIX + cryptoService.encrypt(plaintext);
        } catch (RuntimeException exception) {
            return Require.fail(
                    NoticeCode.NOTICE_CHANNEL_SECRET_INVALID,
                    "渠道 Secret 加密失败",
                    exception);
        }
    }

    public String decrypt(String ciphertext) {
        Require.isTrue(
                isEncrypted(ciphertext),
                NoticeCode.NOTICE_CHANNEL_SECRET_INVALID,
                "渠道 Secret 密文格式无效");
        ICryptoService cryptoService =
                Require.nonNull(
                        cryptoServiceProvider.getIfAvailable(),
                        NoticeCode.NOTICE_CHANNEL_SECRET_INVALID,
                        "渠道 Secret 解密能力不可用");
        try {
            return cryptoService.decrypt(ciphertext.substring(ENCRYPTED_PREFIX.length()));
        } catch (RuntimeException exception) {
            return Require.fail(
                    NoticeCode.NOTICE_CHANNEL_SECRET_INVALID,
                    "渠道 Secret 解密失败",
                    exception);
        }
    }

    public String decryptCompatible(String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return null;
        }
        return isEncrypted(storedValue) ? decrypt(storedValue) : storedValue;
    }

    public String encryptIfNecessary(String storedValue) {
        if (!StringUtils.hasText(storedValue) || isEncrypted(storedValue)) {
            return storedValue;
        }
        return encrypt(storedValue);
    }

    public boolean isEncrypted(String value) {
        return StringUtils.hasText(value) && value.startsWith(ENCRYPTED_PREFIX);
    }
}
