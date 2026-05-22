package io.mango.template.core.render;

import io.mango.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceholderTemplateEngineTest {

    private final PlaceholderTemplateEngine engine = new PlaceholderTemplateEngine();

    @Test
    void extractKeepsOrderAndDeduplicatesVariables() {
        assertThat(engine.extract("您好 {{ user.name }}，订单 {{orderNo}}，再次 {{user.name}}"))
            .containsExactly("user.name", "orderNo");
    }

    @Test
    void extractSupportsPoiTlLoopRowBracketVariables() {
        assertThat(engine.extract("{{items}} [name] [count]"))
            .containsExactly("items", "name", "count");
    }

    @Test
    void renderSupportsNestedMapVariables() {
        String content = "客户：{{ customer.name }}，金额：{{amount}}";
        Map<String, Object> variables = Map.of(
            "customer", Map.of("name", "张三"),
            "amount", "128.00"
        );

        assertThat(engine.render(content, variables)).isEqualTo("客户：张三，金额：128.00");
    }

    @Test
    void renderAllowsFlatKeyToOverridePathResolution() {
        String content = "处理人：{{ user.name }}";
        Map<String, Object> variables = Map.of("user.name", "李四");

        assertThat(engine.render(content, variables)).isEqualTo("处理人：李四");
    }

    @Test
    void renderFailsWhenVariableIsMissing() {
        assertThatThrownBy(() -> engine.render("客户：{{customer.name}}", Map.of("customer", Map.of())))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("缺少模板变量：customer.name");
    }
}
