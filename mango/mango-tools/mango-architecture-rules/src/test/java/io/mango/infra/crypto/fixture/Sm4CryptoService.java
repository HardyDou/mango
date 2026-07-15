package io.mango.infra.crypto.fixture;

import io.mango.common.contract.LocalCapabilityContract;

@LocalCapabilityContract
public final class Sm4CryptoService implements LocalCryptoService {
    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext is required");
        }
        return plaintext;
    }
}
