package io.mango.notice.channel.sms;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.ChannelSendCommand;
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

        ChannelSendResult result = sender.send(new ChannelSendCommand());

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.RECIPIENT_INVALID.name(), result.getFailCode());
        assertEquals("手机号不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_validAliyunConfig_sendsWithTemplateParams() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(request -> {
            assertEquals("13800138000", request.mobile());
            assertEquals("芒果", request.signName());
            assertEquals("SMS_10001", request.templateCode());
            assertEquals("{\"code\":\"123456\",\"product\":\"Mango\"}", request.templateParam());
            assertEquals("ak", request.config().accessKeyId());
            assertEquals("sk", request.config().accessKeySecret());
            assertEquals("dysmsapi.aliyuncs.com", request.config().endpoint());
            return SmsGatewayResponse.success("biz-1001",
                    "{\"provider\":\"ALIYUN\",\"code\":\"OK\",\"bizId\":\"biz-1001\"}");
        });
        ChannelSendCommand command = new ChannelSendCommand();
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
    void send_missingTemplateCode_returnsNonRetryableFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway());
        ChannelSendCommand command = new ChannelSendCommand();
        command.setMobile("13800138000");
        command.setChannelConfigJson("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\",\"signName\":\"芒果\"}");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), result.getFailCode());
        assertEquals("阿里云短信模板 Code 不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_whenProviderRejects_mapsFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(request -> SmsGatewayResponse.failed(
                "ALIYUN_SMS_isv.INVALID_PARAMETERS", "阿里云短信发送失败：参数非法", false,
                "{\"provider\":\"ALIYUN\",\"code\":\"isv.INVALID_PARAMETERS\"}"));
        ChannelSendCommand command = new ChannelSendCommand();
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
    void send_missingMappedParam_returnsNonRetryableFailure() {
        SmsNoticeChannelSender sender = new SmsNoticeChannelSender(successGateway());
        ChannelSendCommand command = new ChannelSendCommand();
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
        return request -> SmsGatewayResponse.success("biz-1001", "{\"provider\":\"ALIYUN\",\"code\":\"OK\"}");
    }
}
