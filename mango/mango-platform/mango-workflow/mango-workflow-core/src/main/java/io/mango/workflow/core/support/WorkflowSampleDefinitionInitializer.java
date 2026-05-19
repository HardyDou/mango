package io.mango.workflow.core.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowGroup;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowGroupMapper;
import io.mango.workflow.core.service.IWorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 补齐可演示、可发起的内置工作流示例。
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class WorkflowSampleDefinitionInitializer implements ApplicationRunner {

    private static final Long SYSTEM_USER_ID = 1L;

    private final WorkflowSampleProperties properties;
    private final WorkflowGroupMapper groupMapper;
    private final WorkflowDefinitionMapper definitionMapper;
    private final IWorkflowDefinitionService definitionService;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withSecurity(SYSTEM_USER_ID, String.valueOf(properties.getTenantId()),
                    "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", properties.getTenantId(), "internal-admin"));
            Long groupId = ensureGroup();
            for (SampleDefinition sample : samples()) {
                ensureDefinition(groupId, sample);
            }
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private Long ensureGroup() {
        WorkflowGroup group = groupMapper.selectOne(new LambdaQueryWrapper<WorkflowGroup>()
                .eq(WorkflowGroup::getTenantId, properties.getTenantId())
                .eq(WorkflowGroup::getGroupCode, properties.getGroupCode())
                .last("LIMIT 1"));
        if (group != null) {
            return group.getId();
        }
        LocalDateTime now = LocalDateTime.now();
        WorkflowGroup created = new WorkflowGroup();
        created.setTenantId(properties.getTenantId());
        created.setGroupName(properties.getGroupName());
        created.setGroupCode(properties.getGroupCode());
        created.setSort(10);
        created.setStatus(1);
        created.setRemark("系统内置通用示例流程分组");
        created.setCreatedBy(SYSTEM_USER_ID);
        created.setUpdatedBy(SYSTEM_USER_ID);
        created.setCreatedTime(now);
        created.setCreatedAt(now);
        created.setUpdatedTime(now);
        created.setUpdatedAt(now);
        groupMapper.insert(created);
        return created.getId();
    }

    private void ensureDefinition(Long groupId, SampleDefinition sample) {
        WorkflowDefinition definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinition>()
                .eq(WorkflowDefinition::getTenantId, properties.getTenantId())
                .eq(WorkflowDefinition::getDefinitionKey, sample.definitionKey())
                .last("LIMIT 1"));
        boolean needsRefresh = needsSampleRefresh(definition, sample);
        if (isPublishedAndDeployable(definition) && !needsRefresh) {
            return;
        }
        Long definitionId = definition == null
                ? createDefinition(groupId, sample)
                : needsRefresh ? updateDefinition(definition, sample) : definition.getId();
        R<?> deployResult = definitionService.deploy(definitionId);
        if (!deployResult.isSuccess()) {
            log.warn("内置示例流程发布失败，definitionKey={}, msg={}", sample.definitionKey(), deployResult.getMsg());
        }
    }

    private boolean isPublishedAndDeployable(WorkflowDefinition definition) {
        if (definition == null
                || !WorkflowDefinitionStatus.PUBLISHED.name().equals(definition.getStatus())
                || !StringUtils.hasText(definition.getProcessDefinitionId())) {
            return false;
        }
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definition.getProcessDefinitionId())
                .singleResult() != null;
    }

    private boolean needsSampleRefresh(WorkflowDefinition definition, SampleDefinition sample) {
        if (definition == null || !StringUtils.hasText(definition.getFormJson())) {
            return true;
        }
        return needsSampleFormRefresh(definition, sample) || needsSampleDesignerRefresh(definition, sample);
    }

    private boolean needsSampleFormRefresh(WorkflowDefinition definition, SampleDefinition sample) {
        if (!sample.formJson().contains("\"customConfig\"")) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(definition.getFormJson());
            JsonNode sampleRoot = objectMapper.readTree(sample.formJson());
            String currentSubmitPath = root.path("customConfig").path("submitPath").asText("");
            String currentApplyPageKey = root.path("customConfig").path("applyPageKey").asText("");
            String sampleSubmitPath = sampleRoot.path("customConfig").path("submitPath").asText("");
            String sampleApplyPageKey = sampleRoot.path("customConfig").path("applyPageKey").asText("");
            return !sampleSubmitPath.equals(currentSubmitPath) || !sampleApplyPageKey.equals(currentApplyPageKey);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean needsSampleDesignerRefresh(WorkflowDefinition definition, SampleDefinition sample) {
        if (!sample.designerJson().contains("\"approvePageKey\"")) {
            return false;
        }
        if (!StringUtils.hasText(definition.getDesignerJson())) {
            return true;
        }
        try {
            JsonNode currentRoot = objectMapper.readTree(definition.getDesignerJson());
            JsonNode sampleRoot = objectMapper.readTree(sample.designerJson());
            return !collectApprovePageKeys(currentRoot).containsAll(collectApprovePageKeys(sampleRoot));
        } catch (Exception e) {
            return true;
        }
    }

    private List<String> collectApprovePageKeys(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        List<String> result = new java.util.ArrayList<>();
        collectApprovePageKeys(node, result);
        return result;
    }

    private void collectApprovePageKeys(JsonNode node, List<String> result) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        String approvePageKey = node.path("properties")
                .path("approvalConfig")
                .path("extension")
                .path("approvePageKey")
                .asText("");
        if (StringUtils.hasText(approvePageKey)) {
            result.add(approvePageKey);
        }
        collectApprovePageKeys(node.path("childNode"), result);
        JsonNode conditionNodes = node.path("conditionNodes");
        if (conditionNodes.isArray()) {
            conditionNodes.forEach(child -> collectApprovePageKeys(child, result));
        }
    }

    private Long createDefinition(Long groupId, SampleDefinition sample) {
        SaveWorkflowDefinitionCommand command = new SaveWorkflowDefinitionCommand();
        command.setGroupId(groupId);
        command.setAdminUsers(List.of("admin"));
        command.setIcon(sample.icon());
        command.setDefinitionName(sample.definitionName());
        command.setDefinitionKey(sample.definitionKey());
        command.setDesignerJson(sample.designerJson());
        command.setFormCode(sample.formCode());
        command.setFormJson(sample.formJson());
        command.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        command.setRemark(sample.remark());
        R<String> createResult = definitionService.create(command);
        if (!createResult.isSuccess()) {
            throw new IllegalStateException("内置示例流程创建失败：" + createResult.getMsg());
        }
        return Long.valueOf(createResult.getData());
    }

    private Long updateDefinition(WorkflowDefinition definition, SampleDefinition sample) {
        SaveWorkflowDefinitionCommand command = new SaveWorkflowDefinitionCommand();
        command.setId(definition.getId());
        command.setGroupId(definition.getGroupId());
        command.setAdminUsers(List.of("admin"));
        command.setIcon(sample.icon());
        command.setDefinitionName(sample.definitionName());
        command.setDefinitionKey(sample.definitionKey());
        command.setDesignerJson(sample.designerJson());
        command.setFormCode(sample.formCode());
        command.setFormJson(sample.formJson());
        command.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        command.setRemark(sample.remark());
        R<Boolean> updateResult = definitionService.update(command);
        if (!updateResult.isSuccess()) {
            throw new IllegalStateException("内置示例流程更新失败：" + updateResult.getMsg());
        }
        return definition.getId();
    }

    private List<SampleDefinition> samples() {
        return List.of(expenseSample(), sealSample(), leaveSample());
    }

    private SampleDefinition expenseSample() {
        return new SampleDefinition(
                "费用报销审批",
                "expense_reimbursement",
                "form_expense_reimbursement",
                "Money",
                rootDesignerJson(approvalNode("部门经理审批", "manager_approve",
                        approvalNode("财务复核", "finance_review", null,
                                Map.of("approvePageKey", "workflow.expense.approve.finance", "sectionPreset", "FINANCE_REVIEW")),
                        Map.of("approvePageKey", "workflow.expense.approve.manager", "sectionPreset", "MANAGER_APPROVE"))),
                customFormJson("/workflow/custom-apply", "/workflow/business-form", "workflow.expense.apply", "workflow.expense.approve"),
                "费用报销业务示例，采用自定义申请页和自定义审批页，适合展示业务接入工作流。");
    }

    private SampleDefinition sealSample() {
        return new SampleDefinition(
                "合同用印审批",
                "contract_seal_approval",
                "form_contract_seal_approval",
                "Stamp",
                rootDesignerJson(approvalNode("部门负责人审批", "dept_manager_approve",
                        approvalNode("法务复核", "legal_review",
                                approvalNode("财务复核", "finance_review",
                                        approvalNode("印章管理员办理", "seal_keeper", null,
                                                Map.of("approvePageKey", "workflow.contractSeal.approve.sealKeeper",
                                                        "sectionPreset", "SEAL_KEEPER")),
                                        Map.of("approvePageKey", "workflow.contractSeal.approve.finance",
                                                "sectionPreset", "FINANCE_REVIEW")),
                                Map.of("approvePageKey", "workflow.contractSeal.approve.legal",
                                        "sectionPreset", "LEGAL_REVIEW")),
                        Map.of("approvePageKey", "workflow.contractSeal.approve.manager",
                                "sectionPreset", "MANAGER_APPROVE"))),
                customFormJson("/workflow/custom-apply", "/workflow/business-form", "workflow.contractSeal.apply", "workflow.contractSeal.approve"),
                "合同用印业务示例，使用类 Word 表格申请页，覆盖多节点业务审批。");
    }

    private SampleDefinition leaveSample() {
        return new SampleDefinition(
                "请假申请",
                "leave_application",
                "form_leave_application",
                "CalendarDays",
                rootDesignerJson(approvalNode("直属主管审批", "leave_manager_approve",
                        approvalNode("人事备案", "hr_record", null,
                                Map.of("sectionPreset", "HR_RECORD")),
                        Map.of("sectionPreset", "MANAGER_APPROVE"))),
                json(List.of(
                        formItem("input", "applicant", "申请人", Map.of("placeholder", "请输入申请人")),
                        formItem("inputNumber", "days", "请假天数", Map.of("min", 1, "placeholder", "请输入请假天数")),
                        formItem("datePicker", "startDate", "开始日期", Map.of("type", "date", "valueFormat", "YYYY-MM-DD")),
                        formItem("datePicker", "endDate", "结束日期", Map.of("type", "date", "valueFormat", "YYYY-MM-DD")),
                        formItem("textarea", "reason", "请假事由", Map.of("placeholder", "请输入请假事由")))),
                "通用动态表单示例，适合验证发起流程和审批任务的标准动态表单能力。");
    }

    private String rootDesignerJson(Map<String, Object> childNode) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", "startEvent");
        root.put("nodeName", "发起人");
        root.put("nodeType", "ROOT");
        root.put("childNode", childNode);
        root.put("conditionNodes", List.of());
        root.put("properties", Map.of());
        return json(root);
    }

    private Map<String, Object> approvalNode(String nodeName,
                                             String nodeId,
                                             Map<String, Object> childNode,
                                             Map<String, Object> extension) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", nodeId);
        node.put("nodeName", nodeName);
        node.put("nodeType", "APPROVAL");
        node.put("bpmnType", "userTask");
        node.put("executionType", "USER_TASK");
        node.put("childNode", childNode);
        node.put("conditionNodes", List.of());
        node.put("properties", Map.of("approvalConfig", approvalConfig(extension)));
        return node;
    }

    private Map<String, Object> approvalConfig(Map<String, Object> extension) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("assigneeType", "SPECIFIED_USER");
        config.put("assigneeIds", List.of("admin"));
        config.put("roleIds", List.of());
        config.put("postIds", List.of());
        config.put("orgIds", List.of());
        config.put("approvalMode", "COUNTERSIGN");
        config.put("emptyAssigneeStrategy", "TO_ADMIN");
        config.put("emptyAssigneeUserIds", List.of());
        config.put("rejectStrategy", "END_PROCESS");
        config.put("formPermissions", Map.of());
        config.put("extension", extension);
        config.put("eventNotify", Map.of("enabled", false, "type", "HTTP", "method", "POST", "timeoutMillis", 5000));
        config.put("initiatorSelectMultiple", false);
        return config;
    }

    private Map<String, Object> formItem(String type, String field, String title, Map<String, Object> props) {
        return Map.of(
                "type", type,
                "field", field,
                "title", title,
                "props", props,
                "validate", List.of(Map.of("required", true, "message", title + "不能为空", "trigger", "blur")));
    }

    private String customFormJson(String submitPath, String viewPath, String applyPageKey, String approvePageKey) {
        Map<String, Object> customConfig = new LinkedHashMap<>();
        customConfig.put("submitPath", submitPath);
        customConfig.put("viewPath", viewPath);
        customConfig.put("applyPageKey", applyPageKey);
        customConfig.put("approvePageKey", approvePageKey);
        Map<String, Object> formConfig = new LinkedHashMap<>();
        formConfig.put("mode", "CUSTOM");
        formConfig.put("rules", List.of());
        formConfig.put("fields", List.of());
        formConfig.put("customConfig", customConfig);
        return json(formConfig);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("示例流程JSON生成失败", e);
        }
    }

    private record SampleDefinition(String definitionName, String definitionKey, String formCode, String icon,
                                    String designerJson, String formJson, String remark) {
    }
}
