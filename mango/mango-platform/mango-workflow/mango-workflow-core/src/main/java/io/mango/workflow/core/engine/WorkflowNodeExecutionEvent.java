package io.mango.workflow.core.engine;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 工作流节点执行事件。
 */
@Getter
public class WorkflowNodeExecutionEvent extends ApplicationEvent {

    private final WorkflowNodeExecutionContext context;

    public WorkflowNodeExecutionEvent(Object source, WorkflowNodeExecutionContext context) {
        super(source);
        this.context = context;
    }
}
