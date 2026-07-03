package io.mango.notice.channel.sms;

class SmsGatewayException extends RuntimeException {

    private final String failCode;
    private final String failReason;
    private final boolean retryable;

    SmsGatewayException(String failCode, String failReason, boolean retryable) {
        super(failReason);
        this.failCode = failCode;
        this.failReason = failReason;
        this.retryable = retryable;
    }

    String failCode() {
        return failCode;
    }

    String failReason() {
        return failReason;
    }

    boolean retryable() {
        return retryable;
    }
}
