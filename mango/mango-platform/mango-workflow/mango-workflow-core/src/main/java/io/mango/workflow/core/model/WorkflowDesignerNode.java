package io.mango.workflow.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流设计器节点。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowDesignerNode {

    private String id;
    private String nodeDefinitionCode;
    private String nodeName;
    private String nodeType;
    private String bpmnType;
    private String executionType;
    private Integer type;
    private String description;
    private String conditionExpression;
    private String serviceHandler;
    private Map<String, Object> properties;
    private WorkflowDesignerNode childNode;
    private List<WorkflowDesignerNode> conditionNodes;

    public String resolvedNodeType() {
        if (nodeType != null && !nodeType.isBlank()) {
            return nodeType.trim().toUpperCase();
        }
        if (type == null) {
            return "APPROVAL";
        }
        return switch (type) {
            case 0 -> "ROOT";
            case 1 -> "APPROVAL";
            case 2 -> "CC";
            case 4 -> "EXCLUSIVE_GATEWAY";
            case 5 -> "PARALLEL_GATEWAY";
            default -> "EMPTY";
        };
    }

    public String resolvedName() {
        if (nodeName != null && !nodeName.isBlank()) {
            return nodeName.trim();
        }
        return switch (resolvedNodeType()) {
            case "ROOT" -> "发起人";
            case "CC" -> "抄送人";
            case "EXCLUSIVE_GATEWAY" -> "条件分支";
            case "PARALLEL_GATEWAY" -> "并行分支";
            case "SERVICE" -> "服务任务";
            default -> "审批人";
        };
    }
}
