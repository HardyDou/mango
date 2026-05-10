package io.mango.common.result;

import io.mango.common.exception.BizException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RequireTest {

    @Test
    void testNotNull() {
        Require.notNull("not null", "should not throw");
        assertThatThrownBy(() -> Require.notNull(null, "is null"))
                .isInstanceOf(BizException.class)
                .hasMessage("is null")
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    void testIsTrue() {
        Require.isTrue(true, "should not throw");
        assertThatThrownBy(() -> Require.isTrue(false, "is false"))
                .isInstanceOf(BizException.class)
                .hasMessage("is false")
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    void testNotBlank() {
        Require.notBlank("abc", "should not throw");
        assertThatThrownBy(() -> Require.notBlank("  ", "is blank"))
                .isInstanceOf(BizException.class)
                .hasMessage("is blank")
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    void testFail() {
        assertThatThrownBy(() -> Require.fail(CommonCode.NOT_FOUND))
                .isInstanceOf(BizException.class)
                .hasMessage("资源不存在")
                .extracting("code")
                .isEqualTo(404);
    }
}
