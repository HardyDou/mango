package io.mango.template.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.template.api.command.TemplateVariableCommand;
import io.mango.template.core.entity.TemplateVersionEntity;
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
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(objectVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersionEntity.class, Map.class);
        method.setAccessible(true);
        method.invoke(service, version, Map.of("customer", Map.of("name", "张三")));
    }

    @Test
    void validateRequiredVariablesFailsWhenNestedRequiredVariableMissing() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(objectVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersionEntity.class, Map.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(service, version, Map.of("customer", Map.of())))
                .hasRootCauseInstanceOf(BizException.class)
                .hasRootCauseMessage("缺少模板变量：customer.name");
    }

    @Test
    void validateRequiredVariablesFailsWhenTypeMismatch() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersionEntity version = new TemplateVersionEntity();
        TemplateVariableCommand amount = new TemplateVariableCommand();
        amount.setName("amount");
        amount.setType("NUMBER");
        amount.setRequired(true);
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(amount)));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersionEntity.class, Map.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(service, version, Map.of("amount", "abc")))
                .hasRootCauseInstanceOf(BizException.class)
                .hasRootCauseMessage("模板变量类型不匹配：amount，期望 NUMBER");
    }

    @Test
    void validateRequiredVariablesSupportsArrayObjectChildren() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(arrayVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersionEntity.class, Map.class);
        method.setAccessible(true);
        method.invoke(service, version, Map.of("items", List.of(
                Map.of("name", "身份证", "qty", 1),
                Map.of("name", "营业执照", "qty", 2)
        )));
    }

    @Test
    void validateRequiredVariablesFailsWhenArrayChildMissing() throws Exception {
        TemplateServiceImpl service = newService();
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setVariableSchema(objectMapper.writeValueAsString(List.of(arrayVariable())));

        Method method = TemplateServiceImpl.class.getDeclaredMethod("validateRequiredVariables", TemplateVersionEntity.class, Map.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(service, version, Map.of("items", List.of(Map.of("qty", 1)))))
                .hasRootCauseInstanceOf(BizException.class)
                .hasRootCauseMessage("缺少模板变量：items[0].name");
    }

    private TemplateServiceImpl newService() {
        return new TemplateServiceImpl(null, null, null, null, null, null, null);
    }

    private TemplateVariableCommand objectVariable() {
        TemplateVariableCommand customer = new TemplateVariableCommand();
        customer.setName("customer");
        customer.setType("OBJECT");
        customer.setRequired(true);

        TemplateVariableCommand name = new TemplateVariableCommand();
        name.setName("name");
        name.setType("STRING");
        name.setRequired(true);
        customer.setChildren(List.of(name));
        return customer;
    }

    private TemplateVariableCommand arrayVariable() {
        TemplateVariableCommand items = new TemplateVariableCommand();
        items.setName("items");
        items.setType("ARRAY");
        items.setRequired(true);

        TemplateVariableCommand name = new TemplateVariableCommand();
        name.setName("name");
        name.setType("STRING");
        name.setRequired(true);

        TemplateVariableCommand qty = new TemplateVariableCommand();
        qty.setName("qty");
        qty.setType("NUMBER");
        qty.setRequired(true);

        items.setChildren(List.of(name, qty));
        return items;
    }
}
