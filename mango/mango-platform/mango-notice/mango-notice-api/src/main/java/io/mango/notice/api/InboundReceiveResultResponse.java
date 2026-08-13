package io.mango.notice.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

/** Result of accepting an inbound message. */
@Getter
@AllArgsConstructor
public class InboundReceiveResultResponse {

    @Schema(description = "消息 ID") private final Long messageId;
    @Schema(description = "事件 ID") private final String eventId;
    @Schema(description = "是否重复") private final boolean duplicate;
    @Schema(description = "是否接受") private final boolean accepted;

    public Long messageId() {
        return messageId;
    }

    public String eventId() {
        return eventId;
    }

    public boolean duplicate() {
        return duplicate;
    }

    public boolean accepted() {
        return accepted;
    }
}
