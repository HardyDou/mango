package io.mango.template.core.render;

import io.mango.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreemarkerTemplateEngineTest {

    private final FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine();

    @Test
    void renderSupportsNestedVariables() {
        String content = "客户：${customer.name}，金额：${amount}";
        Map<String, Object> variables = Map.of(
                "customer", Map.of("name", "张三"),
                "amount", "128.00"
        );

        assertThat(engine.render(content, variables)).isEqualTo("客户：张三，金额：128.00");
    }

    @Test
    void renderSupportsConditionsAndLoops() {
        String content = """
                <#if approved>通过</#if>
                <#list materials as item>${item.name};</#list>
                """;
        Map<String, Object> variables = Map.of(
                "approved", true,
                "materials", List.of(Map.of("name", "身份证"), Map.of("name", "营业执照"))
        );

        assertThat(engine.render(content, variables)).contains("通过").contains("身份证;营业执照;");
    }

    @Test
    void extractVariablesFromFreemarkerTemplate() {
        String content = """
                ${customer.name}
                <#if approved && amount gt 1000>${amount?string("0.00")}</#if>
                <#list materials as item>${item.name}</#list>
                """;

        assertThat(engine.extract(content)).containsExactly("materials", "approved", "amount", "customer.name");
    }

    @Test
    void renderFailsWhenVariableIsMissing() {
        assertThatThrownBy(() -> engine.render("客户：${customer.name}", Map.of("customer", Map.of())))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板渲染失败");
    }
}
