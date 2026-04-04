package io.mango.auth.starter.controller;

import io.mango.auth.api.anti.AntiApi;
import io.mango.auth.core.anti.AntiService;
import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Anti service controller - signature and security endpoints
 */
@RestController
@RequestMapping("/auth/anti")
@RequiredArgsConstructor
public class AntiController {

    private final AntiApi antiApi;

    @GetMapping("/signature/key")
    @Perm("auth:anti:signature:key")
    public R<String> getSignatureKey() {
        return antiApi.getSignatureKey(null);
    }

    @PostMapping("/verify")
    @Perm("auth:anti:verify")
    public R<Boolean> verifySignature(
            @RequestParam String algorithm,
            @RequestParam String appKey,
            @RequestParam String timestamp,
            @RequestParam String sign,
            @RequestParam(required = false) String body) {
        return antiApi.verifySignature(algorithm, appKey, timestamp, sign, body);
    }
}
