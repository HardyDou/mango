package io.mango.notice.channel.wecom;

import io.mango.notice.api.InboundNoticeMessage;
import io.mango.notice.api.enums.NoticeChannelType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Official WeCom callback algorithm implemented outside the public endpoint. */
@Component
public class DefaultWecomInboundMessageAdapter implements WecomInboundMessageAdapter {

    private static final int AES_BLOCK_SIZE = 16;
    private static final int MAX_PADDING_BYTES = 32;
    private static final int BYTE_MASK = 0xff;
    private static final int RANDOM_BYTES = AES_BLOCK_SIZE;

    @Override
    public String verifyUrl(WecomInboundRequest request, WecomInboundConfig config) {
        requireSignature(request.signature(), config.token(), request.timestamp(), request.nonce(), request.echoString());
        return decrypt(request.echoString(), config);
    }

    @Override
    public InboundNoticeMessage parseMessage(WecomInboundRequest request, WecomInboundConfig config,
                                             String tenantId, Long channelConfigId) {
        String encrypted = element(request.body(), "Encrypt");
        requireSignature(request.signature(), config.token(), request.timestamp(), request.nonce(), encrypted);
        String plainXml = decrypt(encrypted, config);
        String messageId = firstText(element(plainXml, "MsgId"),
                element(plainXml, "EventId"), element(plainXml, "CreateTime"));
        String from = element(plainXml, "FromUserName");
        String to = element(plainXml, "ToUserName");
        String content = element(plainXml, "Content");
        String msgType = element(plainXml, "MsgType");
        String sourceKey = "WECOM:" + firstText(messageId,
                sha256(from + "|" + to + "|" + msgType + "|" + plainXml));
        return new InboundNoticeMessage(tenantId, channelConfigId, NoticeChannelType.WECOM, "WECOM", null,
                sourceKey, messageId, msgType, from, to == null ? List.of() : List.of(to),
                content, null, Map.of("msgType", firstText(msgType, "unknown")), List.of(), Instant.now());
    }

    private void requireSignature(String signature, String token, String timestamp, String nonce, String encrypted) {
        if (signature == null || token == null || timestamp == null || nonce == null || encrypted == null) {
            throw new WecomInboundException("企业微信回调签名参数不完整");
        }
        String[] values = {token, timestamp, nonce, encrypted};
        Arrays.sort(values);
        String calculated = sha1(String.join("", values));
        if (!MessageDigest.isEqual(calculated.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII))) {
            throw new WecomInboundException("企业微信回调签名校验失败");
        }
    }

    private String decrypt(String encrypted, WecomInboundConfig config) {
        try {
            byte[] key = Base64.getDecoder().decode(config.encodingAesKey() + "=");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(key, 0, AES_BLOCK_SIZE));
            byte[] padded = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            byte[] plain = unpad(padded);
            ByteBuffer buffer = ByteBuffer.wrap(plain);
            byte[] random = new byte[RANDOM_BYTES];
            buffer.get(random);
            int length = buffer.getInt();
            if (length < 0 || length > buffer.remaining()) {
                throw new WecomInboundException("企业微信回调密文长度非法");
            }
            byte[] message = new byte[length];
            buffer.get(message);
            byte[] receiver = new byte[buffer.remaining()];
            buffer.get(receiver);
            String receiverId = new String(receiver, StandardCharsets.UTF_8);
            if (config.corpId() != null && !config.corpId().isBlank() && !config.corpId().equals(receiverId)) {
                throw new WecomInboundException("企业微信回调接收方不匹配");
            }
            return new String(message, StandardCharsets.UTF_8);
        } catch (WecomInboundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WecomInboundException("企业微信回调解密失败", ex);
        }
    }

    private byte[] unpad(byte[] value) {
        int padding = value[value.length - 1] & BYTE_MASK;
        if (padding < 1 || padding > MAX_PADDING_BYTES || padding > value.length) {
            throw new WecomInboundException("企业微信回调填充非法");
        }
        for (int index = value.length - padding; index < value.length; index++) {
            if ((value[index] & BYTE_MASK) != padding) {
                throw new WecomInboundException("企业微信回调填充非法");
            }
        }
        return Arrays.copyOf(value, value.length - padding);
    }

    private String element(String xml, String name) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element element = (Element) document.getElementsByTagName(name).item(0);
            return element == null ? null : element.getTextContent();
        } catch (Exception ex) {
            throw new WecomInboundException("企业微信回调 XML 解析失败", ex);
        }
    }

    private String sha1(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new WecomInboundException("企业微信签名算法不可用", ex);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new WecomInboundException("摘要算法不可用", ex);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public static final class WecomInboundException extends RuntimeException {
        public WecomInboundException(String message) {
            super(message);
        }

        public WecomInboundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
