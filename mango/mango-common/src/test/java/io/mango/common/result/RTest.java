package io.mango.common.result;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

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
}
