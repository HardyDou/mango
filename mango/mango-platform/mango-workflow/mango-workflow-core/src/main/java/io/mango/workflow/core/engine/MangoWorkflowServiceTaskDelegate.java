package io.mango.workflow.core.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.workflow.api.WorkflowCode;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.ServiceTask;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mango 统一服务节点执行入口。
 */
@Component
@RequiredArgsConstructor
public class MangoWorkflowServiceTaskDelegate implements JavaDelegate {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final List<WorkflowNodeExecutor> executors;

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, String> fields = fieldMap(execution);
        String resolvedExecutionType = fieldValue(fields, "executionType", "NONE").toUpperCase(Locale.ROOT);
        WorkflowNodeExecutionContext context = WorkflowNodeExecutionContext.builder()
                .execution(execution)
                .nodeDefinitionCode(fieldValue(fields, "nodeDefinitionCode", null))
                .nodeType(fieldValue(fields, "nodeType", null))
                .nodeName(execution.getCurrentFlowElement() == null ? null : execution.getCurrentFlowElement().getName())
                .executionType(resolvedExecutionType)
                .properties(parseProperties(fieldValue(fields, "nodeProperties", null)))
                .build();
        Map<String, WorkflowNodeExecutor> executorMap = executorMap();
        WorkflowNodeExecutor executor = executorMap.getOrDefault(resolvedExecutionType, executorMap.get("NONE"));
        Require.notNull(executor, WorkflowCode.DESIGNER_INVALID.getCode(), "节点执行器未配置：" + resolvedExecutionType);
        executor.execute(context);
    }

    private Map<String, WorkflowNodeExecutor> executorMap() {
        if (executors == null || executors.isEmpty()) {
            return Collections.emptyMap();
        }
        return executors.stream().collect(Collectors.toMap(
                item -> item.executionType().toUpperCase(Locale.ROOT),
                Function.identity(),
                (left, right) -> left));
    }

    private Map<String, Object> parseProperties(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Require.fail(WorkflowCode.DESIGNER_INVALID.getCode(), "节点属性JSON解析失败：" + e.getMessage());
        }
    }

    private Map<String, String> fieldMap(DelegateExecution execution) {
        if (!(execution.getCurrentFlowElement() instanceof ServiceTask task) || task.getFieldExtensions() == null) {
            return Collections.emptyMap();
        }
        return task.getFieldExtensions().stream()
                .filter(item -> StringUtils.hasText(item.getFieldName()))
                .collect(Collectors.toMap(
                        FieldExtension::getFieldName,
                        item -> item.getStringValue() == null ? "" : item.getStringValue(),
                        (left, right) -> left));
    }

    private String fieldValue(Map<String, String> fields, String fieldName, String defaultValue) {
        String value = fields.get(fieldName);
        if (value == null) {
            return defaultValue;
        }
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
