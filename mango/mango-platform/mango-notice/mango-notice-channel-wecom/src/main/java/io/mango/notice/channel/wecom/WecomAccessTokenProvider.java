package io.mango.notice.channel.wecom;

public interface WecomAccessTokenProvider {

    String getAccessToken(String corpId, String corpSecret);
}
