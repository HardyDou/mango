package io.mango.notice.channel.wecom;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WecomNoticeChannelSender implements NoticeChannelSender {

    private static final Pattern WEBHOOK_URL = Pattern.compile("\"webhookUrl\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.WECOM;
    }

    @Override
    public ChannelSendResult send(ChannelSendCommand command) {
        if (StringUtils.hasText(readString(command.getChannelConfigJson(), WEBHOOK_URL))) {
            return ChannelSendResult.providerSuccess("wecom-robot-" + command.getSendRecordId(), "{\"status\":\"ACCEPTED\"}");
        }
        if (command.getWecomUserId() == null || command.getWecomUserId().isBlank()) {
            return ChannelSendResult.failed("WECOM_USER_EMPTY", "企业微信用户 ID 不能为空", false);
        }
        return ChannelSendResult.providerSuccess("wecom-app-" + command.getSendRecordId(), "{\"status\":\"ACCEPTED\"}");
    }

    private String readString(String value, Pattern pattern) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }
}
