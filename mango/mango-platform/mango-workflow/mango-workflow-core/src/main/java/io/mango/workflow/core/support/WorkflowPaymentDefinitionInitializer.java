package io.mango.workflow.core.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.core.entity.WorkflowCategory;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
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
 * 初始化支付域内置工作流定义。
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@RequiredArgsConstructor
public class WorkflowPaymentDefinitionInitializer implements ApplicationRunner {

    public static final String PAYMENT_DOMAIN_CODE = "PAYMENT";
    public static final String REFUND_APPROVAL_DEFINITION_KEY = "PAYMENT_REFUND_APPROVAL";

    private static final Long SYSTEM_TENANT_ID = 1L;
    private static final Long SYSTEM_USER_ID = 1L;
    private static final String CATEGORY_CODE = "PAYMENT_BUILTIN";

    private final WorkflowCategoryMapper categoryMapper;
    private final WorkflowDefinitionMapper definitionMapper;
    private final IWorkflowDefinitionService definitionService;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withSecurity(SYSTEM_USER_ID, String.valueOf(SYSTEM_TENANT_ID),
                    "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", SYSTEM_TENANT_ID, "internal-admin"));
            Long categoryId = ensureCategory();
            ensureRefundApprovalDefinition(categoryId);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private Long ensureCategory() {
        WorkflowCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<WorkflowCategory>()
                .eq(WorkflowCategory::getTenantId, SYSTEM_TENANT_ID)
                .eq(WorkflowCategory::getDomainCode, PAYMENT_DOMAIN_CODE)
                .eq(WorkflowCategory::getCategoryCode, CATEGORY_CODE)
                .last("limit 1"));
        if (category != null) {
            return category.getId();
        }
        LocalDateTime now = LocalDateTime.now();
        WorkflowCategory created = new WorkflowCategory();
        created.setTenantId(SYSTEM_TENANT_ID);
        created.setCategoryName("支付内置流程");
        created.setCategoryCode(CATEGORY_CODE);
        created.setDomainCode(PAYMENT_DOMAIN_CODE);
        created.setSort(20);
        created.setStatus(1);
        created.setRemark("支付域内置工作流分类");
        created.setCreatedBy(SYSTEM_USER_ID);
        created.setUpdatedBy(SYSTEM_USER_ID);
        created.setCreatedTime(now);
        created.setCreatedAt(now);
        created.setUpdatedTime(now);
        created.setUpdatedAt(now);
        categoryMapper.insert(created);
        return created.getId();
    }

    private void ensureRefundApprovalDefinition(Long categoryId) {
        WorkflowDefinition definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinition>()
                .eq(WorkflowDefinition::getTenantId, SYSTEM_TENANT_ID)
                .eq(WorkflowDefinition::getDefinitionKey, REFUND_APPROVAL_DEFINITION_KEY)
                .last("limit 1"));
        if (isPublishedAndDeployable(definition)) {
            return;
        }
        Long definitionId = definition == null ? createDefinition(categoryId) : definition.getId();
        R<?> deployResult = definitionService.deploy(definitionId);
        if (!deployResult.isSuccess()) {
            log.warn("支付内置退款审批流程发布失败，definitionKey={}, msg={}",
                    REFUND_APPROVAL_DEFINITION_KEY, deployResult.getMsg());
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

    private Long createDefinition(Long categoryId) {
        SaveWorkflowDefinitionCommand command = new SaveWorkflowDefinitionCommand();
        command.setCategoryId(categoryId);
        command.setDomainCode(PAYMENT_DOMAIN_CODE);
        command.setOrgId(1L);
        command.setAdminUsers(List.of("admin"));
        command.setIcon("ReceiptText");
        command.setDefinitionName("退款审批");
        command.setDefinitionKey(REFUND_APPROVAL_DEFINITION_KEY);
        command.setDesignerJson(designerJson());
        command.setFormCode("payment_refund_approval");
        command.setFormJson(formJson());
        command.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        command.setRemark("支付域内置退款审批流程，默认服务节点自动通过。");
        R<String> createResult = definitionService.create(command);
        if (!createResult.isSuccess()) {
            throw new IllegalStateException("支付内置退款审批流程创建失败：" + createResult.getMsg());
        }
        return Long.valueOf(createResult.getData());
    }

    private String designerJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", "startEvent");
        root.put("nodeName", "发起人");
        root.put("nodeType", "ROOT");
        root.put("childNode", autoApproveNode());
        root.put("conditionNodes", List.of());
        root.put("properties", Map.of());
        return json(root);
    }

    private Map<String, Object> autoApproveNode() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", "payment_refund_auto_approve");
        node.put("nodeName", "默认自动通过");
        node.put("nodeType", "SERVICE");
        node.put("bpmnType", "serviceTask");
        node.put("executionType", "NONE");
        node.put("childNode", null);
        node.put("conditionNodes", List.of());
        node.put("properties", Map.of(
                "description", "默认配置下提交后自动审批通过，业务侧仍通过 workflow 完成事件推进退款。"));
        return node;
    }

    private String formJson() {
        return json(Map.of(
                "mode", "CUSTOM",
                "rules", List.of(),
                "fields", List.of(),
                "customConfig", Map.of(
                        "viewPath", "/payment/refund-approvals",
                        "applyPageKey", "payment.refundApproval.apply",
                        "approvePageKey", "payment.refundApproval.approve")));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("支付内置工作流JSON生成失败", e);
        }
    }
}
