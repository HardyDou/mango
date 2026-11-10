package io.mango.workflow.starter.notice;

import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.starter.WorkflowAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * 工作流默认通知自动装配。
 *
 * <p>默认通知是平台的副作用行为：只要启用工作流就会把审批事件转成站内信和企业微信消息。
 * 业务需要自己的通知链路时，按 {@code mango.workflow.notice.enabled=false} 关闭默认通知，
 * 由业务订阅工作流公开事件后自行发送通知。
 *
 * <p>关闭后 {@code workflow.task.assigned}、{@code workflow.process.completed}、
 * {@code workflow.process.rejected} 三类默认消息不再产生；工作流领域事件和业务回写不受影响。
 */
@AutoConfiguration(after = WorkflowAutoConfiguration.class)
@ConditionalOnClass(WorkflowDefinitionMapper.class)
@ConditionalOnProperty(prefix = "mango.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "mango.workflow.notice", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowNoticeAutoConfiguration {

    @Bean
    public WorkflowNoticeDomainEventSubscriber workflowNoticeDomainEventSubscriber(
            ApplicationEventPublisher eventPublisher) {
        return new WorkflowNoticeDomainEventSubscriber(eventPublisher);
    }
}
