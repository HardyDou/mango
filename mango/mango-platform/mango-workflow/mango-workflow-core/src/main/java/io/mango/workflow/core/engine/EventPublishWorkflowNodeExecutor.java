package io.mango.workflow.core.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring 事件发布节点执行器。
 */
@Component
@RequiredArgsConstructor
public class EventPublishWorkflowNodeExecutor implements WorkflowNodeExecutor {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String executionType() {
        return "EVENT_PUBLISH";
    }

    @Override
    public void execute(WorkflowNodeExecutionContext context) {
        eventPublisher.publishEvent(new WorkflowNodeExecutionEvent(this, context));
    }
}
