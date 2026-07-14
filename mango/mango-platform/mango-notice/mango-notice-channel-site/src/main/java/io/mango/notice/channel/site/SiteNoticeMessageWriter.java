package io.mango.notice.channel.site;

import io.mango.notice.support.channel.NoticeChannelMessage;

public interface SiteNoticeMessageWriter {

    SiteNoticeMessageWriteResult write(NoticeChannelMessage message);
}
