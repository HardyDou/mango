package io.mango.template.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.template.api.command.CreateTemplateCommand;
import io.mango.template.api.command.TemplateRenderCommand;
import io.mango.template.api.command.UpdateTemplateCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateApiJsonCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void createAndUpdateKeepExistingJsonFieldNames() throws Exception {
        String createJson = """
                {"templateCode":"contract.notice","templateName":"合同通知","domainCode":"CONTRACT",
                 "sourceFormat":"TEXT","draftContent":"合同编号：${contractNo}","draftVariables":[],"remark":"demo"}
                """;
        CreateTemplateCommand create = objectMapper.readValue(createJson, CreateTemplateCommand.class);
        assertThat(create.getTemplateCode()).isEqualTo("contract.notice");
        assertThat(create.getSourceFormat()).isEqualTo("TEXT");

        UpdateTemplateCommand update = objectMapper.readValue(
                createJson.substring(0, createJson.lastIndexOf('}')) + ",\"id\":1001}",
                UpdateTemplateCommand.class);
        assertThat(update.getId()).isEqualTo(1001L);
        assertThat(objectMapper.valueToTree(update).has("templateCode")).isTrue();
    }

    @Test
    void renderVariablesRemainAJsonObjectOnTheWire() throws Exception {
        String json = """
                {"templateCode":"contract.notice","outputFormat":"TEXT",
                 "variables":{"contractNo":"C-001","amount":12,"customer":{"name":"张三"},"tags":["urgent","vip"]},
                 "async":false,"bizType":"contract","bizId":"1001"}
                """;
        TemplateRenderCommand command = objectMapper.readValue(json, TemplateRenderCommand.class);

        assertThat(command.getVariables().toMap()).containsAllEntriesOf(Map.of(
                "contractNo", "C-001",
                "amount", 12,
                "customer", Map.of("name", "张三"),
                "tags", List.of("urgent", "vip")));
        JsonNode serialized = objectMapper.valueToTree(command);
        assertThat(serialized.path("variables").isObject()).isTrue();
        assertThat(serialized.path("variables").path("contractNo").asText()).isEqualTo("C-001");
        assertThat(serialized.path("variables").path("customer").path("name").asText()).isEqualTo("张三");
        assertThat(serialized.path("variables").path("tags").isArray()).isTrue();

        assertThatThrownBy(() -> objectMapper.readValue(
                "{\"templateCode\":\"contract.notice\",\"outputFormat\":\"TEXT\",\"variables\":[1,2],\"async\":false}",
                TemplateRenderCommand.class))
                .hasRootCauseMessage("模板变量必须是 JSON 对象");
    }
}
