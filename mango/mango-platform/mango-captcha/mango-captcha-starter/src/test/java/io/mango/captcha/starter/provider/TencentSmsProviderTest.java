package io.mango.captcha.starter.provider;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import io.mango.captcha.api.enums.CaptchaCode;
import io.mango.captcha.starter.properties.CaptchaProperties;
import io.mango.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
 * 腾讯云短信供应商单元测试。
 *
 * <p>替换的协作者：腾讯云短信 SDK 客户端；被测目标 {@link TencentSmsProvider} 不做 mock，
 * 仅通过 {@link RecordingTencentSender} 覆盖 {@code createClient} 注入 SDK 替身并记录发送参数。</p>
 */
class TencentSmsProviderTest {

    private static final String MOBILE = "13800138000";
    private static final String TEMPLATE = "100001";
    private static final String CODE = "654321";

    private SmsClient client;
    private RecordingTencentSender sender;
    private TencentSmsProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        client = mock(SmsClient.class);
        when(client.SendSms(any())).thenReturn(okResponse());
        sender = new RecordingTencentSender(client);
        provider = new TencentSmsProvider(configuration(), sender);
    }

    @Test
    void send_mapsConfigurationAndCallerTemplateCodeToRequest() throws Exception {
        assertTrue(provider.send(MOBILE, TEMPLATE, CODE));

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).SendSms(captor.capture());
        SendSmsRequest request = captor.getValue();
        assertArrayEquals(new String[]{"+8613800138000"}, request.getPhoneNumberSet());
        assertEquals("Mango", request.getSignName());
        assertEquals("1400000000", request.getSmsSdkAppId());
        assertEquals(TEMPLATE, request.getTemplateId());
        assertArrayEquals(new String[]{CODE}, request.getTemplateParamSet());
    }

    @Test
    void send_usesConfiguredEndpointRegionAndCredentials() throws Exception {
        assertTrue(provider.send(MOBILE, TEMPLATE, CODE));

        TencentSmsMessage message = sender.lastMessage();
        assertEquals("sms.tencentcloudapi.com", message.endpoint());
        assertEquals("ap-guangzhou", message.region());
        assertEquals("sid", message.secretId());
        assertEquals("skey", message.secretKey());
        assertEquals("+86", message.countryCode());
    }

    @Test
    void send_withCountryCodeWithoutPlus_prefixesPlus() throws Exception {
        CaptchaProperties.Sms sms = configuration();
        sms.getTencent().setCountryCode("852");
        TencentSmsProvider padded = new TencentSmsProvider(sms, sender);

        assertTrue(padded.send(MOBILE, TEMPLATE, CODE));

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).SendSms(captor.capture());
        assertArrayEquals(new String[]{"+85213800138000"}, captor.getValue().getPhoneNumberSet());
    }

    @Test
    void send_withBlankEndpointRegionCountryCode_fallsBackToDefaults() throws Exception {
        CaptchaProperties.Sms sms = configuration();
        sms.getTencent().setEndpoint("");
        sms.getTencent().setRegion(null);
        sms.getTencent().setCountryCode(" ");
        TencentSmsProvider fallbackProvider = new TencentSmsProvider(sms, sender);

        assertTrue(fallbackProvider.send(MOBILE, TEMPLATE, CODE));

        TencentSmsMessage message = sender.lastMessage();
        assertEquals(TencentSmsSender.DEFAULT_ENDPOINT, message.endpoint());
        assertEquals("ap-guangzhou", message.region());
        assertEquals("+86", message.countryCode());
    }

    @Test
    void send_withInvalidCountryCode_throwsSendFailed() {
        CaptchaProperties.Sms sms = configuration();
        sms.getTencent().setCountryCode("86abc");
        TencentSmsProvider invalid = new TencentSmsProvider(sms, sender);

        assertSendFailed(() -> invalid.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void send_withNonOkStatus_throwsSendFailed() throws Exception {
        when(client.SendSms(any())).thenReturn(response("FailedOperation.PhoneNumberInBlacklist", "号码在黑名单"));

        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void send_withEmptyStatusSet_throwsSendFailed() throws Exception {
        when(client.SendSms(any())).thenReturn(new SendSmsResponse());

        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void send_whenSdkThrows_throwsSendFailed() throws Exception {
        when(client.SendSms(any())).thenThrow(new TencentCloudSDKException("sdk unavailable"));

        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void send_withBlankTemplateCode_throwsSendFailedWithoutCallingSdk() throws Exception {
        assertSendFailed(() -> provider.send(MOBILE, null, CODE));
        assertSendFailed(() -> provider.send(MOBILE, "", CODE));
        verify(client, never()).SendSms(any());
    }

    @Test
    void send_withBlankMobile_throwsSendFailed() {
        assertSendFailed(() -> provider.send(null, TEMPLATE, CODE));
    }

    @Test
    void send_withoutTemplateParam_throwsSendFailed() {
        assertSendFailed(() -> provider.send(MOBILE, TEMPLATE));
    }

    @Test
    void send_withoutSecretId_throwsSendFailed() {
        CaptchaProperties.Sms sms = configuration();
        sms.getTencent().setSecretId(null);
        TencentSmsProvider misconfigured = new TencentSmsProvider(sms, sender);

        assertSendFailed(() -> misconfigured.send(MOBILE, TEMPLATE, CODE));
        assertNull(sender.lastMessage());
    }

    @Test
    void send_withoutSdkAppId_throwsSendFailed() {
        CaptchaProperties.Sms sms = configuration();
        sms.getTencent().setSdkAppId("  ");
        TencentSmsProvider misconfigured = new TencentSmsProvider(sms, sender);

        assertSendFailed(() -> misconfigured.send(MOBILE, TEMPLATE, CODE));
    }

    @Test
    void getName_returnsTencent() {
        assertEquals("tencent", provider.getName());
    }

    private static CaptchaProperties.Sms configuration() {
        CaptchaProperties.Sms sms = new CaptchaProperties.Sms();
        sms.setProvider("tencent");
        sms.setTemplateCode(TEMPLATE);
        sms.getTencent().setSecretId("sid");
        sms.getTencent().setSecretKey("skey");
        sms.getTencent().setSignName("Mango");
        sms.getTencent().setSdkAppId("1400000000");
        return sms;
    }

    private static SendSmsResponse okResponse() {
        return response("Ok", "send success");
    }

    private static SendSmsResponse response(String code, String message) {
        SendStatus status = new SendStatus();
        status.setCode(code);
        status.setMessage(message);
        status.setSerialNo("serial-no");
        SendSmsResponse response = new SendSmsResponse();
        response.setSendStatusSet(new SendStatus[]{status});
        return response;
    }

    private static void assertSendFailed(Executable executable) {
        BizException exception = assertThrows(BizException.class, executable);
        assertEquals(CaptchaCode.CAPTCHA_SEND_FAILED.getCode(), exception.getCode());
    }

    /**
     * 通过子类覆盖 {@code createClient} 注入 SDK 替身，并记录供应商传入的发送参数。
     */
    private static final class RecordingTencentSender extends TencentSmsSender {

        private final SmsClient client;
        private TencentSmsMessage message;

        private RecordingTencentSender(SmsClient client) {
            this.client = client;
        }

        @Override
        protected SmsClient createClient(TencentSmsMessage message) {
            this.message = message;
            return client;
        }

        private TencentSmsMessage lastMessage() {
            return message;
        }
    }
}
