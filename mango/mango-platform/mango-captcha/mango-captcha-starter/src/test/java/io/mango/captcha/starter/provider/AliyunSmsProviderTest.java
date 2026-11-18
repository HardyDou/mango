package io.mango.captcha.starter.provider;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import io.mango.captcha.api.enums.CaptchaCode;
import io.mango.captcha.starter.properties.CaptchaProperties;
import io.mango.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阿里云短信供应商单元测试。
 *
 * <p>替换的协作者：阿里云短信 SDK 客户端；被测目标 {@link AliyunSmsProvider} 不做 mock，
 * 仅通过 {@link RecordingAliyunSender} 覆盖 {@code createClient} 注入 SDK 替身并记录发送参数。</p>
 */
class AliyunSmsProviderTest {

    private static final String MOBILE = "13800138000";
    private static final String TEMPLATE = "SMS_100001";
    private static final String CODE = "123456";

    private Client client;
    private RecordingAliyunSender sender;
    private AliyunSmsProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        client = mock(Client.class);
        when(client.sendSms(any())).thenReturn(okResponse());
        sender = new RecordingAliyunSender(client);
        provider = new AliyunSmsProvider(configuration(), sender);
    }

    @Test
    void send_mapsConfigurationAndCallerTemplateCodeToRequest() throws Exception {
        assertTrue(provider.send(MOBILE, TEMPLATE, CODE));

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).sendSms(captor.capture());
        SendSmsRequest request = captor.getValue();
        assertEquals(MOBILE, request.getPhoneNumbers());
        assertEquals("Mango 短信", request.getSignName());
        assertEquals(TEMPLATE, request.getTemplateCode());
        assertEquals("{\"code\":\"" + CODE + "\"}", request.getTemplateParam());
    }

    @Test
    void send_usesConfiguredEndpointRegionAndCredentials() throws Exception {
        assertTrue(provider.send(MOBILE, TEMPLATE, CODE));

        AliyunSmsMessage message = sender.lastMessage();
        assertEquals("dysmsapi.aliyuncs.com", message.endpoint());
        assertEquals("cn-hangzhou", message.region());
        assertEquals("ak-id", message.accessKeyId());
        assertEquals("ak-secret", message.accessKeySecret());
    }

    @Test
    void send_withBlankEndpointAndRegion_fallsBackToDefaults() throws Exception {
        CaptchaProperties.Sms sms = configuration();
        sms.getAliyun().setEndpoint("  ");
        sms.getAliyun().setRegion(null);
        AliyunSmsProvider fallbackProvider = new AliyunSmsProvider(sms, sender);

        assertTrue(fallbackProvider.send(MOBILE, TEMPLATE, CODE));

        AliyunSmsMessage message = sender.lastMessage();
        assertEquals(AliyunSmsSender.DEFAULT_ENDPOINT, message.endpoint());
        assertEquals("cn-hangzhou", message.region());
    }

    @Test
    void send_withNonOkResponseCode_throwsSendFailed() throws Exception {
        when(client.sendSms(any())).thenReturn(response("isv.BUSINESS_LIMIT_CONTROL", "触发流控"));

        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void send_withNullResponseBody_throwsSendFailed() throws Exception {
        when(client.sendSms(any())).thenReturn(null);

        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void send_whenSdkThrows_throwsSendFailed() throws Exception {
        when(client.sendSms(any())).thenThrow(new IllegalStateException("sdk unavailable"));

        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void send_withBlankTemplateCode_throwsSendFailedWithoutCallingSdk() throws Exception {
        assertSendFailed(() -> provider.send(MOBILE, null, CODE));
        assertSendFailed(() -> provider.send(MOBILE, "  ", CODE));
        verify(client, never()).sendSms(any());
    }

    @Test
    void send_withBlankMobile_throwsSendFailed() {
        assertSendFailed(() -> provider.send("  ", TEMPLATE, CODE));
    }

    @Test
    void send_withoutTemplateParam_throwsSendFailed() {
        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE));
        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE, " "));
    }

    @Test
    void send_withoutAccessKey_throwsSendFailed() {
        CaptchaProperties.Sms sms = configuration();
        sms.getAliyun().setAccessKeyId(null);
        AliyunSmsProvider misconfigured = new AliyunSmsProvider(sms, sender);

        assertSendFailed(() -> misconfigured.send(MOBILE, TEMPLATE, CODE));
        assertNull(sender.lastMessage());
    }

    @Test
    void send_withoutSignName_throwsSendFailed() {
        CaptchaProperties.Sms sms = configuration();
        sms.getAliyun().setSignName("");
        AliyunSmsProvider misconfigured = new AliyunSmsProvider(sms, sender);

        assertSendFailed(() -> misconfigured.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void getName_returnsAliyun() {
        assertEquals("aliyun", provider.getName());
    }

    private static CaptchaProperties.Sms configuration() {
        CaptchaProperties.Sms sms = new CaptchaProperties.Sms();
        sms.setProvider("aliyun");
        sms.setTemplateCode(TEMPLATE);
        sms.getAliyun().setAccessKeyId("ak-id");
        sms.getAliyun().setAccessKeySecret("ak-secret");
        sms.getAliyun().setSignName("Mango 短信");
        return sms;
    }

    private static SendSmsResponse okResponse() {
        return response("OK", "OK");
    }

    private static SendSmsResponse response(String code, String message) {
        return new SendSmsResponse()
                .setBody(new SendSmsResponseBody()
                        .setCode(code)
                        .setMessage(message)
                        .setBizId("biz-id")
                        .setRequestId("request-id"));
    }

    private static void assertSendFailed(Executable executable) {
        BizException exception = assertThrows(BizException.class, executable);
        assertEquals(CaptchaCode.CAPTCHA_SEND_FAILED.getCode(), exception.getCode());
    }

    /**
     * 通过子类覆盖 {@code createClient} 注入 SDK 替身，并记录供应商传入的发送参数。
     */
    private static final class RecordingAliyunSender extends AliyunSmsSender {

        private final Client client;
        private AliyunSmsMessage message;

        private RecordingAliyunSender(Client client) {
            this.client = client;
        }

        @Override
        protected Client createClient(AliyunSmsMessage message) {
            this.message = message;
            return client;
        }

        private AliyunSmsMessage lastMessage() {
            return message;
        }
    }
}
