package io.mango.notice.channel.email;

import io.mango.common.exception.BizException;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelMessage;
import io.mango.notice.support.channel.NoticeChannelSender;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.net.ssl.SSLSocketFactory;

@Component
public class EmailNoticeChannelSender implements NoticeChannelSender {
    private static final int ATTACHMENT_BUFFER_SIZE = 8192;
    private static final int MAX_ATTACHMENT_COUNT = 50;
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_ATTACHMENT_READ_TIMEOUT_MILLIS = 120_000L;
    private static final int MIME_BASE64_LINE_LENGTH = 76;
    private static final int PRINTABLE_ASCII_MIN = 0x20;
    private static final int PRINTABLE_ASCII_MAX = 0x7e;
    private static final int UNSIGNED_BYTE_MASK = 0xff;
    private static final int HEX_HIGH_NIBBLE_SHIFT = 4;
    private static final int HEX_LOW_NIBBLE_MASK = 0x0f;
    private static final int HEX_RADIX = 16;
    private static final int DEFAULT_SMTP_PORT = 25;
    private static final int DEFAULT_SMTPS_PORT = 465;
    private static final int DEFAULT_SMTP_TIMEOUT_MILLIS = 20_000;
    private static final int SMTP_SERVICE_READY = 220;
    private static final int SMTP_REQUEST_OK = 250;
    private static final int SMTP_USER_NOT_LOCAL = 251;
    private static final int SMTP_AUTH_SUCCESS = 235;
    private static final int SMTP_AUTH_CHALLENGE = 334;
    private static final int SMTP_START_MAIL_INPUT = 354;
    private static final int SMTP_SERVICE_CLOSING = 221;
    private static final int SMTP_AUTH_REQUIRED = 530;
    private static final int SMTP_AUTH_REJECTED = 535;
    private static final int SMTP_REPLY_CODE_LENGTH = 3;
    private static final ExecutorService ATTACHMENT_EXECUTOR =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual().name("notice-email-attachment-", 0).factory());

    private final SmtpMailSender smtpMailSender;
    private final Supplier<IFileContentProvider> fileContentProvider;

    EmailNoticeChannelSender() {
        this(new SocketSmtpMailSender(), () -> null);
    }

    EmailNoticeChannelSender(SmtpMailSender smtpMailSender) {
        this(smtpMailSender, () -> null);
    }

    EmailNoticeChannelSender(
            SmtpMailSender smtpMailSender, IFileContentProvider fileContentProvider) {
        this(smtpMailSender, () -> fileContentProvider);
    }

    private EmailNoticeChannelSender(
            SmtpMailSender smtpMailSender, Supplier<IFileContentProvider> fileContentProvider) {
        this.smtpMailSender = smtpMailSender;
        this.fileContentProvider = fileContentProvider;
    }

    @Autowired
    public EmailNoticeChannelSender(ObjectProvider<IFileContentProvider> fileContentProvider) {
        this(new SocketSmtpMailSender(), fileContentProvider::getIfAvailable);
    }

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.EMAIL;
    }

    @Override
    public ChannelSendResult send(NoticeChannelMessage command) {
        if (!StringUtils.hasText(command.getEmail())) {
            return ChannelSendResult.failed(
                    NoticeFailureCode.RECIPIENT_INVALID.name(), "邮箱地址不能为空", false);
        }
        if (!StringUtils.hasText(command.getChannelConfigJson())) {
            return ChannelSendResult.failed(
                    NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), "邮件通道配置不能为空", false);
        }
        EmailConfig config;
        try {
            config = EmailConfig.from(command.getChannelConfigJson());
        } catch (IllegalArgumentException ex) {
            return ChannelSendResult.failed(
                    NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), ex.getMessage(), false);
        }
        try {
            validateHeader(command.getEmail(), "收件人地址");
            validateHeader(config.from(), "发件人地址");
            validateHeader(config.senderName(), "发件人名称");
            validateHeader(command.getTitle(), "邮件标题");
            List<ResolvedAttachment> attachments =
                    resolveAttachments(command.getAttachmentFileIds(), config.attachmentPolicy());
            String messageId = smtpMailSender.send(EmailMessage.from(command, config, attachments));
            return ChannelSendResult.providerSuccess(messageId, successSnapshot(attachments));
        } catch (AttachmentException ex) {
            return ChannelSendResult.failed(
                    ex.failureCode().name(), ex.getMessage(), ex.retryable(), failureSnapshot(ex));
        } catch (IllegalArgumentException ex) {
            return ChannelSendResult.failed(
                    NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), ex.getMessage(), false);
        } catch (SmtpAuthException ex) {
            return ChannelSendResult.failed(
                    NoticeFailureCode.PROVIDER_REJECTED.name(), "SMTP 认证失败", false);
        } catch (SmtpException ex) {
            return ChannelSendResult.failed(
                    NoticeFailureCode.PROVIDER_ERROR.name(), ex.getMessage(), true);
        }
    }

    private List<ResolvedAttachment> resolveAttachments(List<Long> fileIds, AttachmentPolicy policy)
            throws AttachmentException {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        if (fileIds.size() > policy.maxCount()) {
            throw attachmentFailure(
                    NoticeFailureCode.ATTACHMENT_LIMIT_EXCEEDED,
                    null,
                    "附件数量超过限制（最多 " + policy.maxCount() + " 个）",
                    false);
        }
        IFileContentProvider provider = fileContentProvider.get();
        if (provider == null) {
            throw attachmentFailure(
                    NoticeFailureCode.ATTACHMENT_PROVIDER_UNAVAILABLE,
                    null,
                    "文件服务不可用，无法读取邮件附件",
                    false);
        }
        List<ResolvedAttachment> attachments = new ArrayList<>(fileIds.size());
        long totalSize = 0;
        for (Long fileId : fileIds) {
            if (fileId == null) {
                throw attachmentFailure(
                        NoticeFailureCode.ATTACHMENT_NOT_FOUND_OR_FORBIDDEN,
                        null,
                        "附件文件标识不能为空",
                        false);
            }
            long remaining = policy.maxTotalSizeBytes() - totalSize;
            if (remaining <= 0) {
                throw attachmentFailure(
                        NoticeFailureCode.ATTACHMENT_LIMIT_EXCEEDED, fileId, "附件总大小超过限制", false);
            }
            Future<ResolvedAttachment> future =
                    ATTACHMENT_EXECUTOR.submit(
                            () -> loadAttachment(provider, fileId, policy, remaining));
            try {
                ResolvedAttachment attachment =
                        future.get(policy.readTimeout().toMillis(), TimeUnit.MILLISECONDS);
                attachments.add(attachment);
                totalSize += attachment.content().length;
            } catch (TimeoutException ex) {
                future.cancel(true);
                throw attachmentFailure(
                        NoticeFailureCode.ATTACHMENT_READ_TIMEOUT,
                        fileId,
                        "附件读取超时，fileId=" + fileId,
                        true);
            } catch (InterruptedException ex) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw attachmentFailure(
                        NoticeFailureCode.ATTACHMENT_READ_FAILED,
                        fileId,
                        "附件读取被中断，fileId=" + fileId,
                        true);
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof AttachmentException attachmentException) {
                    throw attachmentException;
                }
                throw attachmentFailure(
                        NoticeFailureCode.ATTACHMENT_READ_FAILED,
                        fileId,
                        "附件读取失败，fileId=" + fileId,
                        true);
            }
        }
        return List.copyOf(attachments);
    }

    private ResolvedAttachment loadAttachment(
            IFileContentProvider provider,
            Long fileId,
            AttachmentPolicy policy,
            long remainingTotal)
            throws AttachmentException {
        FileDownloadVO download;
        try {
            download = provider.downloadForService(fileId);
        } catch (BizException ex) {
            boolean unavailable =
                    ex.getCode() == FileCode.FILE_NOT_FOUND.getCode()
                            || ex.getCode() == FileCode.FILE_ACCESS_DENIED.getCode()
                            || ex.getCode() == FileCode.FILE_STATUS_INVALID.getCode();
            throw attachmentFailure(
                    unavailable
                            ? NoticeFailureCode.ATTACHMENT_NOT_FOUND_OR_FORBIDDEN
                            : NoticeFailureCode.ATTACHMENT_READ_FAILED,
                    fileId,
                    unavailable ? "附件不存在或不可访问，fileId=" + fileId : "附件读取失败，fileId=" + fileId,
                    !unavailable);
        } catch (RuntimeException ex) {
            throw attachmentFailure(
                    NoticeFailureCode.ATTACHMENT_READ_FAILED,
                    fileId,
                    "附件读取失败，fileId=" + fileId,
                    true);
        }
        if (download == null || download.inputStream() == null) {
            throw attachmentFailure(
                    NoticeFailureCode.ATTACHMENT_READ_FAILED,
                    fileId,
                    "附件内容不可用，fileId=" + fileId,
                    true);
        }
        String contentType = normalizeContentType(download.contentType());
        if (!policy.allowedContentTypes().contains(contentType)) {
            closeQuietly(download.inputStream());
            throw attachmentFailure(
                    NoticeFailureCode.ATTACHMENT_TYPE_NOT_ALLOWED,
                    fileId,
                    "附件类型不允许，fileId=" + fileId + "，contentType=" + contentType,
                    false);
        }
        long declaredSize = download.contentLength();
        if (declaredSize > policy.maxFileSizeBytes() || declaredSize > remainingTotal) {
            closeQuietly(download.inputStream());
            throw attachmentFailure(
                    NoticeFailureCode.ATTACHMENT_LIMIT_EXCEEDED,
                    fileId,
                    "附件大小超过限制，fileId=" + fileId,
                    false);
        }
        long readLimit = Math.min(policy.maxFileSizeBytes(), remainingTotal);
        try (InputStream inputStream = download.inputStream()) {
            byte[] content = readLimited(inputStream, readLimit, fileId);
            return new ResolvedAttachment(
                    fileId, safeFileName(download.fileName(), fileId), contentType, content);
        } catch (AttachmentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw attachmentFailure(
                    NoticeFailureCode.ATTACHMENT_READ_FAILED,
                    fileId,
                    "附件读取失败，fileId=" + fileId,
                    true);
        }
    }

    private byte[] readLimited(InputStream inputStream, long limit, Long fileId)
            throws IOException, AttachmentException {
        ByteArrayOutputStream output =
                new ByteArrayOutputStream((int) Math.min(limit, ATTACHMENT_BUFFER_SIZE));
        byte[] buffer = new byte[ATTACHMENT_BUFFER_SIZE];
        long total = 0;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("interrupted");
            }
            int read = inputStream.read(buffer);
            if (read < 0) {
                return output.toByteArray();
            }
            total += read;
            if (total > limit) {
                throw attachmentFailure(
                        NoticeFailureCode.ATTACHMENT_LIMIT_EXCEEDED,
                        fileId,
                        "附件实际大小超过限制，fileId=" + fileId,
                        false);
            }
            output.write(buffer, 0, read);
        }
    }

    private void validateHeader(String value, String fieldName) {
        if (value != null && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)) {
            throw new IllegalArgumentException(fieldName + "包含非法换行符");
        }
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        int parameter = contentType.indexOf(';');
        String value = parameter >= 0 ? contentType.substring(0, parameter) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeFileName(String fileName, Long fileId) {
        String value = StringUtils.hasText(fileName) ? fileName : "attachment-" + fileId;
        value = value.replace('\\', '/');
        value =
                value.substring(value.lastIndexOf('/') + 1)
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim();
        return value.isEmpty() ? "attachment-" + fileId : value;
    }

    private static void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
            // Best-effort close before returning a deterministic validation failure.
        }
    }

    private static AttachmentException attachmentFailure(
            NoticeFailureCode failureCode, Long fileId, String message, boolean retryable) {
        return new AttachmentException(failureCode, fileId, message, retryable);
    }

    private static String successSnapshot(List<ResolvedAttachment> attachments) {
        if (attachments.isEmpty()) {
            return "{\"status\":\"SENT\",\"provider\":\"SMTP\"}";
        }
        StringBuilder snapshot =
                new StringBuilder("{\"status\":\"SENT\",\"provider\":\"SMTP\",\"attachments\":[");
        for (int index = 0; index < attachments.size(); index++) {
            if (index > 0) {
                snapshot.append(',');
            }
            ResolvedAttachment attachment = attachments.get(index);
            snapshot.append("{\"fileId\":")
                    .append(attachment.fileId())
                    .append(",\"fileName\":\"")
                    .append(jsonEscape(attachment.fileName()))
                    .append("\",\"contentType\":\"")
                    .append(jsonEscape(attachment.contentType()))
                    .append("\",\"size\":")
                    .append(attachment.content().length)
                    .append(",\"status\":\"SENT\"}");
        }
        return snapshot.append("]}").toString();
    }

    private static String failureSnapshot(AttachmentException failure) {
        return "{\"status\":\"FAILED\",\"stage\":\"ATTACHMENT\",\"fileId\":"
                + (failure.fileId() == null ? "null" : failure.fileId())
                + ",\"failCode\":\""
                + failure.failureCode().name()
                + "\"}";
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    interface SmtpMailSender {
        String send(EmailMessage message) throws SmtpException;
    }

    record EmailConfig(
            String host,
            int port,
            String username,
            String password,
            String from,
            String senderName,
            boolean ssl,
            int timeoutMillis,
            AttachmentPolicy attachmentPolicy) {
        static EmailConfig from(String configJson) {
            Map<String, String> config = SimpleJson.parse(configJson);
            String host = text(config, "host", "smtpHost");
            String username = text(config, "username", "account");
            String password = text(config, "password", "smtpPassword");
            String from = text(config, "from", "fromAddress");
            if (!StringUtils.hasText(host)) {
                throw new IllegalArgumentException("SMTP 地址不能为空");
            }
            if (!StringUtils.hasText(username)) {
                throw new IllegalArgumentException("SMTP 账号不能为空");
            }
            if (!StringUtils.hasText(password)) {
                throw new IllegalArgumentException("SMTP 密码不能为空");
            }
            if (!StringUtils.hasText(from)) {
                throw new IllegalArgumentException("发件人不能为空");
            }
            boolean ssl = bool(config.get("ssl"), false);
            return new EmailConfig(
                    host,
                    integer(config.get("port"), ssl ? DEFAULT_SMTPS_PORT : DEFAULT_SMTP_PORT),
                    username,
                    password,
                    from,
                    text(config, "senderName", "fromAlias"),
                    ssl,
                    integer(config.get("timeoutMillis"), DEFAULT_SMTP_TIMEOUT_MILLIS),
                    AttachmentPolicy.from(config));
        }

        private static String text(Map<String, String> config, String... keys) {
            for (String key : keys) {
                String value = config.get(key);
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
            return null;
        }

        private static int integer(String value, int defaultValue) {
            if (!StringUtils.hasText(value)) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }

        private static long longValue(String value, long defaultValue) {
            if (!StringUtils.hasText(value)) {
                return defaultValue;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("邮件附件限制配置必须为整数");
            }
        }

        private static boolean bool(String value, boolean defaultValue) {
            return StringUtils.hasText(value) ? Boolean.parseBoolean(value.trim()) : defaultValue;
        }
    }

    record AttachmentPolicy(
            int maxCount,
            long maxFileSizeBytes,
            long maxTotalSizeBytes,
            Duration readTimeout,
            Set<String> allowedContentTypes) {
        private static final int DEFAULT_MAX_COUNT = 10;
        private static final long DEFAULT_MAX_FILE_SIZE = 10L * 1024 * 1024;
        private static final long DEFAULT_MAX_TOTAL_SIZE = 25L * 1024 * 1024;
        private static final long DEFAULT_TIMEOUT_MILLIS = 15_000L;
        private static final Set<String> DEFAULT_ALLOWED_TYPES =
                Set.of(
                        "application/pdf",
                        "application/zip",
                        "application/json",
                        "application/msword",
                        "application/vnd.ms-excel",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "image/gif",
                        "image/jpeg",
                        "image/png",
                        "image/webp",
                        "text/csv",
                        "text/plain");

        static AttachmentPolicy from(Map<String, String> config) {
            int maxCount = EmailConfig.integer(config.get("attachmentMaxCount"), DEFAULT_MAX_COUNT);
            long maxFileSize =
                    EmailConfig.longValue(
                            config.get("attachmentMaxFileSizeBytes"), DEFAULT_MAX_FILE_SIZE);
            long maxTotalSize =
                    EmailConfig.longValue(
                            config.get("attachmentMaxTotalSizeBytes"), DEFAULT_MAX_TOTAL_SIZE);
            long timeoutMillis =
                    EmailConfig.longValue(
                            config.get("attachmentReadTimeoutMillis"), DEFAULT_TIMEOUT_MILLIS);
            if (maxCount <= 0
                    || maxCount > MAX_ATTACHMENT_COUNT
                    || maxFileSize <= 0
                    || maxFileSize > MAX_ATTACHMENT_SIZE_BYTES
                    || maxTotalSize <= 0
                    || maxTotalSize > MAX_ATTACHMENT_SIZE_BYTES
                    || timeoutMillis <= 0
                    || timeoutMillis > MAX_ATTACHMENT_READ_TIMEOUT_MILLIS) {
                throw new IllegalArgumentException("邮件附件限制配置超出允许范围");
            }
            Set<String> allowedTypes =
                    parseAllowedTypes(config.get("attachmentAllowedContentTypes"));
            if (allowedTypes.isEmpty()) {
                throw new IllegalArgumentException("邮件附件 MIME 白名单不能为空");
            }
            return new AttachmentPolicy(
                    maxCount,
                    maxFileSize,
                    maxTotalSize,
                    Duration.ofMillis(timeoutMillis),
                    allowedTypes);
        }

        private static Set<String> parseAllowedTypes(String configured) {
            if (!StringUtils.hasText(configured)) {
                return DEFAULT_ALLOWED_TYPES;
            }
            Set<String> values = new HashSet<>();
            for (String value : configured.split(",")) {
                if (StringUtils.hasText(value)) {
                    values.add(normalizeContentType(value));
                }
            }
            return Set.copyOf(values);
        }
    }

    record ResolvedAttachment(Long fileId, String fileName, String contentType, byte[] content) {}

    static class AttachmentException extends Exception {
        private final NoticeFailureCode failureCode;
        private final Long fileId;
        private final boolean retryable;

        AttachmentException(
                NoticeFailureCode failureCode, Long fileId, String message, boolean retryable) {
            super(message);
            this.failureCode = failureCode;
            this.fileId = fileId;
            this.retryable = retryable;
        }

        NoticeFailureCode failureCode() {
            return failureCode;
        }

        Long fileId() {
            return fileId;
        }

        boolean retryable() {
            return retryable;
        }
    }

    static final class EmailMessage {
        private final Long sendRecordId;
        private final String to;
        private final String subject;
        private final String content;
        private final EmailConfig config;
        private final List<ResolvedAttachment> attachments;

        private EmailMessage(
                Long sendRecordId,
                String to,
                String subject,
                String content,
                EmailConfig config,
                List<ResolvedAttachment> attachments) {
            this.sendRecordId = sendRecordId;
            this.to = to;
            this.subject = subject;
            this.content = content;
            this.config = config;
            this.attachments = attachments;
        }

        static EmailMessage from(
                NoticeChannelMessage command,
                EmailConfig config,
                List<ResolvedAttachment> attachments) {
            return new EmailMessage(
                    command.getSendRecordId(),
                    command.getEmail(),
                    StringUtils.hasText(command.getTitle()) ? command.getTitle() : "通知消息",
                    StringUtils.hasText(command.getContent()) ? command.getContent() : "",
                    config,
                    attachments);
        }

        Long sendRecordId() {
            return sendRecordId;
        }

        String to() {
            return to;
        }

        String subject() {
            return subject;
        }

        String content() {
            return content;
        }

        EmailConfig config() {
            return config;
        }

        List<ResolvedAttachment> attachments() {
            return attachments;
        }
    }

    static class SocketSmtpMailSender implements SmtpMailSender {
        @Override
        public String send(EmailMessage request) throws SmtpException {
            try (Socket socket = openSocket(request.config())) {
                socket.setSoTimeout(request.config().timeoutMillis());
                SmtpSession session = SmtpSession.open(socket);
                session.expect(SMTP_SERVICE_READY);
                session.command("EHLO mango.local", SMTP_REQUEST_OK);
                session.command("AUTH LOGIN", SMTP_AUTH_CHALLENGE);
                session.command(base64(request.config().username()), SMTP_AUTH_CHALLENGE);
                session.command(base64(request.config().password()), SMTP_AUTH_SUCCESS);
                session.command("MAIL FROM:<" + request.config().from() + ">", SMTP_REQUEST_OK);
                session.command(
                        "RCPT TO:<" + request.to() + ">", SMTP_REQUEST_OK, SMTP_USER_NOT_LOCAL);
                session.command("DATA", SMTP_START_MAIL_INPUT);
                String messageId =
                        "<mango-notice-"
                                + request.sendRecordId()
                                + "-"
                                + UUID.randomUUID()
                                + "@mango.local>";
                session.data(message(request, messageId));
                session.command("QUIT", SMTP_SERVICE_CLOSING);
                return messageId;
            } catch (SmtpAuthException ex) {
                throw ex;
            } catch (IOException ex) {
                throw new SmtpException("SMTP 连接或发送失败: " + ex.getMessage(), ex);
            }
        }

        private Socket openSocket(EmailConfig config) throws IOException {
            if (config.ssl()) {
                return SSLSocketFactory.getDefault().createSocket(config.host(), config.port());
            }
            return new Socket(config.host(), config.port());
        }

        String message(EmailMessage request, String messageId) {
            String fromName =
                    StringUtils.hasText(request.config().senderName())
                            ? mimeText(request.config().senderName())
                                    + " <"
                                    + request.config().from()
                                    + ">"
                            : request.config().from();
            StringBuilder builder = new StringBuilder();
            builder.append("Message-ID: ").append(messageId).append("\r\n");
            builder.append("From: ").append(fromName).append("\r\n");
            builder.append("To: ").append(request.to()).append("\r\n");
            builder.append("Subject: ").append(mimeText(request.subject())).append("\r\n");
            builder.append("MIME-Version: 1.0\r\n");
            if (request.attachments().isEmpty()) {
                appendHtmlPart(builder, request.content(), null);
            } else {
                String boundary = "mango-notice-" + UUID.randomUUID();
                builder.append("Content-Type: multipart/mixed; boundary=\"")
                        .append(boundary)
                        .append("\"\r\n");
                builder.append("\r\n");
                appendHtmlPart(builder, request.content(), boundary);
                for (ResolvedAttachment attachment : request.attachments()) {
                    appendAttachmentPart(builder, boundary, attachment);
                }
                builder.append("--").append(boundary).append("--\r\n");
            }
            builder.append("\r\n.\r\n");
            return builder.toString();
        }

        private void appendHtmlPart(StringBuilder builder, String content, String boundary) {
            if (boundary != null) {
                builder.append("--").append(boundary).append("\r\n");
            }
            builder.append("Content-Type: text/html; charset=UTF-8\r\n");
            builder.append("Content-Transfer-Encoding: base64\r\n");
            builder.append("\r\n");
            builder.append(
                    Base64.getMimeEncoder(
                                    MIME_BASE64_LINE_LENGTH,
                                    "\r\n".getBytes(StandardCharsets.US_ASCII))
                            .encodeToString(htmlBody(content).getBytes(StandardCharsets.UTF_8)));
            builder.append("\r\n");
        }

        private void appendAttachmentPart(
                StringBuilder builder, String boundary, ResolvedAttachment attachment) {
            String fallbackName = asciiFileName(attachment.fileName());
            String encodedName = rfc5987(attachment.fileName());
            builder.append("--").append(boundary).append("\r\n");
            builder.append("Content-Type: ")
                    .append(attachment.contentType())
                    .append("; name=\"")
                    .append(fallbackName)
                    .append("\"\r\n");
            builder.append("Content-Disposition: attachment; filename=\"")
                    .append(fallbackName)
                    .append("\"; filename*=UTF-8''")
                    .append(encodedName)
                    .append("\r\n");
            builder.append("Content-Transfer-Encoding: base64\r\n");
            builder.append("\r\n");
            builder.append(
                    Base64.getMimeEncoder(
                                    MIME_BASE64_LINE_LENGTH,
                                    "\r\n".getBytes(StandardCharsets.US_ASCII))
                            .encodeToString(attachment.content()));
            builder.append("\r\n");
        }

        private String asciiFileName(String fileName) {
            StringBuilder value = new StringBuilder(fileName.length());
            for (int index = 0; index < fileName.length(); index++) {
                char ch = fileName.charAt(index);
                value.append(
                        ch >= PRINTABLE_ASCII_MIN
                                        && ch <= PRINTABLE_ASCII_MAX
                                        && ch != '"'
                                        && ch != '\\'
                                        && ch != ';'
                                ? ch
                                : '_');
            }
            return value.isEmpty() ? "attachment" : value.toString();
        }

        private String rfc5987(String fileName) {
            StringBuilder encoded = new StringBuilder();
            for (byte value : fileName.getBytes(StandardCharsets.UTF_8)) {
                int unsigned = value & UNSIGNED_BYTE_MASK;
                if ((unsigned >= 'a' && unsigned <= 'z')
                        || (unsigned >= 'A' && unsigned <= 'Z')
                        || (unsigned >= '0' && unsigned <= '9')
                        || "!#$&+-.^_`|~".indexOf(unsigned) >= 0) {
                    encoded.append((char) unsigned);
                } else {
                    encoded.append('%');
                    encoded.append(
                            Character.toUpperCase(
                                    Character.forDigit(
                                            unsigned >>> HEX_HIGH_NIBBLE_SHIFT, HEX_RADIX)));
                    encoded.append(
                            Character.toUpperCase(
                                    Character.forDigit(unsigned & HEX_LOW_NIBBLE_MASK, HEX_RADIX)));
                }
            }
            return encoded.toString();
        }

        private String htmlBody(String content) {
            if (content.matches("(?is).*<\\s*(html|body|p|div|span|table|br|strong|a)[\\s>/].*")) {
                return content;
            }
            return escapeHtml(content).replace("\n", "<br/>");
        }

        private String escapeHtml(String text) {
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
        }

        private String mimeText(String text) {
            return "=?UTF-8?B?" + base64(text) + "?=";
        }

        private String base64(String text) {
            return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    static class SmtpSession {
        private final BufferedReader reader;
        private final BufferedWriter writer;

        private SmtpSession(BufferedReader reader, BufferedWriter writer) {
            this.reader = reader;
            this.writer = writer;
        }

        static SmtpSession open(Socket socket) throws IOException {
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(), StandardCharsets.UTF_8));
            return new SmtpSession(reader, writer);
        }

        void command(String command, int... expectedCodes) throws IOException, SmtpException {
            writer.write(command);
            writer.write("\r\n");
            writer.flush();
            expect(expectedCodes);
        }

        void data(String data) throws IOException, SmtpException {
            writer.write(data);
            writer.flush();
            expect(SMTP_REQUEST_OK);
        }

        void expect(int... expectedCodes) throws IOException, SmtpException {
            SmtpReply reply = readReply();
            for (int code : expectedCodes) {
                if (reply.code() == code) {
                    return;
                }
            }
            if (reply.code() == SMTP_AUTH_REJECTED || reply.code() == SMTP_AUTH_REQUIRED) {
                throw new SmtpAuthException(reply.text());
            }
            throw new SmtpException(reply.text());
        }

        private SmtpReply readReply() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("SMTP 服务无响应");
            }
            StringBuilder text = new StringBuilder(line);
            int code = Integer.parseInt(line.substring(0, SMTP_REPLY_CODE_LENGTH));
            while (line.length() > SMTP_REPLY_CODE_LENGTH
                    && line.charAt(SMTP_REPLY_CODE_LENGTH) == '-') {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                text.append('\n').append(line);
            }
            return new SmtpReply(code, text.toString());
        }
    }

    record SmtpReply(int code, String text) {}

    static class SmtpException extends Exception {
        SmtpException(String message) {
            super(message);
        }

        SmtpException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class SmtpAuthException extends SmtpException {
        SmtpAuthException(String message) {
            super(message);
        }
    }

    static final class SimpleJson {
        private SimpleJson() {}

        static Map<String, String> parse(String json) {
            String trimmed = json == null ? "" : json.trim();
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                throw new IllegalArgumentException("邮件通道配置 JSON 格式错误");
            }
            Map<String, String> values = new LinkedHashMap<>();
            int index = 1;
            while (index < trimmed.length() - 1) {
                index = skip(trimmed, index);
                if (index >= trimmed.length() - 1) {
                    break;
                }
                ParseResult key = string(trimmed, index);
                index = skip(trimmed, key.nextIndex());
                if (index >= trimmed.length() || trimmed.charAt(index) != ':') {
                    throw new IllegalArgumentException("邮件通道配置 JSON 格式错误");
                }
                index = skip(trimmed, index + 1);
                ParseResult value = value(trimmed, index);
                values.put(key.value(), value.value());
                index = skip(trimmed, value.nextIndex());
                if (index < trimmed.length() - 1) {
                    if (trimmed.charAt(index) != ',') {
                        throw new IllegalArgumentException("邮件通道配置 JSON 格式错误");
                    }
                    index++;
                }
            }
            return values;
        }

        private static ParseResult value(String json, int index) {
            if (json.charAt(index) == '"') {
                return string(json, index);
            }
            int end = index;
            while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) {
                end++;
            }
            return new ParseResult(json.substring(index, end).trim(), end);
        }

        private static ParseResult string(String json, int index) {
            if (json.charAt(index) != '"') {
                throw new IllegalArgumentException("邮件通道配置 JSON 格式错误");
            }
            StringBuilder builder = new StringBuilder();
            int cursor = index + 1;
            while (cursor < json.length()) {
                char ch = json.charAt(cursor++);
                if (ch == '"') {
                    return new ParseResult(builder.toString(), cursor);
                }
                if (ch == '\\') {
                    if (cursor >= json.length()) {
                        throw new IllegalArgumentException("邮件通道配置 JSON 格式错误");
                    }
                    char escaped = json.charAt(cursor++);
                    builder.append(unescape(escaped));
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("邮件通道配置 JSON 格式错误");
        }

        private static char unescape(char escaped) {
            return switch (escaped) {
                case '"', '\\', '/' -> escaped;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                default -> escaped;
            };
        }

        private static int skip(String json, int index) {
            int cursor = index;
            while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
                cursor++;
            }
            return cursor;
        }
    }

    record ParseResult(String value, int nextIndex) {}
}
