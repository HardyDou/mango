package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentRefundApprovalWorkflowDefinitionService {

    public static final String PAYMENT_DOMAIN_CODE = "PAYMENT";
    public static final String REFUND_APPROVAL_DEFINITION_KEY = "PAYMENT_REFUND_APPROVAL";

    private static final String CATEGORY_CODE = "PAYMENT_BUILTIN";

    private final WorkflowDefinitionApi workflowDefinitionApi;
    private final ObjectMapper objectMapper;

    public PaymentRefundApprovalWorkflowDefinitionService(
            @Qualifier("workflowDefinitionServiceImpl") WorkflowDefinitionApi workflowDefinitionApi,
            ObjectMapper objectMapper) {
        this.workflowDefinitionApi = workflowDefinitionApi;
        this.objectMapper = objectMapper;
    }

    public R<WorkflowDeployVO> ensureRefundApprovalDefinition() {
        return workflowDefinitionApi.ensurePublished(definitionCommand());
    }

    private EnsureWorkflowDefinitionCommand definitionCommand() {
        EnsureWorkflowDefinitionCommand command = new EnsureWorkflowDefinitionCommand();
        command.setDomainCode(PAYMENT_DOMAIN_CODE);
        command.setCategoryCode(CATEGORY_CODE);
        command.setCategoryName("支付内置流程");
        command.setCategorySort(20);
        command.setCategoryRemark("支付域内置工作流分类");
        command.setOrgId(1L);
        command.setAdminUsers(List.of("admin"));
        command.setIcon("ReceiptText");
        command.setDefinitionName("退款审批");
        command.setDefinitionKey(REFUND_APPROVAL_DEFINITION_KEY);
        command.setDesignerJson(designerJson());
        command.setFormCode("payment_refund_approval");
        command.setFormJson(formJson());
        command.setRemark("支付域内置退款审批流程，默认服务节点自动通过。");
        return command;
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
