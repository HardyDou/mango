package io.mango.notice.channel.wecom;

import lombok.Getter;

@Getter
public class WecomApiException extends RuntimeException {

    private final String failCode;
    private final String failReason;
    private final boolean retryable;

    public WecomApiException(String failCode, String failReason, boolean retryable) {
        super(failReason);
        this.failCode = failCode;
        this.failReason = failReason;
        this.retryable = retryable;
    }
}
