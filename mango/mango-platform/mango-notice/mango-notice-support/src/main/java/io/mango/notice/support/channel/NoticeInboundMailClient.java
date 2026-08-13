package io.mango.notice.support.channel;

import io.mango.notice.api.enums.NoticeInboundProtocol;
import java.util.List;

/** Protocol-neutral mailbox client contract. */
public interface NoticeInboundMailClient {

    boolean supports(NoticeInboundProtocol protocol);

    List<NoticeInboundMailItem> fetch(
            NoticeInboundMailAccount account, String cursorValue, String cursorVersion);
}
