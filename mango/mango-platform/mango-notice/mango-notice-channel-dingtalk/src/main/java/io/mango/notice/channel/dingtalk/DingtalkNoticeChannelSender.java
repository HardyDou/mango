package io.mango.notice.channel.dingtalk;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DingtalkNoticeChannelSender implements NoticeChannelSender {

 private static final Pattern WEBHOOK_URL = Pattern.compile("\"webhookUrl\"\\s*:\\s*\"([^\"]+)\"");

 @Override
 public NoticeChannelType channelType() {
 return NoticeChannelType.DINGTALK;
 }

 @Override
 public ChannelSendResult send(ChannelSendCommand command) {
 if (StringUtils.hasText(readString(command.getChannelConfigJson(), WEBHOOK_URL))) {
 return ChannelSendResult.providerSuccess("dingtalk-robot-" + command.getSendRecordId(), "{\"status\":\"ACCEPTED\"}");
 }
 if (command.getDingtalkUserId() == null || command.getDingtalkUserId().isBlank()) {
 return ChannelSendResult.failed("DINGTALK_USER_EMPTY", "钉钉用户 ID 不能为空", false);
 }
 return ChannelSendResult.providerSuccess("dingtalk-work-" + command.getSendRecordId(), "{\"status\":\"ACCEPTED\"}");
 }

 private String readString(String value, Pattern pattern) {
 if (!StringUtils.hasText(value)) {
 return null;
 }
 Matcher matcher = pattern.matcher(value);
 return matcher.find() ? matcher.group(1) : null;
 }
}
