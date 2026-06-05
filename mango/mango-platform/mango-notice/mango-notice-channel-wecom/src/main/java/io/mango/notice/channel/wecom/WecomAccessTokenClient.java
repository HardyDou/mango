package io.mango.notice.channel.wecom;

public interface WecomAccessTokenClient {

    WecomAccessToken fetch(String corpId, String corpSecret);
}
