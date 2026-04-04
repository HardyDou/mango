package io.mango.auth.api.anti.enums;

public enum AntiTypeEnum {
    REPLAY,     // 防重放
    IDEMPOTENCY, // 防幂等
    SIGNATURE   // 签名验证
}
