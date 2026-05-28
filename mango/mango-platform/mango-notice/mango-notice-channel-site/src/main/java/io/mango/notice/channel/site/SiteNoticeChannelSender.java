package io.mango.notice.channel.site;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteNoticeChannelSender implements NoticeChannelSender {

    private final RealtimeApi realtimeApi;
    private final SiteNoticeMessageWriter messageWriter;

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.SITE;
    }

    @Override
    public ChannelSendResult send(ChannelSendCommand command) {
        Long messageId = messageWriter.write(command);
        try {
            realtimeApi.publishToUser(command.getUserId(), "notice",
                    "{\"messageId\":\"" + messageId + "\",\"title\":\"" + escape(command.getTitle())
                            + "\",\"bizType\":\"" + escape(command.getBizType()) + "\",\"priority\":\""
                            + command.getPriority().name() + "\"}");
        } catch (RuntimeException ex) {
            log.warn("Failed to publish site notice realtime message: {}", messageId, ex);
        }
        return ChannelSendResult.success(messageId);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
