package io.mango.notice.channel.sms;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.NoticeChannelMessage;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsNoticeChannelSenderTest {

    @Test
    void channelType_returnsSms() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway());

        assertEquals(NoticeChannelType.SMS, sender.channelType());
    }

    @Test
    void send_missingMobile_returnsNonRetryableFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway());

        ChannelSendResult result = sender.send(new NoticeChannelMessage());

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.RECIPIENT_INVALID.name(), result.getFailCode());
        assertEquals("手机号不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_validAliyunConfig_sendsWithTemplateParams() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(request -> {
            AliyunSmsConfig config = (AliyunSmsConfig) request.config();
            assertEquals("13800138000", request.mobile());
            assertEquals("芒果", request.signName());
            assertEquals("SMS_10001", request.templateCode());
            assertEquals("{\"code\":\"123456\",\"product\":\"Mango\"}", request.templateParam());
            assertEquals("ak", config.accessKeyId());
            assertEquals("sk", config.accessKeySecret());
            assertEquals("dysmsapi.aliyuncs.com", config.endpoint());
            return SmsGatewayResult.success("biz-1001",
                    "{\"provider\":\"ALIYUN\",\"code\":\"OK\",\"bizId\":\"biz-1001\"}");
        });
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setSendRecordId(1001L);
        command.setMobile("13800138000");
        command.setChannelConfigJson("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\",\"signName\":\"芒果\"}");
        command.setChannelTemplateId("SMS_10001");
        command.setVariableMapping("{\"code\":\"captchaCode\",\"product\":\"productName\"}");
        command.setParams(Map.of("captchaCode", "123456", "productName", "Mango"));

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("biz-1001", result.getProviderMessageId());
        assertEquals("{\"provider\":\"ALIYUN\",\"code\":\"OK\",\"bizId\":\"biz-1001\"}", result.getResponseSnapshot());
    }

    @Test
    void send_validTencentConfig_sendsWithTemplateParams() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway(), request -> {
            TencentSmsConfig config = (TencentSmsConfig) request.config();
            assertEquals("13800138000", request.mobile());
            assertEquals("芒果科技", request.signName());
            assertEquals("498790758", request.templateCode());
            assertEquals("{\"code\":\"654321\",\"product\":\"Mango\"}", request.templateParam());
            assertEquals("sid", config.secretId());
            assertEquals("skey", config.secretKey());
            assertEquals("1400000001", config.smsSdkAppId());
            assertEquals("ap-guangzhou", config.region());
            assertEquals("sms.tencentcloudapi.com", config.endpoint());
            assertEquals("+86", config.countryCode());
            return SmsGatewayResult.success("tencent-1001",
                    "{\"provider\":\"TENCENT\",\"code\":\"Ok\",\"serialNo\":\"tencent-1001\"}");
        });
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setSendRecordId(1001L);
        command.setMobile("13800138000");
        command.setChannelProviderCode("TENCENT_SMS");
        command.setChannelConfigJson("""
                {"secretId":"sid","secretKey":"skey","smsSdkAppId":"1400000001","signName":"芒果科技"}
                """);
        command.setChannelTemplateId("498790758");
        command.setVariableMapping("{\"code\":\"captchaCode\",\"product\":\"productName\"}");
        command.setParams(Map.of("captchaCode", "654321", "productName", "Mango"));

        ChannelSendResult result = sender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("tencent-1001", result.getProviderMessageId());
        assertEquals("{\"provider\":\"TENCENT\",\"code\":\"Ok\",\"serialNo\":\"tencent-1001\"}",
                result.getResponseSnapshot());
    }

    @Test
    void send_missingTemplateCode_returnsNonRetryableFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway());
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setMobile("13800138000");
        command.setChannelConfigJson("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\",\"signName\":\"芒果\"}");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), result.getFailCode());
        assertEquals("阿里云短信模板 Code 不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_tencentMissingSmsSdkAppId_returnsNonRetryableFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway(), successGateway());
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setMobile("13800138000");
        command.setChannelProviderCode("TENCENT_SMS");
        command.setChannelConfigJson("{\"secretId\":\"sid\",\"secretKey\":\"skey\",\"signName\":\"芒果\"}");
        command.setChannelTemplateId("498790758");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), result.getFailCode());
        assertEquals("腾讯云 SmsSdkAppId 不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_whenProviderRejects_mapsFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(request -> SmsGatewayResult.failed(
                "ALIYUN_SMS_isv.INVALID_PARAMETERS", "阿里云短信发送失败：参数非法", false,
                "{\"provider\":\"ALIYUN\",\"code\":\"isv.INVALID_PARAMETERS\"}"));
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setMobile("13800138000");
        command.setChannelConfigJson("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\",\"signName\":\"芒果\"}");
        command.setChannelTemplateId("SMS_10001");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals("ALIYUN_SMS_isv.INVALID_PARAMETERS", result.getFailCode());
        assertEquals("阿里云短信发送失败：参数非法", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_whenTencentProviderRejects_mapsFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway(), request -> SmsGatewayResult.failed(
                "TENCENT_SMS_FailedOperation.TemplateIncorrect", "腾讯云短信发送失败：模板错误", false,
                "{\"provider\":\"TENCENT\",\"code\":\"FailedOperation.TemplateIncorrect\"}"));
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setMobile("13800138000");
        command.setChannelProviderCode("TENCENT_SMS");
        command.setChannelConfigJson(
                "{\"secretId\":\"sid\",\"secretKey\":\"skey\",\"smsSdkAppId\":\"1400000001\",\"signName\":\"芒果\"}");
        command.setChannelTemplateId("498790758");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals("TENCENT_SMS_FailedOperation.TemplateIncorrect", result.getFailCode());
        assertEquals("腾讯云短信发送失败：模板错误", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_missingMappedParam_returnsNonRetryableFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway());
        NoticeChannelMessage command = new NoticeChannelMessage();
        command.setMobile("13800138000");
        command.setChannelConfigJson("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\",\"signName\":\"芒果\"}");
        command.setChannelTemplateId("SMS_10001");
        command.setVariableMapping("{\"code\":\"captchaCode\"}");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals("SMS_TEMPLATE_PARAM_INVALID", result.getFailCode());
        assertEquals("短信模板参数缺失：captchaCode", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    private SmsGateway successGateway() {
        return request -> SmsGatewayResult.success("biz-1001", "{\"provider\":\"ALIYUN\",\"code\":\"OK\"}");
    }
}
