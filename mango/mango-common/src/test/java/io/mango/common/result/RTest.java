package io.mango.common.result;

import io.mango.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RTest {

    @Test
    void testOk() {
        R<String> r = R.ok("hello");
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getData()).isEqualTo("hello");
        assertThat(r.getMsg()).isEqualTo("操作成功");
    }

    @Test
    void testFail() {
        R<Void> r = R.fail(400, "error");
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMsg()).isEqualTo("error");
    }

    @Test
    void testFailWithBizCode() {
        R<Void> r = R.fail(CommonCode.BAD_REQUEST);
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMsg()).isEqualTo("参数校验失败");
    }

    @Test
    void unwrapReturnsSuccessfulPayload() {
        assertThat(R.ok("hello").unwrap(CommonCode.BAD_REQUEST, "读取失败"))
                .isEqualTo("hello");
    }

    @Test
    void unwrapUsesRemoteFailureMessage() {
        R<String> response = R.fail(503, "文件服务不可用");

        assertThatThrownBy(() -> response.unwrap(CommonCode.SERVER_ERROR, "读取文件失败"))
                .isInstanceOf(BizException.class)
                .hasMessage("文件服务不可用");
    }

    @Test
    void unwrapUsesFallbackWhenRemoteFailureMessageIsBlank() {
        R<String> response = R.fail(503, " ");

        assertThatThrownBy(() -> response.unwrap(CommonCode.SERVER_ERROR, "读取文件失败"))
                .isInstanceOf(BizException.class)
                .hasMessage("读取文件失败");
    }

    @Test
    void unwrapRejectsSuccessfulResponseWithoutPayload() {
        assertThatThrownBy(() -> R.ok().unwrap(CommonCode.NOT_FOUND, "数据为空"))
                .isInstanceOf(BizException.class)
                .hasMessage("数据为空");
    }
}
