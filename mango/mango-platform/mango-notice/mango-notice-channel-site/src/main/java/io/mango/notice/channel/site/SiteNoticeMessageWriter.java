package io.mango.notice.channel.site;

import io.mango.notice.support.channel.ChannelSendCommand;

public interface SiteNoticeMessageWriter {

    SiteNoticeMessageWriteResult write(ChannelSendCommand command);
}
