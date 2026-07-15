package io.mango.infra.crypto.fixture;

import io.mango.common.contract.LocalCapabilityContract;

@LocalCapabilityContract
public interface LocalCryptoService {
    String encrypt(String plaintext);
}
