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

    @Test
    void channelType_returnsEmail() {
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender();

        assertEquals(NoticeChannelType.EMAIL, sender.channelType());
    }

    @Test
    void send_missingEmail_returnsNonRetryableFailure() {
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(new FakeSmtpMailSender("message-1"));

        ChannelSendResult result = sender.send(new ChannelSendCommand());

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.RECIPIENT_INVALID.name(), result.getFailCode());
        assertEquals("邮箱地址不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_invalidConfig_returnsConfigFailure() {
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(new FakeSmtpMailSender("message-1"));
        ChannelSendCommand command = new ChannelSendCommand();
        command.setEmail("user@example.com");
        command.setChannelConfigJson("{\"host\":\"smtp.example.com\"}");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), result.getFailCode());
        assertEquals("SMTP 账号不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_validEmailWithHtmlAndAttachmentFileIds_sendsBySmtp() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("smtp-message-2001");
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender);
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(2001L);
        command.setEmail("user@example.com");
        command.setTitle("发货通知");
        command.setContent("<p>订单 SO-1001 已发货</p>");
        command.setAttachmentFileIds(List.of(1001L, 1002L));
        command.setChannelConfigJson("""
                {"host":"smtp.example.com","port":465,"username":"u","password":"p","from":"noreply@example.com","ssl":true}
                """);

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("smtp-message-2001", result.getProviderMessageId());
        assertEquals("{\"status\":\"SENT\",\"provider\":\"SMTP\"}", result.getResponseSnapshot());
        assertEquals("user@example.com", mailSender.request.to());
        assertEquals("发货通知", mailSender.request.subject());
        assertEquals(List.of(1001L, 1002L), command.getAttachmentFileIds());
    }

    private static class FakeSmtpMailSender implements EmailNoticeChannelSender.SmtpMailSender {

        private final String messageId;

        private EmailNoticeChannelSender.EmailRequest request;

        FakeSmtpMailSender(String messageId) {
            this.messageId = messageId;
        }

        @Override
        public String send(EmailNoticeChannelSender.EmailRequest request) {
            this.request = request;
            return messageId;
        }
    }
}
