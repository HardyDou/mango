package io.mango.notice.channel.sms;

record SmsGatewayResponse(boolean success, String messageId, String responseSnapshot, String failCode,
                          String failReason, boolean retryable) {

    static SmsGatewayResponse success(String messageId, String responseSnapshot) {
        return new SmsGatewayResponse(true, messageId, responseSnapshot, null, null, false);
    }

    static SmsGatewayResponse failed(String failCode, String failReason, boolean retryable, String responseSnapshot) {
        return new SmsGatewayResponse(false, null, responseSnapshot, failCode, failReason, retryable);
    }
}
