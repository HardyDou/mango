package io.mango.infra.fileproc.render;

import io.mango.infra.fileproc.render.service.FreemarkerRenderEngine;
import io.mango.infra.fileproc.render.service.PlaceholderRenderEngine;
import io.mango.infra.fileproc.render.service.RenderToolException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderEngineTest {

    private final FreemarkerRenderEngine freemarkerEngine = new FreemarkerRenderEngine();
    private final PlaceholderRenderEngine placeholderEngine = new PlaceholderRenderEngine();

    @Test
    void freemarkerRenderRendersNestedVariables() {
        String content = "客户：${customer.name}，金额：${amount}";

        assertThat(freemarkerEngine.render(content,
                Map.of("customer", Map.of("name", "张三"), "amount", "128.00")))
                .isEqualTo("客户：张三，金额：128.00");
    }

    @Test
    void freemarkerExtractFindsInterpolationConditionAndListVariables() {
        String content = """
                <#if customer.level == 'VIP'>${customer.name}</#if>
                <#list materials as item>${item.name};</#list>
                ${amount}
                """;

        assertThat(freemarkerEngine.extract(content))
                .containsExactly("materials", "customer.level", "customer.name", "amount");
    }

    @Test
    void freemarkerRenderThrowsWhenVariableMissing() {
        assertThatThrownBy(() -> freemarkerEngine.render("客户：${customer.name}", Map.of("customer", Map.of())))
                .isInstanceOf(RenderToolException.class)
                .hasMessageContaining("模板渲染失败");
    }

    @Test
    void placeholderExtractsDoubleBraceAndBracketVariables() {
        String content = "客户：{{customer.name}}，资料：[materials.name]";

        assertThat(placeholderEngine.extract(content))
                .containsExactly("customer.name", "materials.name");
    }

    @Test
    void placeholderRenderSupportsNestedMapAndBeanGetter() {
        Person person = new Person("李四");

        assertThat(placeholderEngine.render("客户：{{customer.name}}，处理人：{{handler.name}}",
                Map.of("customer", Map.of("name", "张三"), "handler", person)))
                .isEqualTo("客户：张三，处理人：李四");
    }

    @Test
    void placeholderRenderThrowsWhenVariableMissing() {
        assertThatThrownBy(() -> placeholderEngine.render("客户：{{customer.name}}", Map.of("customer", Map.of())))
                .isInstanceOf(RenderToolException.class)
                .hasMessage("缺少模板变量：customer.name");
    }

    private static final class Person {

        private final String name;

        private Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
