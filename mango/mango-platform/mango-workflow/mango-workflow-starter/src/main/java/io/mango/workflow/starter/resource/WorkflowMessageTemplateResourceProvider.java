package io.mango.workflow.starter.resource;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.support.resource.NoticeMessageTemplateResourceDeclarations;
import io.mango.notice.support.resource.NoticeMessageTemplateResourceDeclarations.MessageTemplateSpec;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.model.ResourceDeclaration;
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
                spec(2026061900300010000L, 2060000000001030000L, "workflow.task.assigned", "待审核",
                        "流程产生待办任务后通知办理人。", NoticePriority.HIGH,
                        "待审核：{{processName}}", "业务：{{applyTitle}}；状态：已提交审核。",
                        "待审核：{{processName}}", "业务：{{applyTitle}}；状态：已提交审核。",
                        "待审核：{{processName}}", "业务：{{applyTitle}}；状态：已提交审核。",
                        "待审核：{{processName}}；业务：{{applyTitle}}。"),
                spec(2026061900300010400L, 2060000000001030400L, "workflow.process.completed", "流程完成",
                        "流程审核通过后通知发起人。", NoticePriority.NORMAL,
                        "审核通过：{{processName}}", "业务：{{applyTitle}}；结果：审核通过。",
                        "审核通过：{{processName}}", "业务：{{applyTitle}}；结果：审核通过。",
                        "审核通过：{{processName}}", "业务：{{applyTitle}}；结果：审核通过。",
                        "审核通过：{{processName}}；业务：{{applyTitle}}。"),
                spec(2026061900300010500L, 2060000000001030500L, "workflow.process.rejected", "流程拒绝",
                        "流程审核未通过后通知发起人。", NoticePriority.HIGH,
                        "审核未通过：{{processName}}", "业务：{{applyTitle}}；结果：审核未通过。原因：{{reason}}",
                        "审核未通过：{{processName}}", "业务：{{applyTitle}}；结果：审核未通过。原因：{{reason}}",
                        "审核未通过：{{processName}}", "业务：{{applyTitle}}；结果：审核未通过。原因：{{reason}}",
                        "审核未通过：{{processName}}；业务：{{applyTitle}}。")
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
        return "{\"type\":\"object\",\"properties\":{\"processInstanceId\":{\"type\":\"string\",\"title\":\"流程实例ID\"},\"processName\":{\"type\":\"string\",\"title\":\"流程名称\"},\"applyTitle\":{\"type\":\"string\",\"title\":\"业务标题\"},\"taskId\":{\"type\":\"string\",\"title\":\"任务ID\"},\"taskName\":{\"type\":\"string\",\"title\":\"任务名称\"},\"reason\":{\"type\":\"string\",\"title\":\"原因\"}},\"required\":[\"processInstanceId\",\"processName\",\"applyTitle\"]}";
    }
}
