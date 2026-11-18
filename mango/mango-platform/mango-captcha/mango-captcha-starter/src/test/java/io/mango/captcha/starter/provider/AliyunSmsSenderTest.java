package io.mango.captcha.starter.provider;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import io.mango.captcha.api.enums.CaptchaCode;
import io.mango.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阿里云短信发送协作者单元测试。
 *
 * <p>替换的协作者：阿里云短信 SDK 客户端；被测目标 {@link AliyunSmsSender} 不做 mock，
 * 仅通过子类覆盖 {@code createClient} 注入 SDK 替身。</p>
 */
class AliyunSmsSenderTest {

    private static final String MOBILE = "13800138000";
    private static final String CODE = "123456";

    @Test
    void send_withOkResponse_returnsTrueAndMapsRequest() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSms(any())).thenReturn(okResponse());

        boolean sent = sender(client).send(message());

        assertTrue(sent);
        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).sendSms(captor.capture());
        SendSmsRequest request = captor.getValue();
        assertEquals(MOBILE, request.getPhoneNumbers());
        assertEquals("Mango 短信", request.getSignName());
        assertEquals("SMS_100001", request.getTemplateCode());
        assertEquals("{\"code\":\"" + CODE + "\"}", request.getTemplateParam());
    }

    @Test
    void send_withMobileKeptAsGiven_doesNotRewriteFormat() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSms(any())).thenReturn(okResponse());
        AliyunSmsMessage message = new AliyunSmsMessage(MOBILE, "Mango", "SMS_100001", CODE,
                "dysmsapi.aliyuncs.com", "cn-hangzhou", "ak-id", "ak-secret");

        sender(client).send(message);

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).sendSms(captor.capture());
        assertEquals(MOBILE, captor.getValue().getPhoneNumbers());
    }

    @Test
    void send_withNonOkCode_throwsSendFailed() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSms(any())).thenReturn(response("isv.BUSINESS_LIMIT_CONTROL", "触发流控"));

        assertSendFailed(() -> sender(client).send(message()));
    }

    @Test
    void send_withNullBody_throwsSendFailed() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSms(any())).thenReturn(null);

        assertSendFailed(() -> sender(client).send(message()));
    }

    @Test
    void send_whenSdkThrows_throwsSendFailed() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSms(any())).thenThrow(new IllegalStateException("sdk unavailable"));

        assertSendFailed(() -> sender(client).send(message()));
    }

    private static AliyunSmsMessage message() {
        return new AliyunSmsMessage(MOBILE, "Mango 短信", "SMS_100001", CODE,
                "dysmsapi.aliyuncs.com", "cn-hangzhou", "ak-id", "ak-secret");
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

    private static AliyunSmsSender sender(Client client) {
        return new AliyunSmsSender() {
            @Override
            protected Client createClient(AliyunSmsMessage message) {
                return client;
            }
        };
    }

    private static void assertSendFailed(Executable executable) {
        BizException exception = assertThrows(BizException.class, executable);
        assertEquals(CaptchaCode.CAPTCHA_SEND_FAILED.getCode(), exception.getCode());
    }
}
