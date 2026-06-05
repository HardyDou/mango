package io.mango.notice.channel.wecom;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WecomNoticeChannelSender implements NoticeChannelSender {

    private static final Pattern WEBHOOK_URL = stringPattern("webhookUrl");

    private final WecomAccessTokenProvider accessTokenProvider;
    private final WecomMessageClient messageClient;

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.WECOM;
    }

    @Override
    public ChannelSendResult send(ChannelSendCommand command) {
        if (StringUtils.hasText(readString(command.getChannelConfigJson(), WEBHOOK_URL))) {
            return ChannelSendResult.providerSuccess("wecom-robot-" + command.getSendRecordId(), "{\"status\":\"ACCEPTED\"}");
        }
        if (!StringUtils.hasText(command.getWecomUserId())) {
            return ChannelSendResult.failed("WECOM_USER_EMPTY", "企业微信用户 ID 不能为空", false);
        }
        WecomChannelConfig config = WecomChannelConfig.fromJson(command.getChannelConfigJson());
        if (!StringUtils.hasText(config.corpId())) {
            return ChannelSendResult.failed("WECOM_CORP_ID_EMPTY", "企业微信 CorpId 不能为空", false);
        }
        if (!StringUtils.hasText(config.secret())) {
            return ChannelSendResult.failed("WECOM_SECRET_EMPTY", "企业微信应用 Secret 不能为空", false);
        }
        if (!StringUtils.hasText(config.agentId())) {
            return ChannelSendResult.failed("WECOM_AGENT_ID_EMPTY", "企业微信应用 AgentId 不能为空", false);
        }
        if (!StringUtils.hasText(command.getContent())) {
            return ChannelSendResult.failed("WECOM_CONTENT_EMPTY", "企业微信消息内容不能为空", false);
        }
        try {
            int agentId = Integer.parseInt(config.agentId());
            String accessToken = accessTokenProvider.getAccessToken(config.corpId(), config.secret());
            WecomMessageSendResponse response = messageClient.sendText(accessToken, new WecomTextMessageRequest(
                    command.getWecomUserId(),
                    agentId,
                    buildContent(command)));
            String providerMessageId = StringUtils.hasText(response.messageId())
                    ? response.messageId()
                    : "wecom-app-" + command.getSendRecordId();
            return ChannelSendResult.providerSuccess(providerMessageId, response.rawResponse());
        } catch (NumberFormatException ex) {
            return ChannelSendResult.failed("WECOM_AGENT_ID_INVALID", "企业微信应用 AgentId 必须为数字", false);
        } catch (WecomApiException ex) {
            return ChannelSendResult.failed(ex.getFailCode(), ex.getFailReason(), ex.isRetryable());
        } catch (RuntimeException ex) {
            return ChannelSendResult.failed("WECOM_SEND_ERROR", "企业微信消息发送异常", true);
        }
    }

    private String buildContent(ChannelSendCommand command) {
        if (!StringUtils.hasText(command.getTitle())) {
            return command.getContent();
        }
        return command.getTitle() + "\n" + command.getContent();
    }

    private String readString(String value, Pattern pattern) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Pattern stringPattern(String key) {
        return Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
    }
}
