package io.mango.workflow.starter.resource;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations.MessageTemplateSpec;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow module message template resources.
 */
@Component
public class WorkflowMessageTemplateResourceProvider implements ResourceProvider {

    @Override
    public List<String> moduleCodes() {
        return List.of("workflow");
    }

    @Override
    public List<ResourceDeclaration> provide() {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        declarations.addAll(specs().stream()
                .flatMap(spec -> NoticeMessageTemplateResourceDeclarations.fourChannels(spec).stream())
                .toList());
        return declarations;
    }

    private List<MessageTemplateSpec> specs() {
        return List.of(
                spec(2026061900300010000L, 2060000000001030000L, "workflow.task.assigned", "审批待办",
                        "流程产生待办任务后通知办理人。", NoticePriority.HIGH,
                        "审批待办：{{processName}}", "你有新的审批待办 {{taskName}}，流程：{{processName}}，申请单：{{businessKey}}。",
                        "【Mango】审批待办：{{processName}}", "你有新的审批待办 {{taskName}}，流程：{{processName}}，申请单：{{businessKey}}。请及时处理。",
                        "审批待办：{{processName}}", "你有新的审批待办 {{taskName}}，申请单：{{businessKey}}。",
                        "你有新的审批待办：{{processName}}。"),
                spec(2026061900300010100L, 2060000000001030100L, "workflow.task.claimable", "待领取审批",
                        "流程产生候选组待领取任务后通知候选办理人。", NoticePriority.NORMAL,
                        "待领取审批：{{processName}}", "你有待领取审批任务 {{taskName}}，流程：{{processName}}，申请单：{{businessKey}}。",
                        "【Mango】待领取审批：{{processName}}", "你有待领取审批任务 {{taskName}}，流程：{{processName}}，申请单：{{businessKey}}。",
                        "待领取审批：{{processName}}", "你有待领取审批任务 {{taskName}}，申请单：{{businessKey}}。",
                        "你有待领取审批：{{processName}}。"),
                spec(2026061900300010200L, 2060000000001030200L, "workflow.task.cc", "流程抄送",
                        "流程抄送节点生成待阅记录后通知抄送人。", NoticePriority.NORMAL,
                        "流程抄送：{{processName}}", "流程 {{processName}} 已抄送给你，申请单：{{businessKey}}。",
                        "【Mango】流程抄送：{{processName}}", "流程 {{processName}} 已抄送给你，申请单：{{businessKey}}。请查看详情。",
                        "流程抄送：{{processName}}", "流程 {{processName}} 已抄送给你，申请单：{{businessKey}}。",
                        "流程 {{processName}} 已抄送给你。"),
                spec(2026061900300010300L, 2060000000001030300L, "workflow.task.rejected", "审批驳回",
                        "审批任务被驳回后通知发起人或相关人。", NoticePriority.HIGH,
                        "审批已驳回：{{processName}}", "流程 {{processName}} 的审批任务 {{taskName}} 已驳回，原因：{{comment}}。",
                        "【Mango】审批已驳回：{{processName}}", "流程 {{processName}} 的审批任务 {{taskName}} 已驳回，申请单：{{businessKey}}，原因：{{comment}}。",
                        "审批已驳回：{{processName}}", "流程 {{processName}} 已驳回，原因：{{comment}}。",
                        "审批已驳回：{{processName}}。"),
                spec(2026061900300010400L, 2060000000001030400L, "workflow.process.completed", "流程完成",
                        "流程审批通过并完成后通知发起人或相关人。", NoticePriority.NORMAL,
                        "流程已完成：{{processName}}", "流程 {{processName}} 已完成，申请单：{{businessKey}}。",
                        "【Mango】流程已完成：{{processName}}", "流程 {{processName}} 已完成，申请单：{{businessKey}}。",
                        "流程已完成：{{processName}}", "流程 {{processName}} 已完成，申请单：{{businessKey}}。",
                        "流程已完成：{{processName}}。"),
                spec(2026061900300010500L, 2060000000001030500L, "workflow.process.rejected", "流程拒绝",
                        "流程实例被拒绝后通知发起人或相关人。", NoticePriority.HIGH,
                        "流程已拒绝：{{processName}}", "流程 {{processName}} 已拒绝，原因：{{reason}}。",
                        "【Mango】流程已拒绝：{{processName}}", "流程 {{processName}} 已拒绝，申请单：{{businessKey}}，原因：{{reason}}。",
                        "流程已拒绝：{{processName}}", "流程 {{processName}} 已拒绝，原因：{{reason}}。",
                        "流程已拒绝：{{processName}}。"),
                spec(2026061900300010600L, 2060000000001030600L, "workflow.process.ended", "流程结束",
                        "流程实例结束后通知发起人或相关人。", NoticePriority.NORMAL,
                        "流程已结束：{{processName}}", "流程 {{processName}} 已结束，申请单：{{businessKey}}。",
                        "【Mango】流程已结束：{{processName}}", "流程 {{processName}} 已结束，申请单：{{businessKey}}。",
                        "流程已结束：{{processName}}", "流程 {{processName}} 已结束，申请单：{{businessKey}}。",
                        "流程已结束：{{processName}}。"),
                spec(2026061900300010700L, 2060000000001030700L, "workflow.task.empty-assignee", "审批人为空处理",
                        "审批人为空触发自动通过、驳回或结束时通知流程管理员。", NoticePriority.HIGH,
                        "审批人为空：{{processName}}", "流程 {{processName}} 的节点 {{taskName}} 审批人为空，系统已按策略处理：{{action}}。",
                        "【Mango】审批人为空：{{processName}}", "流程 {{processName}} 的节点 {{taskName}} 审批人为空，系统已按策略处理：{{action}}。申请单：{{businessKey}}。",
                        "审批人为空：{{processName}}", "流程 {{processName}} 节点 {{taskName}} 审批人为空，处理：{{action}}。",
                        "审批人为空：{{processName}}。")
        );
    }

    private MessageTemplateSpec spec(long resourceBase, long targetBase, String bizType, String bizName,
                                     String description, NoticePriority priority, String siteTitle, String siteContent,
                                     String emailTitle, String emailContent, String wecomTitle, String wecomContent,
                                     String smsContent) {
        return new MessageTemplateSpec("workflow", "工作流", resourceBase, targetBase, targetBase + 1,
                targetBase + 2, 1, bizType, bizName, "WORKFLOW", "WORKFLOW", description, paramsSchema(),
                priority, "BIZ_ID", true, siteTitle, siteContent, emailTitle, emailContent, wecomTitle,
                wecomContent, siteTitle, smsContent);
    }

    private String paramsSchema() {
        return "{\"type\":\"object\",\"properties\":{\"processInstanceId\":{\"type\":\"string\",\"title\":\"流程实例ID\"},\"processName\":{\"type\":\"string\",\"title\":\"流程名称\"},\"taskId\":{\"type\":\"string\",\"title\":\"任务ID\"},\"taskName\":{\"type\":\"string\",\"title\":\"任务名称\"},\"businessType\":{\"type\":\"string\",\"title\":\"业务类型\"},\"businessKey\":{\"type\":\"string\",\"title\":\"业务主键\"},\"comment\":{\"type\":\"string\",\"title\":\"审批意见\"},\"reason\":{\"type\":\"string\",\"title\":\"原因\"},\"action\":{\"type\":\"string\",\"title\":\"处理动作\"}},\"required\":[\"processInstanceId\"]}";
    }
}
