package io.mango.notice.channel.wecom;

public interface WecomMessageClient {

    WecomMessageSendResponse sendText(String accessToken, WecomTextMessageRequest request);
}
