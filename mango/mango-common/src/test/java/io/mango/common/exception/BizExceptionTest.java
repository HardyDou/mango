package io.mango.common.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class BizExceptionTest {

    @Test
    void testBizException() {
        BizException e = new BizException(500, "error");
        assertThat(e.getCode()).isEqualTo(500);
        assertThat(e.getMessage()).isEqualTo("error");
    }

    @Test
    void testDefaultCode() {
        BizException e = new BizException("default error");
        assertThat(e.getCode()).isEqualTo(400);
        assertThat(e.getMessage()).isEqualTo("default error");
    }
}
