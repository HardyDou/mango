package io.mango.notice.channel.wecom;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class WecomNoticeChannelSenderTest {

    private final WecomNoticeChannelSender sender = new WecomNoticeChannelSender(
            (corpId, corpSecret) -> "token-" + corpId,
            (accessToken, request) -> new WecomMessageSendResponse("{\"errcode\":0}", null));

    @Test
    void channelType_returnsWecom() {
        assertEquals(NoticeChannelType.WECOM, sender.channelType());
    }

    @Test
    void send_missingUserAndRobot_returnsNonRetryableFailure() {
        ChannelSendResult result = sender.send(new ChannelSendCommand());

        assertFalse(result.isSuccess());
        assertEquals("WECOM_USER_EMPTY", result.getFailCode());
        assertEquals("企业微信用户 ID 不能为空", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_missingConfig_returnsNonRetryableFailure() {
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(4001L);
        command.setWecomUserId("zhangsan");
        command.setContent("待办提醒");

        ChannelSendResult result = sender.send(command);

        assertFalse(result.isSuccess());
        assertEquals("WECOM_CORP_ID_EMPTY", result.getFailCode());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_withUserIdAndAppConfig_sendsAppTextMessage() {
        WecomNoticeChannelSender appSender = new WecomNoticeChannelSender(
                (corpId, corpSecret) -> {
                    assertEquals("ww-demo", corpId);
                    assertEquals("corp-secret", corpSecret);
                    return "access-token";
                },
                (accessToken, request) -> {
                    assertEquals("access-token", accessToken);
                    assertEquals("zhangsan", request.toUser());
                    assertEquals(1000003, request.agentId());
                    assertEquals("审批提醒\n请处理待办", request.content());
                    return new WecomMessageSendResponse("{\"errcode\":0,\"errmsg\":\"ok\",\"msgid\":\"msg-001\"}", "msg-001");
                });
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(4001L);
        command.setWecomUserId("zhangsan");
        command.setTitle("审批提醒");
        command.setContent("请处理待办");
        command.setChannelConfigJson("{\"corpId\":\"ww-demo\",\"agentId\":\"1000003\",\"secret\":\"corp-secret\"}");

        ChannelSendResult result = appSender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("msg-001", result.getProviderMessageId());
        assertEquals("{\"errcode\":0,\"errmsg\":\"ok\",\"msgid\":\"msg-001\"}", result.getResponseSnapshot());
    }

    @Test
    void send_withNumericAgentId_usesAppTextMessage() {
        WecomNoticeChannelSender appSender = new WecomNoticeChannelSender(
                (corpId, corpSecret) -> "access-token",
                (accessToken, request) -> {
                    assertEquals(1000003, request.agentId());
                    return new WecomMessageSendResponse("{\"errcode\":0}", null);
                });
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(4003L);
        command.setWecomUserId("zhangsan");
        command.setContent("请处理待办");
        command.setChannelConfigJson("{\"corpId\":\"ww-demo\",\"agentId\":1000003,\"corpSecret\":\"corp-secret\"}");

        ChannelSendResult result = appSender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("wecom-app-4003", result.getProviderMessageId());
    }

    @Test
    void send_whenProviderFails_mapsFailure() {
        WecomNoticeChannelSender appSender = new WecomNoticeChannelSender(
                (corpId, corpSecret) -> "access-token",
                (accessToken, request) -> {
                    throw new WecomApiException("WECOM_SEND_40003", "企业微信消息发送失败：invalid userid", false);
                });
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(4004L);
        command.setWecomUserId("unknown");
        command.setContent("请处理待办");
        command.setChannelConfigJson("{\"corpId\":\"ww-demo\",\"agentId\":\"1000003\",\"secret\":\"corp-secret\"}");

        ChannelSendResult result = appSender.send(command);

        assertFalse(result.isSuccess());
        assertEquals("WECOM_SEND_40003", result.getFailCode());
        assertEquals("企业微信消息发送失败：invalid userid", result.getFailReason());
        assertFalse(result.isRetryable());
    }

    @Test
    void send_withRobotWebhook_returnsRobotMessageSuccessWithoutUserId() {
        WecomNoticeChannelSender robotSender = new WecomNoticeChannelSender(
                (corpId, corpSecret) -> fail("robot webhook should not request access token"),
                (accessToken, request) -> fail("robot webhook should not send app message"));
        ChannelSendCommand command = new ChannelSendCommand();
        command.setSendRecordId(4002L);
        command.setChannelConfigJson("{\"webhookUrl\":\"https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test\"}");

        ChannelSendResult result = robotSender.send(command);

        assertTrue(result.isSuccess());
        assertEquals("wecom-robot-4002", result.getProviderMessageId());
    }
}
