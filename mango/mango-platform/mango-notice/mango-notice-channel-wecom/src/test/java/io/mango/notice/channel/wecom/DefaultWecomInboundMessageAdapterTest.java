package io.mango.notice.channel.wecom;

import io.mango.notice.api.InboundNoticeMessageRequest;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWecomInboundMessageAdapterTest {

    private static final String TOKEN = "IT_765_TOKEN";
    private static final String CORP_ID = "ww-it-765";
    private static final byte[] AES_KEY = HexFormat.of().parseHex(
            "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private static final String AES_KEY_TEXT = Base64.getEncoder().encodeToString(AES_KEY).substring(0, 43);

    private final DefaultWecomInboundMessageAdapter adapter = new DefaultWecomInboundMessageAdapter();
    private final WecomInboundConfig config = new WecomInboundConfig(CORP_ID, TOKEN, AES_KEY_TEXT);

    @Test
    void verifyUrl_validCipher_returnsPlainEcho() {
        String encrypted = encrypt("IT_765_ECHO");
        WecomInboundRequest request = new WecomInboundRequest(
                signature(encrypted), "1786590252", "1786018586", encrypted, null);

        assertThat(adapter.verifyUrl(request, config)).isEqualTo("IT_765_ECHO");
    }

    @Test
    void parseMessage_validEncryptedXml_returnsStableMessage() {
        String plain = "<xml><ToUserName>corp</ToUserName><FromUserName>user-765</FromUserName>"
                + "<CreateTime>1786590252</CreateTime><MsgType>text</MsgType>"
                + "<Content>hello inbound</Content><MsgId>msg-765</MsgId></xml>";
        String encrypted = encrypt(plain);
        String body = "<xml><Encrypt><![CDATA[" + encrypted + "]]></Encrypt></xml>";
        WecomInboundRequest request = new WecomInboundRequest(
                signature(encrypted), "1786590252", "1786018586", null, body);

        InboundNoticeMessageRequest message = adapter.parseMessage(request, config, "tenant-765", 765L);

        assertThat(message.sourceKey()).isEqualTo("WECOM:msg-765");
        assertThat(message.bodyText()).isEqualTo("hello inbound");
        assertThat(message.fromAddress()).isEqualTo("user-765");
        assertThat(message.tenantId()).isEqualTo("tenant-765");
    }

    @Test
    void parseMessage_invalidSignature_rejectsBeforeReturningMessage() {
        String encrypted = encrypt("<xml><MsgType>text</MsgType></xml>");
        String body = "<xml><Encrypt>" + encrypted + "</Encrypt></xml>";

        assertThatThrownBy(() -> adapter.parseMessage(
                new WecomInboundRequest("invalid", "1786590252", "1786018586", null, body),
                config, "tenant-765", 765L))
                .isInstanceOf(DefaultWecomInboundMessageAdapter.WecomInboundException.class)
                .hasMessageContaining("签名校验失败");
    }

    private String encrypt(String message) {
        try {
            byte[] random = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
            byte[] content = message.getBytes(StandardCharsets.UTF_8);
            byte[] receiver = CORP_ID.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(random.length + 4 + content.length + receiver.length);
            buffer.put(random).putInt(content.length).put(content).put(receiver);
            byte[] padded = pad(buffer.array());
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES_KEY, "AES"),
                    new IvParameterSpec(AES_KEY, 0, 16));
            return Base64.getEncoder().encodeToString(cipher.doFinal(padded));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private byte[] pad(byte[] value) {
        int padding = 32 - value.length % 32;
        byte[] result = Arrays.copyOf(value, value.length + padding);
        Arrays.fill(result, value.length, result.length, (byte) padding);
        return result;
    }

    private String signature(String encrypted) {
        try {
            String[] parts = {TOKEN, "1786590252", "1786018586", encrypted};
            Arrays.sort(parts);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(String.join("", parts).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
