package io.mango.notice.channel.email;

import io.mango.common.exception.BizException;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.NoticeChannelMessage;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNoticeChannelSenderTest {

    @Test
    void channelType_returnsEmail() {
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender();

        assertEquals(NoticeChannelType.EMAIL, sender.channelType());
    }

    @Test
    void send_missingEmail_returnsNonRetryableFailure() {
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(new FakeSmtpMailSender("message-1"));

        ChannelSendResult result = sender.send(new NoticeChannelMessage());

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.RECIPIENT_INVALID.name(), result.getFailCode());
        assertEquals("邮箱地址不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_invalidConfig_returnsConfigFailure() {
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(new FakeSmtpMailSender("message-1"));
        NoticeChannelMessage command = new NoticeChannelMessage();
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
        IFileContentProvider fileProvider = mock(IFileContentProvider.class);
        when(fileProvider.downloadForService(1001L)).thenReturn(download("清单.pdf", "application/pdf", "pdf"));
        when(fileProvider.downloadForService(1002L)).thenReturn(download("说明.txt", "text/plain", "readme"));
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender, fileProvider);
        NoticeChannelMessage command = validCommand();
        command.setAttachmentFileIds(List.of(1001L, 1002L));

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("smtp-message-2001", result.getProviderMessageId());
        assertTrue(result.getResponseSnapshot().contains("\"fileId\":1001"));
        assertTrue(result.getResponseSnapshot().contains("\"fileName\":\"清单.pdf\""));
        assertFalse(result.getResponseSnapshot().contains("cGRm"));
        assertEquals("user@example.com", mailSender.request.to());
        assertEquals("发货通知", mailSender.request.subject());
        assertEquals(2, mailSender.request.attachments().size());
        verify(fileProvider).downloadForService(1001L);
        verify(fileProvider).downloadForService(1002L);
    }

    @Test
    void send_withoutAttachments_doesNotRequireFileProviderAndKeepsSnapshot() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("smtp-message-2002");
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender);

        ChannelSendResult result = sender.send(validCommand());

        assertTrue(result.isSuccess());
        assertEquals("{\"status\":\"SENT\",\"provider\":\"SMTP\"}", result.getResponseSnapshot());
        assertTrue(mailSender.request.attachments().isEmpty());
    }

    @Test
    void message_withAttachments_buildsMultipartMixedWithHtmlAndEncodedFileName() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("smtp-message-2003");
        IFileContentProvider fileProvider = mock(IFileContentProvider.class);
        byte[] attachment = "真实附件内容".getBytes(StandardCharsets.UTF_8);
        when(fileProvider.downloadForService(1001L)).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream(attachment), "../报价 \"最终\".pdf", "application/pdf", attachment.length));
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender, fileProvider);
        NoticeChannelMessage command = validCommand();
        command.setAttachmentFileIds(List.of(1001L));
        assertTrue(sender.send(command).isSuccess());

        String mime = new EmailNoticeChannelSender.SocketSmtpMailSender()
                .message(mailSender.request, "<test@mango.local>");

        assertTrue(mime.contains("Content-Type: multipart/mixed; boundary=\"mango-notice-"));
        assertTrue(mime.contains("Content-Type: text/html; charset=UTF-8"));
        assertTrue(mime.contains("Content-Type: application/pdf; name=\""));
        assertFalse(mime.contains("../"));
        assertTrue(mime.contains("filename*=UTF-8''%E6%8A%A5%E4%BB%B7%20%22%E6%9C%80%E7%BB%88%22.pdf"));
        assertTrue(mime.contains(Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(attachment)));
        assertTrue(mime.endsWith("\r\n.\r\n"));
    }

    @Test
    void send_attachmentProviderMissing_failsBeforeSmtp() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("must-not-send");
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender);
        NoticeChannelMessage command = validCommand();
        command.setAttachmentFileIds(List.of(1001L));

        ChannelSendResult result = sender.send(command);

        assertAttachmentFailure(result, NoticeFailureCode.ATTACHMENT_PROVIDER_UNAVAILABLE, false);
        assertEquals(null, mailSender.request);
    }

    @Test
    void send_attachmentNotFound_mapsStableNonRetryableFailure() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("must-not-send");
        IFileContentProvider fileProvider = mock(IFileContentProvider.class);
        when(fileProvider.downloadForService(1001L)).thenThrow(
                new BizException(FileCode.FILE_NOT_FOUND.getCode(), "底层路径不得泄漏 /data/secret"));
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender, fileProvider);
        NoticeChannelMessage command = validCommand();
        command.setAttachmentFileIds(List.of(1001L));

        ChannelSendResult result = sender.send(command);

        assertAttachmentFailure(result, NoticeFailureCode.ATTACHMENT_NOT_FOUND_OR_FORBIDDEN, false);
        assertTrue(result.getFailReason().contains("fileId=1001"));
        assertFalse(result.getFailReason().contains("/data/secret"));
        assertEquals(null, mailSender.request);
    }

    @Test
    void send_attachmentCountSizeAndTypeViolations_failBeforeSmtp() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("must-not-send");
        IFileContentProvider fileProvider = mock(IFileContentProvider.class);
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender, fileProvider);

        NoticeChannelMessage tooMany = validCommand("\"attachmentMaxCount\":1");
        tooMany.setAttachmentFileIds(List.of(1L, 2L));
        assertAttachmentFailure(sender.send(tooMany), NoticeFailureCode.ATTACHMENT_LIMIT_EXCEEDED, false);

        when(fileProvider.downloadForService(3L)).thenReturn(download("script.exe", "application/x-msdownload", "x"));
        NoticeChannelMessage blockedType = validCommand();
        blockedType.setAttachmentFileIds(List.of(3L));
        assertAttachmentFailure(sender.send(blockedType), NoticeFailureCode.ATTACHMENT_TYPE_NOT_ALLOWED, false);

        when(fileProvider.downloadForService(4L)).thenReturn(download("large.pdf", "application/pdf", "12345"));
        NoticeChannelMessage tooLarge = validCommand("\"attachmentMaxFileSizeBytes\":4");
        tooLarge.setAttachmentFileIds(List.of(4L));
        assertAttachmentFailure(sender.send(tooLarge), NoticeFailureCode.ATTACHMENT_LIMIT_EXCEEDED, false);
        assertEquals(null, mailSender.request);
    }

    @Test
    void send_declaredSizeCannotBypassActualReadLimit() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("must-not-send");
        IFileContentProvider fileProvider = mock(IFileContentProvider.class);
        when(fileProvider.downloadForService(5L)).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8)), "large.txt", "text/plain", 1));
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender, fileProvider);
        NoticeChannelMessage command = validCommand("\"attachmentMaxFileSizeBytes\":4");
        command.setAttachmentFileIds(List.of(5L));

        ChannelSendResult result = sender.send(command);

        assertAttachmentFailure(result, NoticeFailureCode.ATTACHMENT_LIMIT_EXCEEDED, false);
        assertTrue(result.getResponseSnapshot().contains("\"fileId\":5"));
        assertEquals(null, mailSender.request);
    }

    @Test
    void send_attachmentReadFailureAndTimeout_areRetryable() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("must-not-send");
        IFileContentProvider fileProvider = mock(IFileContentProvider.class);
        when(fileProvider.downloadForService(6L)).thenReturn(new FileDownloadVO(
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("storage unavailable");
                    }
                }, "failed.txt", "text/plain", 1));
        when(fileProvider.downloadForService(7L)).thenReturn(new FileDownloadVO(
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        try {
                            Thread.sleep(5_000);
                            return -1;
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IOException("interrupted", ex);
                        }
                    }
                }, "slow.txt", "text/plain", 1));
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender, fileProvider);

        NoticeChannelMessage failed = validCommand();
        failed.setAttachmentFileIds(List.of(6L));
        assertAttachmentFailure(sender.send(failed), NoticeFailureCode.ATTACHMENT_READ_FAILED, true);

        NoticeChannelMessage timedOut = validCommand("\"attachmentReadTimeoutMillis\":10");
        timedOut.setAttachmentFileIds(List.of(7L));
        assertAttachmentFailure(sender.send(timedOut), NoticeFailureCode.ATTACHMENT_READ_TIMEOUT, true);
        assertEquals(null, mailSender.request);
    }

    @Test
    void send_headerInjection_isRejectedBeforeFileAndSmtp() {
        FakeSmtpMailSender mailSender = new FakeSmtpMailSender("must-not-send");
        EmailNoticeChannelSender sender = new EmailNoticeChannelSender(mailSender);
        NoticeChannelMessage command = validCommand();
        command.setTitle("正常标题\r\nBcc: attacker@example.com");

        ChannelSendResult result = sender.send(command);

        assertAttachmentFailure(result, NoticeFailureCode.CHANNEL_CONFIG_INVALID, false);
        assertEquals(null, mailSender.request);
    }

    private static NoticeChannelMessage validCommand() {
        return validCommand(null);
    }

    private static NoticeChannelMessage validCommand(String extraConfig) {
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setSendRecordId(2001L);
        command.setEmail("user@example.com");
        command.setTitle("发货通知");
        command.setContent("<p>订单 SO-1001 已发货</p>");
        command.setChannelConfigJson("{\"host\":\"smtp.example.com\",\"port\":465,\"username\":\"u\","
                + "\"password\":\"p\",\"from\":\"noreply@example.com\",\"ssl\":true"
                + (extraConfig == null ? "" : "," + extraConfig) + "}");
        return command;
    }

    private static FileDownloadVO download(String fileName, String contentType, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new FileDownloadVO(new ByteArrayInputStream(bytes), fileName, contentType, bytes.length);
    }

    private static void assertAttachmentFailure(ChannelSendResult result, NoticeFailureCode code, boolean retryable) {
        assertFalse(result.isSuccess());
        assertEquals(code.name(), result.getFailCode());
        assertEquals(retryable, result.isRetryable());
        assertNotNull(result.getFailReason());
    }

    private static class FakeSmtpMailSender implements EmailNoticeChannelSender.SmtpMailSender {

        private final String messageId;

        private EmailNoticeChannelSender.EmailMessage request;

        FakeSmtpMailSender(String messageId) {
            this.messageId = messageId;
        }

        @Override
        public String send(EmailNoticeChannelSender.EmailMessage request) {
            this.request = request;
            return messageId;
        }
    }
}
