package io.mango.notice.api.event;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticePriority;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Event used by business modules to request notice sending without coupling the
 * business transaction to notice delivery.
 */
@Value
@Builder
public class NoticeSendEvent {

    String bizType;

    String bizId;

    Long userId;

    List<Long> userIds;

    String recipientRuleCode;

    @Singular
    List<NoticeChannelType> channelTypes;

    @Singular
    Map<String, Object> params;

    NoticePriority priority;

    String idempotentKey;
}
