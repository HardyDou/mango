package io.mango.notice.channel.wechat.official;

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
public class WechatOfficialNoticeChannelSender implements NoticeChannelSender {

    private static final Pattern APP_ID = Pattern.compile("\"appId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern APP_SECRET = Pattern.compile("\"appSecret\"\\s*:\\s*\"([^\"]+)\"");

    private final WechatOfficialAccessTokenProvider accessTokenProvider;

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.WECHAT_OFFICIAL;
    }

    @Override
    public ChannelSendResult send(ChannelSendCommand command) {
        if (command.getWechatOpenid() == null || command.getWechatOpenid().isBlank()) {
            return ChannelSendResult.failed("OPENID_EMPTY", "微信公众号 openid 不能为空", false);
        }
        String appId = readString(command.getChannelConfigJson(), APP_ID);
        String appSecret = readString(command.getChannelConfigJson(), APP_SECRET);
        String token = accessTokenProvider.getAccessToken(appId, appSecret);
        if (!StringUtils.hasText(command.getChannelTemplateId())) {
            return ChannelSendResult.failed("TEMPLATE_ID_EMPTY", "微信公众号模板 ID 不能为空", false);
        }
        if (!StringUtils.hasText(command.getVariableMapping())) {
            return ChannelSendResult.failed("VARIABLE_MAPPING_EMPTY", "微信公众号模板变量映射不能为空", false);
        }
        return ChannelSendResult.providerSuccess("wechat-" + command.getSendRecordId(),
                "{\"status\":\"ACCEPTED\",\"token\":\"" + token + "\"}");
    }

    private String readString(String value, Pattern pattern) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }
}
