package io.mango.notice.support.channel;

import io.mango.notice.api.InboundNoticeMessage;

/** One mailbox message and the protocol cursor that becomes safe after it is accepted. */
public record NoticeInboundMailItem(
        InboundNoticeMessage message,
        String cursorValue,
        String cursorVersion) {
}
