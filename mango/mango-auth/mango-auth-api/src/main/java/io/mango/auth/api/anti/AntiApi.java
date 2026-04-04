package io.mango.auth.api.anti;

import io.mango.common.result.R;

/**
 * Anti service API - replay protection, idempotency, signature validation
 */
public interface AntiApi {

    /**
     * Get signature public key for current user
     */
    R<String> getSignatureKey(Long userId);

    /**
     * Verify signature (debug endpoint)
     */
    R<Boolean> verifySignature(String algorithm, String appKey, String timestamp, String sign, String body);
}
