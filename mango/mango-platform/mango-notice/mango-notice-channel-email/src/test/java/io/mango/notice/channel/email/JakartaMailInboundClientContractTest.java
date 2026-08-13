package io.mango.notice.channel.email;

import io.mango.notice.api.enums.NoticeInboundProtocol;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JakartaMailInboundClientContractTest {

    private final JakartaMailInboundClient client = new JakartaMailInboundClient();

    @Test
    void supportsOnlyConfiguredMailboxProtocols() {
        assertThat(client.supports(NoticeInboundProtocol.IMAP)).isTrue();
        assertThat(client.supports(NoticeInboundProtocol.POP3)).isTrue();
    }
}
