package io.mango.notice.channel.sms;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsNoticeChannelSenderTest {

    private final SmsNoticeChannelSender sender = new SmsNoticeChannelSender();

    @Test
    void channelType_returnsSms() {
        assertEquals(NoticeChannelType.SMS, sender.channelType());
    }

    @Test
    void send_missingMobile_returnsNonRetryableFailure() {
        ChannelSendResult result = sender.send(new ChannelSendCommand());

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.RECIPIENT_INVALID.name(), result.getFailCode());
        assertEquals("手机号不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_validMobile_returnsProviderSuccess() {
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(1001L);
        command.setMobile("13800138000");
        command.setChannelConfigJson("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\",\"signName\":\"芒果\"}");

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("sms-1001", result.getProviderMessageId());
        assertEquals("{\"status\":\"ACCEPTED\"}", result.getResponseSnapshot());
    }
}
