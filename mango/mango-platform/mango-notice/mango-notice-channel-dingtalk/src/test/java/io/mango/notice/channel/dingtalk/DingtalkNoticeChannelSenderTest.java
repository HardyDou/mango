package io.mango.notice.channel.dingtalk;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.NoticeChannelMessage;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DingtalkNoticeChannelSenderTest {

    private final DingtalkNoticeChannelSender sender = new DingtalkNoticeChannelSender();

    @Test
    void channelType_returnsDingtalk() {
        assertEquals(NoticeChannelType.DINGTALK, sender.channelType());
    }

    @Test
    void send_missingUserAndRobot_returnsNonRetryableFailure() {
        ChannelSendResult result = sender.send(new NoticeChannelMessage());

        assertFalse(result.isSuccess());
        assertEquals("DINGTALK_USER_EMPTY", result.getFailCode());
        assertEquals("钉钉用户 ID 不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_withUserId_returnsWorkNoticeSuccess() {
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setSendRecordId(5001L);
        command.setDingtalkUserId("manager001");

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("dingtalk-work-5001", result.getProviderMessageId());
    }

    @Test
    void send_withRobotWebhook_returnsRobotMessageSuccessWithoutUserId() {
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setSendRecordId(5002L);
        command.setChannelConfigJson("{\"webhookUrl\":\"https://oapi.dingtalk.com/robot/send?access_token=test\"}");

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("dingtalk-robot-5002", result.getProviderMessageId());
    }
}
