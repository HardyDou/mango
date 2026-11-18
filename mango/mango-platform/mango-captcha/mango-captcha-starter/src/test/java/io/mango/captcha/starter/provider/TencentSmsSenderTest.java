package io.mango.captcha.starter.provider;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import io.mango.captcha.api.enums.CaptchaCode;
import io.mango.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 腾讯云短信发送协作者单元测试。
 *
 * <p>替换的协作者：腾讯云短信 SDK 客户端；被测目标 {@link TencentSmsSender} 不做 mock，
 * 仅通过子类覆盖 {@code createClient} 注入 SDK 替身。</p>
 */
class TencentSmsSenderTest {

    private static final String MOBILE = "13800138000";
    private static final String CODE = "654321";

    @Test
    void send_withOkStatus_returnsTrueAndMapsRequest() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenReturn(okResponse());

        boolean sent = sender(client).send(message());

        assertTrue(sent);
        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).SendSms(captor.capture());
        SendSmsRequest request = captor.getValue();
        assertArrayEquals(new String[]{"+8613800138000"}, request.getPhoneNumberSet());
        assertEquals("Mango", request.getSignName());
        assertEquals("1400000000", request.getSmsSdkAppId());
        assertEquals("100001", request.getTemplateId());
        assertArrayEquals(new String[]{CODE}, request.getTemplateParamSet());
    }

    @Test
    void send_withConfiguredCountryCode_prefixesMobile() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenReturn(okResponse());
        TencentSmsMessage message = new TencentSmsMessage(MOBILE, "+852", "Mango",
                "1400000000", "100001", CODE, "sms.tencentcloudapi.com", "ap-guangzhou", "sid", "skey");

        sender(client).send(message);

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).SendSms(captor.capture());
        assertArrayEquals(new String[]{"+85213800138000"}, captor.getValue().getPhoneNumberSet());
    }

    @Test
    void send_withNonOkStatus_throwsSendFailed() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenReturn(response("FailedOperation.PhoneNumberInBlacklist", "号码在黑名单"));

        assertSendFailed(() -> sender(client).send(message()));
    }

    @Test
    void send_withEmptyStatusSet_throwsSendFailed() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenReturn(new SendSmsResponse());

        assertSendFailed(() -> sender(client).send(message()));
    }

    @Test
    void send_whenSdkThrows_throwsSendFailed() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenThrow(new TencentCloudSDKException("sdk unavailable"));

        assertSendFailed(() -> sender(client).send(message()));
    }

    private static TencentSmsMessage message() {
        return new TencentSmsMessage(MOBILE, "+86", "Mango", "1400000000", "100001",
                CODE, "sms.tencentcloudapi.com", "ap-guangzhou", "sid", "skey");
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

    private static TencentSmsSender sender(SmsClient client) {
        return new TencentSmsSender() {
            @Override
            protected SmsClient createClient(TencentSmsMessage message) {
                return client;
            }
        };
    }

    private static void assertSendFailed(Executable executable) {
        BizException exception = assertThrows(BizException.class, executable);
        assertEquals(CaptchaCode.CAPTCHA_SEND_FAILED.getCode(), exception.getCode());
    }
}
