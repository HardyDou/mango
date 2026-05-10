package io.mango.workflow.core.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.core.model.WorkflowDesignerNode;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 将 Mango 设计器 JSON 转换为 Flowable BPMN。
 */
@Component
@RequiredArgsConstructor
public class WorkflowDesignerBpmnConverter {

    private static final String TARGET_NAMESPACE = "http://mango.io/workflow";

    private final ObjectMapper objectMapper;

    public BpmnModel toModel(String designerJson, String processKey, String processName) {
        WorkflowDesignerNode root = parse(designerJson);
        Require.notBlank(processKey, WorkflowCode.DEFINITION_INVALID.getCode(), "流程编码不能为空");
        Require.notBlank(processName, WorkflowCode.DEFINITION_INVALID.getCode(), "流程名称不能为空");

        BpmnModel model = new BpmnModel();
        model.setTargetNamespace(TARGET_NAMESPACE);

        Process process = new Process();
        process.setId(processKey);
        process.setName(processName);
        process.setExecutable(true);

        String startId = nodeId(root, "startEvent");
        StartEvent startEvent = new StartEvent();
        startEvent.setId(startId);
        startEvent.setName(root.resolvedName());
        process.addFlowElement(startEvent);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("endEvent");
        endEvent.setName("结束");
        process.addFlowElement(endEvent);

        String tailId = appendNode(process, startId, root.getChildNode(), new AtomicInteger(1));
        addSequence(process, tailId, endEvent.getId(), null);

        model.addProcess(process);
        return model;
    }

    public String toXml(String designerJson, String processKey, String processName) {
        byte[] bytes = new BpmnXMLConverter().convertToXML(toModel(designerJson, processKey, processName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private WorkflowDesignerNode parse(String designerJson) {
        Require.notBlank(designerJson, WorkflowCode.DESIGNER_INVALID.getCode(), "设计器JSON不能为空");
        try {
            WorkflowDesignerNode root = objectMapper.readValue(designerJson, WorkflowDesignerNode.class);
            Require.notNull(root, WorkflowCode.DESIGNER_INVALID);
            Require.isTrue("ROOT".equals(root.resolvedNodeType()), WorkflowCode.DESIGNER_INVALID.getCode(), "设计器根节点必须是发起节点");
            return root;
        } catch (JsonProcessingException e) {
            return Require.fail(WorkflowCode.DESIGNER_INVALID.getCode(), "设计器JSON解析失败：" + e.getOriginalMessage());
        }
    }

    private String appendNode(Process process, String sourceId, WorkflowDesignerNode node, AtomicInteger index) {
        if (node == null || !StringUtils.hasText(node.getId())) {
            return sourceId;
        }
        return switch (node.resolvedNodeType()) {
            case "EXCLUSIVE_BRANCH", "PARALLEL_BRANCH", "EMPTY" -> appendNode(process, sourceId, node.getChildNode(), index);
            case "EXCLUSIVE_GATEWAY" -> appendExclusiveGateway(process, sourceId, node, index);
            case "PARALLEL_GATEWAY" -> appendParallelGateway(process, sourceId, node, index);
            case "CC", "SERVICE" -> appendServiceTask(process, sourceId, node, index);
            default -> {
                if (node.resolvedNodeType().startsWith("GUARANTEE_")) {
                    yield appendGuaranteeNode(process, sourceId, node, index);
                }
                yield appendUserTask(process, sourceId, node, index);
            }
        };
    }

    private String appendUserTask(Process process, String sourceId, WorkflowDesignerNode node, AtomicInteger index) {
        String taskId = nodeId(node, "userTask" + index.getAndIncrement());
        UserTask task = new UserTask();
        task.setId(taskId);
        task.setName(node.resolvedName());
        applyUserTaskProperties(task, node);
        process.addFlowElement(task);
        addSequence(process, sourceId, taskId, null);
        return appendNode(process, taskId, node.getChildNode(), index);
    }

    private void applyUserTaskProperties(UserTask task, WorkflowDesignerNode node) {
        Map<String, Object> properties = node.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        String assignee = propertyAsText(properties, "assignee");
        if (StringUtils.hasText(assignee)) {
            task.setAssignee(assignee);
        }
        String candidateUsers = propertyAsText(properties, "candidateUsers");
        if (StringUtils.hasText(candidateUsers)) {
            task.setCandidateUsers(List.of(candidateUsers.split("\\s*,\\s*")));
        }
        String candidateGroups = propertyAsText(properties, "candidateGroups");
        if (StringUtils.hasText(candidateGroups)) {
            task.setCandidateGroups(List.of(candidateGroups.split("\\s*,\\s*")));
        }
    }

    private String appendServiceTask(Process process, String sourceId, WorkflowDesignerNode node, AtomicInteger index) {
        String taskId = nodeId(node, "serviceTask" + index.getAndIncrement());
        ServiceTask task = new ServiceTask();
        task.setId(taskId);
        task.setName(node.resolvedName());
        task.setImplementationType("delegateExpression");
        task.setImplementation("${mangoWorkflowServiceTaskDelegate}");
        task.getFieldExtensions().add(field("nodeDefinitionCode", node.getNodeDefinitionCode()));
        task.getFieldExtensions().add(field("nodeType", node.resolvedNodeType()));
        task.getFieldExtensions().add(field("executionType", resolveExecutionType(node)));
        task.getFieldExtensions().add(field("nodeProperties", resolveProperties(node)));
        task.setAsynchronous(false);
        process.addFlowElement(task);
        addSequence(process, sourceId, taskId, null);
        return appendNode(process, taskId, node.getChildNode(), index);
    }

    private String appendGuaranteeNode(Process process, String sourceId, WorkflowDesignerNode node, AtomicInteger index) {
        if ("GUARANTEE_BANK_SUBMIT".equals(node.resolvedNodeType()) || "GUARANTEE_ARCHIVE".equals(node.resolvedNodeType())) {
            return appendServiceTask(process, sourceId, node, index);
        }
        return appendUserTask(process, sourceId, node, index);
    }

    private String appendExclusiveGateway(Process process, String sourceId, WorkflowDesignerNode node, AtomicInteger index) {
        String gatewayId = nodeId(node, "exclusiveGateway" + index.getAndIncrement());
        String mergeId = gatewayId + "_merge";
        ExclusiveGateway gateway = new ExclusiveGateway();
        gateway.setId(gatewayId);
        gateway.setName(node.resolvedName());
        process.addFlowElement(gateway);
        ExclusiveGateway merge = new ExclusiveGateway();
        merge.setId(mergeId);
        merge.setName(node.resolvedName() + "合并");
        process.addFlowElement(merge);
        addSequence(process, sourceId, gatewayId, null);
        appendBranches(process, gatewayId, mergeId, node.getConditionNodes(), true, index);
        return appendNode(process, mergeId, node.getChildNode(), index);
    }

    private String appendParallelGateway(Process process, String sourceId, WorkflowDesignerNode node, AtomicInteger index) {
        String gatewayId = nodeId(node, "parallelGateway" + index.getAndIncrement());
        String mergeId = gatewayId + "_merge";
        ParallelGateway gateway = new ParallelGateway();
        gateway.setId(gatewayId);
        gateway.setName(node.resolvedName());
        process.addFlowElement(gateway);
        ParallelGateway merge = new ParallelGateway();
        merge.setId(mergeId);
        merge.setName(node.resolvedName() + "合并");
        process.addFlowElement(merge);
        addSequence(process, sourceId, gatewayId, null);
        appendBranches(process, gatewayId, mergeId, node.getConditionNodes(), false, index);
        return appendNode(process, mergeId, node.getChildNode(), index);
    }

    private void appendBranches(Process process, String gatewayId, String mergeId, List<WorkflowDesignerNode> branches,
                                boolean withCondition, AtomicInteger index) {
        Require.notEmpty(branches, WorkflowCode.DESIGNER_INVALID.getCode(), "分支节点至少需要一个分支");
        for (int i = 0; i < branches.size(); i++) {
            WorkflowDesignerNode branch = branches.get(i);
            WorkflowDesignerNode child = branch == null ? null : branch.getChildNode();
            String condition = withCondition ? resolveCondition(branch, i == branches.size() - 1) : null;
            if (child == null || !StringUtils.hasText(child.getId())) {
                addSequence(process, gatewayId, mergeId, condition);
                continue;
            }
            String branchTailId = appendNodeFromGateway(process, gatewayId, child, condition, index);
            addSequence(process, branchTailId, mergeId, null);
        }
    }

    private String appendNodeFromGateway(Process process, String gatewayId, WorkflowDesignerNode child,
                                         String condition, AtomicInteger index) {
        String beforeSizeKey = nodeId(child, "branchTask" + index.get());
        String tailId = appendNode(process, gatewayId, child, index);
        process.getFlowElements().stream()
                .filter(item -> item instanceof SequenceFlow)
                .map(item -> (SequenceFlow) item)
                .filter(flow -> gatewayId.equals(flow.getSourceRef()) && beforeSizeKey.equals(flow.getTargetRef()))
                .findFirst()
                .ifPresent(flow -> flow.setConditionExpression(condition));
        return tailId;
    }

    private String resolveCondition(WorkflowDesignerNode branch, boolean defaultBranch) {
        if (defaultBranch) {
            return null;
        }
        if (branch != null && StringUtils.hasText(branch.getConditionExpression())) {
            return branch.getConditionExpression().trim();
        }
        return "${true}";
    }

    private String resolveExecutionType(WorkflowDesignerNode node) {
        if (StringUtils.hasText(node.getExecutionType())) {
            return node.getExecutionType().trim().toUpperCase(Locale.ROOT);
        }
        return switch (node.resolvedNodeType()) {
            case "CC" -> "EVENT_PUBLISH";
            case "GUARANTEE_BANK_SUBMIT" -> "REMOTE_SERVICE";
            case "GUARANTEE_ARCHIVE" -> "EVENT_PUBLISH";
            default -> "NONE";
        };
    }

    private String resolveProperties(WorkflowDesignerNode node) {
        if (node.getProperties() == null || node.getProperties().isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(node.getProperties());
        } catch (JsonProcessingException e) {
            return Require.fail(WorkflowCode.DESIGNER_INVALID.getCode(), "节点属性JSON生成失败：" + e.getOriginalMessage());
        }
    }

    private String propertyAsText(Map<String, Object> properties, String key) {
        Object value = properties.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private FieldExtension field(String name, String value) {
        FieldExtension field = new FieldExtension();
        field.setFieldName(name);
        field.setStringValue(value == null ? "" : value);
        return field;
    }

    private void addSequence(Process process, String sourceId, String targetId, String condition) {
        Require.notBlank(sourceId, WorkflowCode.DESIGNER_INVALID.getCode(), "流程连线来源节点不能为空");
        Require.notBlank(targetId, WorkflowCode.DESIGNER_INVALID.getCode(), "流程连线目标节点不能为空");
        SequenceFlow flow = new SequenceFlow(sourceId, targetId);
        flow.setId("flow_" + sanitize(sourceId) + "_" + sanitize(targetId) + "_" + process.getFlowElements().size());
        flow.setName(sourceId + "->" + targetId);
        if (StringUtils.hasText(condition)) {
            flow.setConditionExpression(condition);
        }
        process.addFlowElement(flow);
    }

    private String nodeId(WorkflowDesignerNode node, String fallback) {
        String raw = node == null ? fallback : node.getId();
        if (!StringUtils.hasText(raw)) {
            raw = fallback;
        }
        return sanitize(raw);
    }

    private String sanitize(String value) {
        String text = value.trim().replaceAll("[^A-Za-z0-9_]", "_");
        if (text.isBlank()) {
            return "node";
        }
        if (Character.isDigit(text.charAt(0))) {
            return "n_" + text;
        }
        return text.toLowerCase(Locale.ROOT);
    }
}
