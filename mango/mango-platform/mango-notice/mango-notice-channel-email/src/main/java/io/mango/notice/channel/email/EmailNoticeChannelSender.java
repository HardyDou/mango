package io.mango.notice.channel.email;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EmailNoticeChannelSender implements NoticeChannelSender {

    private final SmtpMailSender smtpMailSender;

    public EmailNoticeChannelSender() {
        this(new SocketSmtpMailSender());
    }

    EmailNoticeChannelSender(SmtpMailSender smtpMailSender) {
        this.smtpMailSender = smtpMailSender;
    }

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.EMAIL;
    }

    @Override
    public ChannelSendResult send(ChannelSendCommand command) {
        if (!StringUtils.hasText(command.getEmail())) {
            return ChannelSendResult.failed(NoticeFailureCode.RECIPIENT_INVALID.name(), "邮箱地址不能为空", false);
        }
        if (!StringUtils.hasText(command.getChannelConfigJson())) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), "邮件通道配置不能为空", false);
        }
        EmailConfig config;
        try {
            config = EmailConfig.from(command.getChannelConfigJson());
        } catch (IllegalArgumentException ex) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), ex.getMessage(), false);
        }
        try {
            String messageId = smtpMailSender.send(EmailRequest.from(command, config));
            return ChannelSendResult.providerSuccess(messageId, "{\"status\":\"SENT\",\"provider\":\"SMTP\"}");
        } catch (SmtpAuthException ex) {
            return ChannelSendResult.failed(NoticeFailureCode.PROVIDER_REJECTED.name(), "SMTP 认证失败", false);
        } catch (SmtpException ex) {
            return ChannelSendResult.failed(NoticeFailureCode.PROVIDER_ERROR.name(), ex.getMessage(), true);
        }
    }

    interface SmtpMailSender {

        String send(EmailRequest request) throws SmtpException;
    }

    record EmailConfig(String host, int port, String username, String password, String from, String senderName,
                       boolean ssl, int timeoutMillis) {

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
            return new EmailConfig(host, integer(config.get("port"), ssl ? 465 : 25), username, password, from,
                    text(config, "senderName", "fromAlias"), ssl, integer(config.get("timeoutMillis"), 20000));
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

        private static boolean bool(String value, boolean defaultValue) {
            return StringUtils.hasText(value) ? Boolean.parseBoolean(value.trim()) : defaultValue;
        }
    }

    record EmailRequest(Long sendRecordId, String to, String subject, String content, EmailConfig config) {

        static EmailRequest from(ChannelSendCommand command, EmailConfig config) {
            return new EmailRequest(command.getSendRecordId(), command.getEmail(),
                    StringUtils.hasText(command.getTitle()) ? command.getTitle() : "通知消息",
                    StringUtils.hasText(command.getContent()) ? command.getContent() : "", config);
        }
    }

    static class SocketSmtpMailSender implements SmtpMailSender {

        @Override
        public String send(EmailRequest request) throws SmtpException {
            try (Socket socket = openSocket(request.config())) {
                socket.setSoTimeout(request.config().timeoutMillis());
                SmtpSession session = new SmtpSession(socket);
                session.expect(220);
                session.command("EHLO mango.local", 250);
                session.command("AUTH LOGIN", 334);
                session.command(base64(request.config().username()), 334);
                session.command(base64(request.config().password()), 235);
                session.command("MAIL FROM:<" + request.config().from() + ">", 250);
                session.command("RCPT TO:<" + request.to() + ">", 250, 251);
                session.command("DATA", 354);
                String messageId = "<mango-notice-" + request.sendRecordId() + "-" + UUID.randomUUID() + "@mango.local>";
                session.data(message(request, messageId));
                session.command("QUIT", 221);
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

        private String message(EmailRequest request, String messageId) {
            String fromName = StringUtils.hasText(request.config().senderName())
                    ? mimeText(request.config().senderName()) + " <" + request.config().from() + ">"
                    : request.config().from();
            StringBuilder builder = new StringBuilder();
            builder.append("Message-ID: ").append(messageId).append("\r\n");
            builder.append("From: ").append(fromName).append("\r\n");
            builder.append("To: ").append(request.to()).append("\r\n");
            builder.append("Subject: ").append(mimeText(request.subject())).append("\r\n");
            builder.append("MIME-Version: 1.0\r\n");
            builder.append("Content-Type: text/html; charset=UTF-8\r\n");
            builder.append("Content-Transfer-Encoding: base64\r\n");
            builder.append("\r\n");
            builder.append(Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(htmlBody(request.content()).getBytes(StandardCharsets.UTF_8)));
            builder.append("\r\n.\r\n");
            return builder.toString();
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

        SmtpSession(Socket socket) throws IOException {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
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
            expect(250);
        }

        void expect(int... expectedCodes) throws IOException, SmtpException {
            SmtpReply reply = readReply();
            for (int code : expectedCodes) {
                if (reply.code() == code) {
                    return;
                }
            }
            if (reply.code() == 535 || reply.code() == 530) {
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
            int code = Integer.parseInt(line.substring(0, 3));
            while (line.length() > 3 && line.charAt(3) == '-') {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                text.append('\n').append(line);
            }
            return new SmtpReply(code, text.toString());
        }
    }

    record SmtpReply(int code, String text) {
    }

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

        private SimpleJson() {
        }

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
                    builder.append(switch (escaped) {
                        case '"', '\\', '/' -> escaped;
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> escaped;
                    });
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("邮件通道配置 JSON 格式错误");
        }

        private static int skip(String json, int index) {
            int cursor = index;
            while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
                cursor++;
            }
            return cursor;
        }
    }

    record ParseResult(String value, int nextIndex) {
    }
}
