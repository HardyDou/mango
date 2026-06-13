package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.payment.api.PaymentCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentSensitiveValueService {

    private static final String ENCRYPTED_PREFIX = "enc:";

    private final ICryptoService cryptoService;

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return null;
        }
        String trimmed = plaintext.trim();
        if (isEncrypted(trimmed)) {
            return trimmed;
        }
        return ENCRYPTED_PREFIX + cryptoService.encrypt(trimmed);
    }

    public String decrypt(String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return null;
        }
        String trimmed = storedValue.trim();
        if (!isEncrypted(trimmed)) {
            return trimmed;
        }
        try {
            return cryptoService.decrypt(trimmed.substring(ENCRYPTED_PREFIX.length()));
        } catch (RuntimeException ex) {
            throw new BizException(PaymentCode.PAYMENT_SENSITIVE_VALUE_INVALID.getCode(),
                    PaymentCode.PAYMENT_SENSITIVE_VALUE_INVALID.getMessage(), ex);
        }
    }

    public String mask(String storedValue, int prefixLength, int suffixLength) {
        String plaintext = decrypt(storedValue);
        if (!StringUtils.hasText(plaintext)) {
            return "****";
        }
        int safePrefix = Math.max(0, prefixLength);
        int safeSuffix = Math.max(0, suffixLength);
        if (plaintext.length() <= safePrefix + safeSuffix) {
            return "****";
        }
        return plaintext.substring(0, safePrefix) + "****" + plaintext.substring(plaintext.length() - safeSuffix);
    }

    public boolean isEncrypted(String value) {
        return StringUtils.hasText(value) && value.trim().startsWith(ENCRYPTED_PREFIX);
    }

    public String stableHash(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(plaintext.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BizException(PaymentCode.PAYMENT_SENSITIVE_VALUE_INVALID.getCode(),
                    PaymentCode.PAYMENT_SENSITIVE_VALUE_INVALID.getMessage(), ex);
        }
    }
}
