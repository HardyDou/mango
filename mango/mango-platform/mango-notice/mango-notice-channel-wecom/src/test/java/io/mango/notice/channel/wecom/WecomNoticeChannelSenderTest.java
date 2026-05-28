package io.mango.notice.channel.wecom;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WecomNoticeChannelSenderTest {

    private final WecomNoticeChannelSender sender = new WecomNoticeChannelSender();

    @Test
    void channelType_returnsWecom() {
        assertEquals(NoticeChannelType.WECOM, sender.channelType());
    }

    @Test
    void send_missingUserAndRobot_returnsNonRetryableFailure() {
        ChannelSendResult result = sender.send(new ChannelSendCommand());

        assertFalse(result.isSuccess());
        assertEquals("WECOM_USER_EMPTY", result.getFailCode());
        assertEquals("企业微信用户 ID 不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_withUserId_returnsAppMessageSuccess() {
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(4001L);
        command.setWecomUserId("zhangsan");

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("wecom-app-4001", result.getProviderMessageId());
    }

    @Test
    void send_withRobotWebhook_returnsRobotMessageSuccessWithoutUserId() {
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(4002L);
        command.setChannelConfigJson("{\"webhookUrl\":\"https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test\"}");

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("wecom-robot-4002", result.getProviderMessageId());
    }
}
