package io.mango.notice.support.channel;

import io.mango.notice.api.enums.NoticeChannelType;

public interface NoticeChannelSender {

    NoticeChannelType channelType();

    ChannelSendResult send(ChannelSendCommand command);
}
