package io.mango.workflow.core.engine;

import lombok.Builder;
import lombok.Getter;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.Map;

/**
 * 工作流节点执行上下文。
 */
@Getter
@Builder
public class WorkflowNodeExecutionContext {

    private final DelegateExecution execution;
    private final String nodeDefinitionCode;
    private final String nodeType;
    private final String nodeName;
    private final String executionType;
    private final Map<String, Object> properties;
}
