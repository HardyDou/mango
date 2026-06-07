package io.mango.template.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.template.api.command.TemplateVariableDefinition;
import io.mango.template.core.entity.TemplateVersion;
import io.mango.template.core.service.impl.TemplateServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateVariableValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validateRequiredVariablesSupportsNestedDefinitions() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersion version = new TemplateVersion();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(objectVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersion.class, Map.class);
        method.setAccessible(true);
        method.invoke(service, version, Map.of("customer", Map.of("name", "张三")));
    }

    @Test
    void validateRequiredVariablesFailsWhenNestedRequiredVariableMissing() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersion version = new TemplateVersion();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(objectVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersion.class, Map.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(service, version, Map.of("customer", Map.of())))
                .hasRootCauseInstanceOf(BizException.class)
                .hasRootCauseMessage("缺少模板变量：customer.name");
    }

    @Test
    void validateRequiredVariablesFailsWhenTypeMismatch() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersion version = new TemplateVersion();
        TemplateVariableDefinition amount = new TemplateVariableDefinition();
        amount.setName("amount");
        amount.setType("NUMBER");
        amount.setRequired(true);
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(amount)));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersion.class, Map.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(service, version, Map.of("amount", "abc")))
                .hasRootCauseInstanceOf(BizException.class)
                .hasRootCauseMessage("模板变量类型不匹配：amount，期望 NUMBER");
    }

    @Test
    void validateRequiredVariablesSupportsArrayObjectChildren() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersion version = new TemplateVersion();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(arrayVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersion.class, Map.class);
        method.setAccessible(true);
        method.invoke(service, version, Map.of("items", List.of(
                Map.of("name", "身份证", "qty", 1),
                Map.of("name", "营业执照", "qty", 2)
        )));
    }

    @Test
    void validateRequiredVariablesFailsWhenArrayChildMissing() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersion version = new TemplateVersion();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(arrayVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersion.class, Map.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(service, version, Map.of("items", List.of(Map.of("qty", 1)))))
                .hasRootCauseInstanceOf(BizException.class)
                .hasRootCauseMessage("缺少模板变量：items[0].name");
    }

    private TemplateServiceImpl newService() {
        return new TemplateServiceImpl(null, null, null, null, null, objectMapper, null, null);
    }

    private TemplateVariableDefinition objectVariable() {
        TemplateVariableDefinition customer = new TemplateVariableDefinition();
        customer.setName("customer");
        customer.setType("OBJECT");
        customer.setRequired(true);

        TemplateVariableDefinition name = new TemplateVariableDefinition();
        name.setName("name");
        name.setType("STRING");
        name.setRequired(true);
        customer.setChildren(List.of(name));
        return customer;
    }

    private TemplateVariableDefinition arrayVariable() {
        TemplateVariableDefinition items = new TemplateVariableDefinition();
        items.setName("items");
        items.setType("ARRAY");
        items.setRequired(true);

        TemplateVariableDefinition name = new TemplateVariableDefinition();
        name.setName("name");
        name.setType("STRING");
        name.setRequired(true);

        TemplateVariableDefinition qty = new TemplateVariableDefinition();
        qty.setName("qty");
        qty.setType("NUMBER");
        qty.setRequired(true);

        items.setChildren(List.of(name, qty));
        return items;
    }
}
