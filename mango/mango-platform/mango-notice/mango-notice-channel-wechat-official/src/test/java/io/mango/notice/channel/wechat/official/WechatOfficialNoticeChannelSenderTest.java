package io.mango.notice.channel.wechat.official;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatOfficialNoticeChannelSenderTest {

    @Test
    void channelType_returnsWechatOfficial() {
        WechatOfficialNoticeChannelSender sender = new WechatOfficialNoticeChannelSender(tokenProvider());

        assertEquals(NoticeChannelType.WECHAT_OFFICIAL, sender.channelType());
    }

    @Test
    void send_missingOpenid_returnsNonRetryableFailure() {
        WechatOfficialNoticeChannelSender sender = new WechatOfficialNoticeChannelSender(tokenProvider());

        ChannelSendResult result = sender.send(new ChannelSendCommand());

        assertFalse(result.isSuccess());
        assertEquals("OPENID_EMPTY", result.getFailCode());
        assertEquals("微信公众号 openid 不能为空", result.getFailReason());
    }

    @Test
    void send_validTemplate_usesCachedAccessTokenAndVariableMapping() {
        AtomicInteger fetchCount = new AtomicInteger();
        InMemoryWechatOfficialAccessTokenProvider provider = new InMemoryWechatOfficialAccessTokenProvider(
                (appId, appSecret) -> {
                    fetchCount.incrementAndGet();
                    return new WechatOfficialAccessToken("token-" + appId, 7200);
                },
                Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC));
        WechatOfficialNoticeChannelSender sender = new WechatOfficialNoticeChannelSender(provider);
        ChannelSendCommand command = command();

        ChannelSendResult first = sender.send(command);
        ChannelSendResult second = sender.send(command);

        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());
        assertEquals("wechat-3001", first.getProviderMessageId());
        assertEquals("{\"status\":\"ACCEPTED\",\"token\":\"token-wx-app\"}", first.getResponseSnapshot());
        assertEquals(1, fetchCount.get());
    }

    private WechatOfficialAccessTokenProvider tokenProvider() {
        return (appId, appSecret) -> "token-" + appId;
    }

    private ChannelSendCommand command() {
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(3001L);
        command.setWechatOpenid("openid-1");
        command.setChannelConfigJson("{\"appId\":\"wx-app\",\"appSecret\":\"secret\"}");
        command.setChannelTemplateId("TPL_10001");
        command.setVariableMapping("{\"orderNo\":\"thing1\"}");
        return command;
    }
}
