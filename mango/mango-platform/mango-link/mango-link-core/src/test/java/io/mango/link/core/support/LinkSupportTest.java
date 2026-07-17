package io.mango.link.core.support;

import io.mango.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkSupportTest {

    @Test
    void normalizesTagsWithoutChangingTheirFirstSeenOrder() {
        String stored = LinkSupport.joinTags(List.of(" 常用 ", "研发", "常用", ""));

        assertThat(stored).isEqualTo("常用,研发");
        assertThat(LinkSupport.splitTags(stored)).containsExactly("常用", "研发");
    }

    @Test
    void acceptsHttpUrlsAndRejectsUnsafeOrHostlessUrls() {
        assertThat(LinkSupport.normalizeUrl(" https://example.com/path "))
                .isEqualTo("https://example.com/path");
        assertThatThrownBy(() -> LinkSupport.normalizeUrl("javascript:alert(1)"))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> LinkSupport.normalizeUrl("/relative/path"))
                .isInstanceOf(BizException.class);
    }
}
