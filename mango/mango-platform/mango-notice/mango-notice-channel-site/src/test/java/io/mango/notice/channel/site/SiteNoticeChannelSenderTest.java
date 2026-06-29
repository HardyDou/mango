package io.mango.notice.channel.site;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SiteNoticeChannelSenderTest {

    @Test
    void sendPublishesRealtimePayloadWithLatestUnreadCount() {
        TestRealtimeApi realtimeApi = new TestRealtimeApi();
        SiteNoticeChannelSender sender = new SiteNoticeChannelSender(realtimeApi,
                command -> new SiteNoticeMessageWriteResult(1001L, 3L));
        ChannelSendCommand command = new ChannelSendCommand();
        command.setUserId(8L);
        command.setTitle("新的审批");
        command.setBizType("WORKFLOW_APPROVED");
        command.setPriority(NoticePriority.HIGH);

        ChannelSendResult result = sender.send(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSiteMessageId()).isEqualTo(1001L);
        assertThat(realtimeApi.messages).hasSize(1);
        RealtimeOutboundMessage message = realtimeApi.messages.get(0);
        assertThat(message.userId()).isEqualTo(8L);
        assertThat(message.type()).isEqualTo("notice");
        assertThat(message.content())
                .contains("\"messageId\":\"1001\"")
                .contains("\"title\":\"新的审批\"")
                .contains("\"bizType\":\"WORKFLOW_APPROVED\"")
                .contains("\"priority\":\"HIGH\"")
                .contains("\"unreadCount\":3");
    }

    @Test
    void channelTypeReturnsSite() {
        SiteNoticeChannelSender sender = new SiteNoticeChannelSender(new TestRealtimeApi(),
                command -> new SiteNoticeMessageWriteResult(1001L, 1L));

        assertThat(sender.channelType()).isEqualTo(NoticeChannelType.SITE);
    }

    static class TestRealtimeApi implements RealtimeApi {

        private final List<RealtimeOutboundMessage> messages = new ArrayList<>();

        @Override
        public void publish(RealtimeOutboundMessage realtimeOutboundMessage) {
            messages.add(realtimeOutboundMessage);
        }
    }
}
