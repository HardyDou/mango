package io.mango.payment.core.service;

import java.util.Optional;

/**
 * Resolves external channel bill server credentials by reference.
 */
public interface PaymentChannelBillCredentialProvider {

    Optional<Credential> resolve(String credentialRef);

    record Credential(String username, String password) {
    }
}
