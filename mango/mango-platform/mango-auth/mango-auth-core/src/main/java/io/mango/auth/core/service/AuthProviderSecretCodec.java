package io.mango.auth.core.service;

import io.mango.auth.api.enums.AuthCode;
import io.mango.common.result.Require;
import io.mango.infra.crypto.impl.ICryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AuthProviderSecretCodec {

    private static final String ENCRYPTED_PREFIX = "enc:";

    private final ObjectProvider<ICryptoService> cryptoServiceProvider;

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return null;
        }
        ICryptoService cryptoService = Require.nonNull(cryptoServiceProvider.getIfAvailable(),
                AuthCode.PROVIDER_SECRET_INVALID);
        return ENCRYPTED_PREFIX + cryptoService.encrypt(plaintext.trim());
    }

    public String decrypt(String ciphertext) {
        Require.isTrue(StringUtils.hasText(ciphertext) && ciphertext.startsWith(ENCRYPTED_PREFIX),
                AuthCode.PROVIDER_SECRET_INVALID);
        ICryptoService cryptoService = Require.nonNull(cryptoServiceProvider.getIfAvailable(),
                AuthCode.PROVIDER_SECRET_INVALID);
        try {
            return cryptoService.decrypt(ciphertext.substring(ENCRYPTED_PREFIX.length()));
        } catch (RuntimeException exception) {
            return Require.fail(AuthCode.PROVIDER_SECRET_INVALID, AuthCode.PROVIDER_SECRET_INVALID.getMessage(),
                    exception);
        }
    }
}
