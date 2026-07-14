package io.mango.notice.channel.wecom;

public final class WecomSendResult {

    private final String rawResponse;
    private final String messageId;

    public WecomSendResult(String rawResponse, String messageId) {
        this.rawResponse = rawResponse;
        this.messageId = messageId;
    }

    public String rawResponse() {
        return rawResponse;
    }

    public String messageId() {
        return messageId;
    }
}
