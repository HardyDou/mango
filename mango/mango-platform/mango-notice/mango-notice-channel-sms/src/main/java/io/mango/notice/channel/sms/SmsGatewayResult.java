package io.mango.notice.channel.sms;

final class SmsGatewayResult {

    private final boolean success;
    private final String messageId;
    private final String responseSnapshot;
    private final String failCode;
    private final String failReason;
    private final boolean retryable;

    private SmsGatewayResult(boolean success, String messageId, String responseSnapshot, String failCode,
            String failReason, boolean retryable) {
        this.success = success;
        this.messageId = messageId;
        this.responseSnapshot = responseSnapshot;
        this.failCode = failCode;
        this.failReason = failReason;
        this.retryable = retryable;
    }

    static SmsGatewayResult success(String messageId, String responseSnapshot) {
        return new SmsGatewayResult(true, messageId, responseSnapshot, null, null, false);
    }

    static SmsGatewayResult failed(String failCode, String failReason, boolean retryable, String responseSnapshot) {
        return new SmsGatewayResult(false, null, responseSnapshot, failCode, failReason, retryable);
    }

    boolean success() {
        return success;
    }

    String messageId() {
        return messageId;
    }

    String responseSnapshot() {
        return responseSnapshot;
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
