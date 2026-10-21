package io.mango.information.starter.security;

import io.mango.information.api.enums.InformationCode;
import lombok.Getter;

/** 公开提交防刷拒绝结果，交由 HTTP 异常适配器映射为响应。 */
@Getter
public class InformationSubmitProtectionException extends RuntimeException {

    private final int status;
    private final InformationCode code;
    private final long retryAfterSeconds;

    public InformationSubmitProtectionException(InformationSubmitProtection.Decision decision) {
        super(decision.message());
        this.status = decision.status();
        this.code = decision.code();
        this.retryAfterSeconds = decision.retryAfterSeconds();
    }
}
