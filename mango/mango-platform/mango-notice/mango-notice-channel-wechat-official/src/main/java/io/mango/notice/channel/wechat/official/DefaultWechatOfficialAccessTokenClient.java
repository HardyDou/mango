package io.mango.notice.channel.wechat.official;

import org.springframework.stereotype.Component;

@Component
public class DefaultWechatOfficialAccessTokenClient implements WechatOfficialAccessTokenClient {

    @Override
    public WechatOfficialAccessToken fetch(String appId, String appSecret) {
        return new WechatOfficialAccessToken("wechat-token-" + appId, 7200);
    }
}
