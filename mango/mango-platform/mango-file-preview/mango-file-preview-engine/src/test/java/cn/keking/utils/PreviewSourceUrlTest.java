package cn.keking.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreviewSourceUrlTest {

    @Test
    void build_生成fileid协议地址() {
        assertThat(PreviewSourceUrl.build("token-1")).isEqualTo("fileid://token-1");
    }

    @Test
    void build_缺少源标识时拒绝() {
        assertThatThrownBy(() -> PreviewSourceUrl.build(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("源标识");
    }

    @Test
    void isSourceUrl_识别大小写与查询串() {
        assertThat(PreviewSourceUrl.isSourceUrl("fileid://token-1")).isTrue();
        assertThat(PreviewSourceUrl.isSourceUrl("FILEID://token-1?fullfilename=a.txt")).isTrue();
        assertThat(PreviewSourceUrl.isSourceUrl("file:///tmp/a.txt")).isFalse();
        assertThat(PreviewSourceUrl.isSourceUrl("https://example.com/a.txt")).isFalse();
        assertThat(PreviewSourceUrl.isSourceUrl(null)).isFalse();
    }

    @Test
    void sourceId_忽略查询串与片段() {
        assertThat(PreviewSourceUrl.sourceId("fileid://token-1")).isEqualTo("token-1");
        assertThat(PreviewSourceUrl.sourceId("fileid://token-1?fullfilename=a.txt")).isEqualTo("token-1");
        assertThat(PreviewSourceUrl.sourceId("fileid://token-1?a=1#frag")).isEqualTo("token-1");
    }

    @Test
    void sourceId_非fileid地址与空标识都拒绝() {
        assertThatThrownBy(() -> PreviewSourceUrl.sourceId("https://example.com/a.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PreviewSourceUrl.sourceId("fileid://?fullfilename=a.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("源标识");
    }

    @Test
    void isValidUrl_接受fileid协议() {
        assertThat(WebUtils.isValidUrl("fileid://token-1")).isTrue();
    }
}
