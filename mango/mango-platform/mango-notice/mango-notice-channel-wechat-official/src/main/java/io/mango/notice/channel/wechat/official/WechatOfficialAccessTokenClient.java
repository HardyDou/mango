package io.mango.notice.channel.wechat.official;

public interface WechatOfficialAccessTokenClient {

    WechatOfficialAccessToken fetch(String appId, String appSecret);
}
