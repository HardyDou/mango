package io.mango.notice.starter.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.notice.api.InboundNoticeMessage;
import io.mango.notice.api.NoticeInboundReceiver;
import io.mango.notice.api.NoticeInboundWebhookProvider;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.channel.wecom.DefaultWecomInboundMessageAdapter;
import io.mango.notice.channel.wecom.WecomInboundMessageAdapter;
import io.mango.notice.core.service.NoticeInboundChannelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.routerFunctions;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

class NoticeInboundPublicEndpointTest {

    private static final long CHANNEL_CONFIG_ID = 765L;
    private static final String TENANT_ID = "tenant-765";
    private static final String TOKEN = "IT_765_TOKEN";
    private static final String CORP_ID = "ww-it-765";
    private static final byte[] AES_KEY = HexFormat.of().parseHex(
            "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private static final String AES_KEY_TEXT = Base64.getEncoder().encodeToString(AES_KEY).substring(0, 43);

    private NoticeInboundChannelConfigService configService;
    private NoticeInboundReceiver receiver;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        configService = mock(NoticeInboundChannelConfigService.class);
        receiver = mock(NoticeInboundReceiver.class);
        WecomInboundMessageAdapter adapter = new DefaultWecomInboundMessageAdapter();
        ObjectMapper objectMapper = new ObjectMapper();
        NoticeInboundPublicEndpoint endpoint = new NoticeInboundPublicEndpoint(
                configService, receiver, adapter, List.<NoticeInboundWebhookProvider>of(), objectMapper);
        mockMvc = routerFunctions(route(GET("/notice/inbound-callbacks/public")
                        .or(POST("/notice/inbound-callbacks/public")), endpoint::handleWecom)
                .andRoute(POST("/notice/inbound-mail-callbacks/public"), endpoint::handleMail))
                .build();
    }

    @Test
    void wecomGet_validEncryptedEcho_returnsPlainTextWithoutReceiving() throws Exception {
        String timestamp = "1786590252";
        String nonce = "1786018586";
        String encrypted = encrypt("IT_765_ECHO");
        when(configService.resolve(CHANNEL_CONFIG_ID, NoticeChannelType.WECOM))
                .thenReturn(new NoticeInboundChannelConfigService.ResolvedInboundChannelConfig(
                        CHANNEL_CONFIG_ID, TENANT_ID, "IT_765_WECOM", NoticeChannelType.WECOM, "WECOM",
                        "{\"corpId\":\"" + CORP_ID + "\",\"token\":\"" + TOKEN
                                + "\",\"encodingAesKey\":\"" + AES_KEY_TEXT + "\"}"));

        mockMvc.perform(get("/notice/inbound-callbacks/public")
                        .queryParam("channelConfigId", String.valueOf(CHANNEL_CONFIG_ID))
                        .queryParam("msg_signature", signature(encrypted, timestamp, nonce))
                        .queryParam("timestamp", timestamp)
                        .queryParam("nonce", nonce)
                        .queryParam("echostr", encrypted))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().string("IT_765_ECHO"));

        verify(receiver, never()).receive(any());
    }

    @Test
    void wecomPost_validEncryptedXml_receivesTenantAndSourceAndAcksSuccess() throws Exception {
        String timestamp = "1786590252";
        String nonce = "1786018586";
        String plainXml = "<xml><ToUserName>corp</ToUserName><FromUserName>user-765</FromUserName>"
                + "<CreateTime>1786590252</CreateTime><MsgType>text</MsgType>"
                + "<Content>hello inbound</Content><MsgId>msg-765</MsgId></xml>";
        String encrypted = encrypt(plainXml);
        String body = "<xml><Encrypt><![CDATA[" + encrypted + "]]></Encrypt></xml>";
        when(configService.resolve(CHANNEL_CONFIG_ID, NoticeChannelType.WECOM))
                .thenReturn(new NoticeInboundChannelConfigService.ResolvedInboundChannelConfig(
                        CHANNEL_CONFIG_ID, TENANT_ID, "IT_765_WECOM", NoticeChannelType.WECOM, "WECOM",
                        "{\"corpId\":\"" + CORP_ID + "\",\"token\":\"" + TOKEN
                                + "\",\"encodingAesKey\":\"" + AES_KEY_TEXT + "\"}"));

        mockMvc.perform(post("/notice/inbound-callbacks/public")
                        .queryParam("channelConfigId", String.valueOf(CHANNEL_CONFIG_ID))
                        .queryParam("msg_signature", signature(encrypted, timestamp, nonce))
                        .queryParam("timestamp", timestamp)
                        .queryParam("nonce", nonce)
                        .contentType(MediaType.APPLICATION_XML)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().string("success"));

        ArgumentCaptor<InboundNoticeMessage> captor = ArgumentCaptor.forClass(InboundNoticeMessage.class);
        verify(receiver).receive(captor.capture());
        InboundNoticeMessage message = captor.getValue();
        assertThat(message.tenantId()).isEqualTo(TENANT_ID);
        assertThat(message.channelConfigId()).isEqualTo(CHANNEL_CONFIG_ID);
        assertThat(message.sourceKey()).isEqualTo("WECOM:msg-765");
        assertThat(message.bodyText()).isEqualTo("hello inbound");
    }

    @Test
    void wecomPost_invalidSignature_rejectsBeforeReceiver() {
        String timestamp = "1786590252";
        String nonce = "1786018586";
        String encrypted = encrypt("<xml><MsgType>text</MsgType></xml>");
        when(configService.resolve(CHANNEL_CONFIG_ID, NoticeChannelType.WECOM))
                .thenReturn(new NoticeInboundChannelConfigService.ResolvedInboundChannelConfig(
                        CHANNEL_CONFIG_ID, TENANT_ID, "IT_765_WECOM", NoticeChannelType.WECOM, "WECOM",
                        "{\"corpId\":\"" + CORP_ID + "\",\"token\":\"" + TOKEN
                                + "\",\"encodingAesKey\":\"" + AES_KEY_TEXT + "\"}"));

        assertThatThrownBy(() -> mockMvc.perform(post("/notice/inbound-callbacks/public")
                        .queryParam("channelConfigId", String.valueOf(CHANNEL_CONFIG_ID))
                        .queryParam("msg_signature", "invalid")
                        .queryParam("timestamp", timestamp)
                        .queryParam("nonce", nonce)
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml><Encrypt>" + encrypted + "</Encrypt></xml>")))
                .hasRootCauseInstanceOf(DefaultWecomInboundMessageAdapter.WecomInboundException.class)
                .hasMessageContaining("签名校验失败");

        verify(receiver, never()).receive(any());
    }

    @Test
    void mailPost_withoutRealInboundProvider_rejectsInsteadOfFixedSuccess() {
        when(configService.resolve(CHANNEL_CONFIG_ID, NoticeChannelType.EMAIL))
                .thenReturn(new NoticeInboundChannelConfigService.ResolvedInboundChannelConfig(
                        CHANNEL_CONFIG_ID, TENANT_ID, "IT_765_EMAIL", NoticeChannelType.EMAIL, "ALIYUN",
                        "{}"));

        assertThatThrownBy(() -> mockMvc.perform(post("/notice/inbound-mail-callbacks/public")
                        .queryParam("channelConfigId", String.valueOf(CHANNEL_CONFIG_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")))
                .hasMessageContaining("真实入站收件适配器");

        verify(receiver, never()).receive(any());
    }

    private String encrypt(String message) {
        try {
            byte[] random = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
            byte[] content = message.getBytes(StandardCharsets.UTF_8);
            byte[] receiver = CORP_ID.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(random.length + 4 + content.length + receiver.length);
            buffer.put(random).putInt(content.length).put(content).put(receiver);
            byte[] plain = buffer.array();
            int padding = 32 - plain.length % 32;
            byte[] padded = Arrays.copyOf(plain, plain.length + padding);
            Arrays.fill(padded, plain.length, padded.length, (byte) padding);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES_KEY, "AES"),
                    new IvParameterSpec(AES_KEY, 0, 16));
            return Base64.getEncoder().encodeToString(cipher.doFinal(padded));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private String signature(String encrypted, String timestamp, String nonce) {
        try {
            String[] parts = {TOKEN, timestamp, nonce, encrypted};
            Arrays.sort(parts);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(String.join("", parts).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
