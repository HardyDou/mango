package io.mango.notice.channel.sms;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import org.springframework.stereotype.Component;

@Component
public class SmsNoticeChannelSender implements NoticeChannelSender {

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.SMS;
    }

    @Override
    public ChannelSendResult send(ChannelSendCommand command) {
        if (command.getMobile() == null || command.getMobile().isBlank()) {
            return ChannelSendResult.failed(NoticeFailureCode.RECIPIENT_INVALID.name(), "手机号不能为空", false);
        }
        if (command.getChannelConfigJson() == null || command.getChannelConfigJson().isBlank()) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), "短信通道配置不能为空", false);
        }
        return ChannelSendResult.providerSuccess("sms-" + command.getSendRecordId(), "{\"status\":\"ACCEPTED\"}");
    }
}
