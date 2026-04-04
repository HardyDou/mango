package io.mango.auth.core.anti;

import io.mango.auth.api.anti.AntiApi;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Anti service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiService implements AntiApi {

    private final SignatureValidator signatureValidator;

    @Override
    public R<String> getSignatureKey(Long userId) {
        // In a real implementation, this would retrieve the user's
        // stored SM2/RSA public key from sys_user.sign_public_key
        return R.ok("");
    }

    @Override
    public R<Boolean> verifySignature(String algorithm, String appKey,
                                       String timestamp, String sign, String body) {
        // This is a debug endpoint - in production, signature is validated
        // by AntiReplayInterceptor before reaching controllers
        return R.ok(false);
    }
}
