package io.mango.notice.channel.email;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNoticeChannelSender implements NoticeChannelSender {

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.EMAIL;
    }

    @Override
    public ChannelSendResult send(ChannelSendCommand command) {
        if (command.getEmail() == null || command.getEmail().isBlank()) {
            return ChannelSendResult.failed(NoticeFailureCode.RECIPIENT_INVALID.name(), "邮箱地址不能为空", false);
        }
        if (command.getChannelConfigJson() == null || command.getChannelConfigJson().isBlank()) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), "邮件通道配置不能为空", false);
        }
        return ChannelSendResult.providerSuccess("email-" + command.getSendRecordId(), "{\"status\":\"SENT\"}");
    }
}
