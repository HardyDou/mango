package io.mango.notice.channel.email;

import io.mango.notice.api.InboundNoticeAttachmentRequest;
import io.mango.notice.api.InboundNoticeMessageRequest;
import io.mango.notice.api.InboundNoticeHeaderRequest;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.mango.notice.support.channel.NoticeInboundMailAccount;
import io.mango.notice.support.channel.NoticeInboundMailClient;
import io.mango.notice.support.channel.NoticeInboundMailItem;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.pop3.POP3Folder;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Jakarta Mail based IMAP/POP3 receiver; protocol is selected per account. */
@Component
public class JakartaMailInboundClient implements NoticeInboundMailClient {
    private static final long MAX_MESSAGE_BYTES = 25L * 1024 * 1024;
    private static final int READ_BUFFER_BYTES = 8192;

    @Override
    public boolean supports(NoticeInboundProtocol protocol) {
        return protocol == NoticeInboundProtocol.IMAP || protocol == NoticeInboundProtocol.POP3;
    }

    @Override
    public List<NoticeInboundMailItem> fetch(
            NoticeInboundMailAccount account, String cursorValue, String cursorVersion) {
        if (account == null || account.protocol() == null) {
            throw new IllegalArgumentException("邮箱接收账号和协议不能为空");
        }
        String storeProtocol = storeProtocol(account.protocol(), account.ssl());
        Properties properties = new Properties();
        properties.setProperty("mail.store.protocol", storeProtocol);
        properties.setProperty("mail." + storeProtocol + ".connectiontimeout", "20000");
        properties.setProperty("mail." + storeProtocol + ".timeout", "20000");
        try (Store store = Session.getInstance(properties).getStore(storeProtocol)) {
            store.connect(account.host(), account.port(), account.username(), account.password());
            identifyImapClient(store, account);
            Folder inbox = store.getFolder("INBOX");
            try {
                inbox.open(Folder.READ_ONLY);
                return switch (account.protocol()) {
                    case IMAP -> fetchImap(account, inbox, cursorValue, cursorVersion);
                    case POP3 -> fetchPop3(account, inbox, cursorValue);
                    case WEBHOOK -> throw new InboundMailException("邮箱客户端不支持 WEBHOOK 协议", null);
                };
            } finally {
                if (inbox.isOpen()) {
                    inbox.close(false);
                }
            }
        } catch (MessagingException | IOException ex) {
            throw new InboundMailException("邮箱接收失败", ex);
        }
    }

    private void identifyImapClient(Store store, NoticeInboundMailAccount account) throws MessagingException {
        if (store instanceof IMAPStore imapStore
                && account.clientName() != null && !account.clientName().isBlank()) {
            imapStore.id(Map.of(
                    "name", account.clientName().trim(),
                    "vendor", "Mango",
                    "version", "1.0",
                    "contact-address", account.username()));
        }
    }

    private List<NoticeInboundMailItem> fetchImap(
            NoticeInboundMailAccount account, Folder inbox, String cursorValue, String cursorVersion)
            throws MessagingException, IOException {
        if (!(inbox instanceof UIDFolder uidFolder)) {
            throw new InboundMailException("IMAP 服务器不支持 UID 游标", null);
        }
        String uidValidity = String.valueOf(uidFolder.getUIDValidity());
        long lastUid = uidValidity.equals(cursorVersion) ? cursorLong(cursorValue) : 0L;
        Message[] messages = uidFolder.getMessagesByUID(lastUid + 1L, UIDFolder.LASTUID);
        List<NoticeInboundMailItem> result = new ArrayList<>(messages.length);
        for (Message message : messages) {
            long uid = uidFolder.getUID(message);
            if (uid <= 0L) {
                throw new InboundMailException("IMAP 消息缺少有效 UID", null);
            }
            String sourceKey = "IMAP:" + uidValidity + ":" + uid;
            result.add(new NoticeInboundMailItem(
                    parse(account, message, sourceKey), String.valueOf(uid), uidValidity));
        }
        return List.copyOf(result);
    }

    private List<NoticeInboundMailItem> fetchPop3(
            NoticeInboundMailAccount account, Folder inbox, String cursorValue)
            throws MessagingException, IOException {
        if (!(inbox instanceof POP3Folder pop3Folder)) {
            throw new InboundMailException("POP3 服务器不支持 UIDL 游标", null);
        }
        Message[] messages = inbox.getMessages();
        boolean afterCursor = cursorValue == null || cursorValue.isBlank();
        boolean cursorFound = afterCursor;
        List<NoticeInboundMailItem> result = new ArrayList<>();
        for (Message message : messages) {
            String uidl = pop3Folder.getUID(message);
            if (uidl == null || uidl.isBlank()) {
                throw new InboundMailException("POP3 消息缺少 UIDL", null);
            }
            if (!afterCursor) {
                if (uidl.equals(cursorValue)) {
                    afterCursor = true;
                    cursorFound = true;
                }
                continue;
            }
            result.add(new NoticeInboundMailItem(
                    parse(account, message, "POP3:" + uidl), uidl, null));
        }
        if (!cursorFound) {
            result.clear();
            for (Message message : messages) {
                String uidl = pop3Folder.getUID(message);
                result.add(new NoticeInboundMailItem(
                        parse(account, message, "POP3:" + uidl), uidl, null));
            }
        }
        return List.copyOf(result);
    }

    private InboundNoticeMessageRequest parse(
            NoticeInboundMailAccount account, Message source, String sourceKey)
            throws MessagingException, IOException {
        MimeMessage message = (MimeMessage) source;
        ParsedContent content = new ParsedContent();
        parsePart(message, content);
        String messageId = header(message, "Message-ID");
        return new InboundNoticeMessageRequest(
                account.tenantId(), account.channelConfigId(), NoticeChannelType.EMAIL, "STANDARD_MAIL",
                account.protocol(), sourceKey, messageId, message.getSubject(), from(message), addresses(message),
                content.text.toString(), content.html.toString(), safeHeaders(message), content.attachments,
                message.getReceivedDate() == null ? Instant.now() : message.getReceivedDate().toInstant());
    }

    private void parsePart(Part part, ParsedContent target) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part.getFileName() != null) {
            byte[] bytes = readLimited(part);
            target.attachments.add(new InboundNoticeAttachmentRequest(target.attachments.size(),
                    firstText(part.getFileName(), "attachment-" + target.attachments.size()),
                    part.getContentType(), bytes.length, new ByteArrayInputStream(bytes)));
            return;
        }
        if (part.isMimeType("text/plain")) {
            target.text.append(String.valueOf(part.getContent()));
            return;
        }
        if (part.isMimeType("text/html")) {
            target.html.append(String.valueOf(part.getContent()));
            return;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                parsePart(bodyPart, target);
            }
        }
    }

    private byte[] readLimited(Part part) throws MessagingException, IOException {
        try (var input = part.getInputStream(); var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[READ_BUFFER_BYTES];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_MESSAGE_BYTES) {
                    throw new IOException("入站附件超过大小限制");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private List<InboundNoticeHeaderRequest> safeHeaders(Message message) throws MessagingException {
        List<InboundNoticeHeaderRequest> headers = new ArrayList<>();
        for (String name : List.of("Message-ID", "In-Reply-To", "References", "Date", "Reply-To")) {
            String value = header(message, name);
            if (value != null) {
                headers.add(new InboundNoticeHeaderRequest(name, value));
            }
        }
        return List.copyOf(headers);
    }

    private String header(Message message, String name) throws MessagingException {
        String[] values = message.getHeader(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    private String from(Message message) throws MessagingException {
        Address[] values = message.getFrom();
        if (values == null || values.length == 0) {
            return null;
        }
        if (values[0] instanceof InternetAddress address) {
            return address.getAddress();
        }
        return values[0].toString();
    }

    private List<String> addresses(Message message) throws MessagingException {
        Address[] values = message.getAllRecipients();
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.length);
        for (Address value : values) {
            result.add(value instanceof InternetAddress address ? address.getAddress() : value.toString());
        }
        return List.copyOf(result);
    }

    private String storeProtocol(NoticeInboundProtocol protocol, boolean ssl) {
        return switch (protocol) {
            case IMAP -> ssl ? "imaps" : "imap";
            case POP3 -> ssl ? "pop3s" : "pop3";
            case WEBHOOK -> throw new InboundMailException("邮箱客户端不支持 WEBHOOK 协议", null);
        };
    }

    private long cursorLong(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(cursor));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("邮箱接收游标格式错误", ex);
        }
    }

    private String firstText(String first, String second) {
        return first == null || first.isBlank() ? second : first.trim();
    }

    private static final class ParsedContent {
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder html = new StringBuilder();
        private final List<InboundNoticeAttachmentRequest> attachments = new ArrayList<>();
    }

    public static final class InboundMailException extends RuntimeException {
        public InboundMailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
