package io.mango.notice.channel.email;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailNoticeChannelSenderTest {

    private final EmailNoticeChannelSender sender = new EmailNoticeChannelSender();

    @Test
    void channelType_returnsEmail() {
        assertEquals(NoticeChannelType.EMAIL, sender.channelType());
    }

    @Test
    void send_missingEmail_returnsNonRetryableFailure() {
        ChannelSendResult result = sender.send(new ChannelSendCommand());

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.RECIPIENT_INVALID.name(), result.getFailCode());
        assertEquals("邮箱地址不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_validEmailWithHtmlAndAttachmentFileIds_returnsProviderSuccess() {
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(2001L);
        command.setEmail("user@example.com");
        command.setContent("<p>订单 SO-1001 已发货</p>");
        command.setAttachmentFileIds(List.of(1001L, 1002L));
        command.setChannelConfigJson("{\"host\":\"smtp.example.com\",\"username\":\"u\",\"password\":\"p\",\"from\":\"noreply@example.com\"}");

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("email-2001", result.getProviderMessageId());
        assertEquals("{\"status\":\"SENT\"}", result.getResponseSnapshot());
        assertEquals(List.of(1001L, 1002L), command.getAttachmentFileIds());
    }
}
