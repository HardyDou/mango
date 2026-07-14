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

    @Test
    void specializedAssertionsAcceptBizCode() {
        assertThatThrownBy(() -> Require.notEmpty("", CommonCode.BAD_REQUEST))
                .isInstanceOf(BizException.class)
                .hasMessage(CommonCode.BAD_REQUEST.getMessage())
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
        assertThatThrownBy(() -> Require.positive(0, CommonCode.BAD_REQUEST))
                .isInstanceOf(BizException.class)
                .hasMessage(CommonCode.BAD_REQUEST.getMessage())
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
        assertThatThrownBy(() -> Require.nonNegative(-1, CommonCode.BAD_REQUEST))
                .isInstanceOf(BizException.class)
                .hasMessage(CommonCode.BAD_REQUEST.getMessage())
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
        assertThatThrownBy(() -> Require.inRange(4, 1, 3, CommonCode.BAD_REQUEST))
                .isInstanceOf(BizException.class)
                .hasMessage(CommonCode.BAD_REQUEST.getMessage())
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
    }

    @Test
    void bizCodeOverloadsKeepCodeAndAllowSpecificMessage() {
        assertThatThrownBy(() -> Require.notNull(null, CommonCode.NOT_FOUND, "指定对象不存在"))
                .isInstanceOf(BizException.class)
                .hasMessage("指定对象不存在")
                .extracting("code").isEqualTo(CommonCode.NOT_FOUND.getCode());
        assertThatThrownBy(() -> Require.isTrue(false, CommonCode.BAD_REQUEST, "状态不允许"))
                .isInstanceOf(BizException.class)
                .hasMessage("状态不允许")
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
        assertThatThrownBy(() -> Require.notBlank(" ", CommonCode.BAD_REQUEST, "文本不能为空"))
                .isInstanceOf(BizException.class)
                .hasMessage("文本不能为空")
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
        assertThatThrownBy(() -> Require.notEmpty("", CommonCode.BAD_REQUEST, "集合不能为空"))
                .isInstanceOf(BizException.class)
                .hasMessage("集合不能为空")
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
        assertThatThrownBy(() -> Require.fail(CommonCode.NOT_FOUND, "资源已失效"))
                .isInstanceOf(BizException.class)
                .hasMessage("资源已失效")
                .extracting("code").isEqualTo(CommonCode.NOT_FOUND.getCode());
    }

    @Test
    void failPreservesOriginalCause() {
        IllegalStateException cause = new IllegalStateException("root");

        assertThatThrownBy(() -> Require.fail(CommonCode.BAD_REQUEST, "custom", cause))
                .isInstanceOf(BizException.class)
                .hasMessage("custom")
                .hasCause(cause)
                .extracting("code").isEqualTo(CommonCode.BAD_REQUEST.getCode());
    }

    @Test
    void rethrowPreservesOriginalException() {
        IllegalStateException exception = new IllegalStateException("original");

        assertThatThrownBy(() -> Require.rethrow(exception)).isSameAs(exception);
    }
}
