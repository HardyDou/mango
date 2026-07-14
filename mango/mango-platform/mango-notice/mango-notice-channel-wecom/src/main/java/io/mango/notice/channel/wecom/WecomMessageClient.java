package io.mango.notice.channel.wecom;

public interface WecomMessageClient {

    WecomSendResult sendText(String accessToken, WecomTextMessage message);
}
