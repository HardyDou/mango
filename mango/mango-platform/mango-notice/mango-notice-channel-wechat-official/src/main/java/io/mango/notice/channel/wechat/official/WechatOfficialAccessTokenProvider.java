package io.mango.notice.channel.wechat.official;

public interface WechatOfficialAccessTokenProvider {

    String getAccessToken(String appId, String appSecret);
}
