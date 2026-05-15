package io.mango.workflow.core.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.enums.WorkflowApprovalMode;
import io.mango.workflow.api.enums.WorkflowAssigneeType;
import io.mango.workflow.api.enums.WorkflowEmptyAssigneeStrategy;
import io.mango.workflow.api.enums.WorkflowFormPermission;
import io.mango.workflow.api.enums.WorkflowRejectStrategy;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import io.mango.workflow.core.model.WorkflowEventNotifyConfig;
import io.mango.workflow.core.model.WorkflowDesignerNode;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 将 Mango 设计器 JSON 转换为 Flowable BPMN。
 */
@Component
@RequiredArgsConstructor
public class WorkflowDesignerBpmnConverter {

    private static final String TARGET_NAMESPACE = "http://mango.io/workflow";
    private static final String MANGO_EXTENSION_NAMESPACE = "http://mango.io/workflow/extensions";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final WorkflowAssigneeResolver assigneeResolver;

    public BpmnModel toModel(String designerJson, String processKey, String processName) {
        WorkflowDesignerNode root = parse(designerJson);
        Require.notBlank(processKey, WorkflowCode.DEFINITION_INVALID.getCode(), "流程编码不能为空");
        Require.notBlank(processName, WorkflowCode.DEFINITION_INVALID.getCode(), "流程名称不能为空");

        BpmnModel model = new BpmnModel();
        model.setTargetNamespace(TARGET_NAMESPACE);
        model.addNamespace("mango", MANGO_EXTENSION_NAMESPACE);

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
        WorkflowApprovalNodeConfig config = approvalConfig(node);
        Map<String, Object> properties = node.getProperties() == null ? Map.of() : node.getProperties();
        WorkflowAssigneeResolver.ResolvedAssignees resolved = designTimeAssignees(config, node);
        if (isRuntimeResolved(config)) {
            applyRuntimeUserTaskProperties(task, config);
        } else if (resolved.empty()) {
            task.setAssignee("${mangoRuntimeAssignee_" + task.getId() + "}");
        } else if (StringUtils.hasText(resolved.expression())) {
            task.setAssignee(resolved.expression());
        } else if (resolved.users() != null && resolved.users().size() == 1 && config.getApprovalMode() != WorkflowApprovalMode.SEQUENTIAL) {
            task.setAssignee(resolved.users().get(0));
        } else if (resolved.users() != null && resolved.users().size() > 1) {
            applyMultiInstance(task, config, resolved.users());
        } else if (resolved.groups() != null && !resolved.groups().isEmpty()) {
            task.setCandidateGroups(resolved.groups());
        } else {
            applyLegacyUserTaskProperties(task, properties);
        }
        task.getExtensionElements().put("mangoApprovalConfig", List.of(extension("mangoApprovalConfig", resolveApprovalConfigJson(config))));
    }

    private WorkflowAssigneeResolver.ResolvedAssignees designTimeAssignees(WorkflowApprovalNodeConfig config, WorkflowDesignerNode node) {
        return assigneeResolver.applyEmptyStrategy(config,
                assigneeResolver.resolve(config, Map.of(), "${mangoInitiator}", node.getId()));
    }

    private boolean isRuntimeResolved(WorkflowApprovalNodeConfig config) {
        return config != null && (config.getAssigneeType() == WorkflowAssigneeType.INITIATOR_SELECT
                || config.getAssigneeType() == WorkflowAssigneeType.FORM_USER);
    }

    private void applyRuntimeUserTaskProperties(UserTask task, WorkflowApprovalNodeConfig config) {
        if (shouldUseMultiInstance(config, List.of())) {
            if (config.getAssigneeType() == WorkflowAssigneeType.FORM_USER) {
                applyMultiInstance(task, config, "${mangoWorkflowAssigneeCollection.formUsers(execution, '" + safeExpressionText(config.getFormUserField()) + "')}");
            } else {
                applyMultiInstance(task, config, "${mangoWorkflowAssigneeCollection.selected(execution, '" + task.getId() + "')}");
            }
            return;
        }
        if (config.getAssigneeType() == WorkflowAssigneeType.FORM_USER) {
            task.setAssignee("${mangoWorkflowAssigneeCollection.formUsers(execution, '" + safeExpressionText(config.getFormUserField()) + "')[0]}");
        } else {
            task.setAssignee("${mangoWorkflowAssigneeCollection.selected(execution, '" + task.getId() + "')[0]}");
        }
    }

    private void applyLegacyUserTaskProperties(UserTask task, Map<String, Object> properties) {
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
        if (!StringUtils.hasText(assignee) && !StringUtils.hasText(candidateUsers) && !StringUtils.hasText(candidateGroups)) {
            task.setAssignee("${mangoInitiator}");
        }
    }

    private void applyMultiInstance(UserTask task, WorkflowApprovalNodeConfig config, List<String> users) {
        applyMultiInstance(task, config, "${mangoWorkflowAssigneeCollection.list('" + String.join(",", users) + "')}");
    }

    private void applyMultiInstance(UserTask task, WorkflowApprovalNodeConfig config, String collectionExpression) {
        String variableName = "mangoAssignee_" + task.getId();
        task.setAssignee("${" + variableName + "}");
        MultiInstanceLoopCharacteristics loop = new MultiInstanceLoopCharacteristics();
        loop.setInputDataItem(collectionExpression.startsWith("${") ? collectionExpression : "${" + collectionExpression + "}");
        loop.setElementVariable(variableName);
        loop.setSequential(config.getApprovalMode() == WorkflowApprovalMode.SEQUENTIAL);
        if (config.getApprovalMode() == WorkflowApprovalMode.OR_SIGN) {
            loop.setCompletionCondition("${nrOfCompletedInstances >= 1}");
        }
        task.setLoopCharacteristics(loop);
    }

    private boolean shouldUseMultiInstance(WorkflowApprovalNodeConfig config, List<String> users) {
        if (config == null) {
            return users != null && users.size() > 1;
        }
        return config.getApprovalMode() == WorkflowApprovalMode.SEQUENTIAL
                || config.getApprovalMode() == WorkflowApprovalMode.OR_SIGN
                || (users != null && users.size() > 1)
                || config.isInitiatorSelectMultiple();
    }

    private WorkflowApprovalNodeConfig approvalConfig(WorkflowDesignerNode node) {
        Map<String, Object> properties = node.getProperties() == null ? Map.of() : node.getProperties();
        Object structuredValue = properties.get("approvalConfig");
        WorkflowApprovalNodeConfig config = readApprovalConfig(structuredValue);
        if (structuredValue == null) {
            applyLegacyApprovalProperties(config, properties);
        } else {
            config.setFormPermissions(formPermissions(properties.get("formPermissions"), config.getFormPermissions()));
            config.setEventNotify(eventNotify(properties.get("eventNotify"), config.getEventNotify()));
        }
        return config;
    }

    private void applyLegacyApprovalProperties(WorkflowApprovalNodeConfig config, Map<String, Object> properties) {
        config.setAssigneeType(WorkflowAssigneeType.fromCode(text(properties, "assigneeType"), config.getAssigneeType()));
        config.setApprovalMode(WorkflowApprovalMode.fromCode(text(properties, "approvalMode"), config.getApprovalMode()));
        config.setEmptyAssigneeStrategy(WorkflowEmptyAssigneeStrategy.fromCode(text(properties, "emptyAssigneeStrategy"), config.getEmptyAssigneeStrategy()));
        config.setRejectStrategy(WorkflowRejectStrategy.fromCode(text(properties, "rejectStrategy"), config.getRejectStrategy()));
        replaceIfPresent(properties, values -> config.setAssigneeIds(values), "assigneeIds", "assignee", "candidateUsers", "users");
        replaceIfPresent(properties, values -> config.setRoleIds(values), "roleIds", "roles", "candidateGroups");
        replaceIfPresent(properties, values -> config.setPostIds(values), "postIds", "posts");
        replaceIfPresent(properties, values -> config.setOrgIds(values), "orgIds", "orgs");
        replaceIfPresent(properties, values -> config.setEmptyAssigneeUserIds(values), "emptyAssigneeUserIds", "emptyAssigneeUsers");
        if (StringUtils.hasText(text(properties, "formUserField"))) {
            config.setFormUserField(text(properties, "formUserField"));
        }
        if (StringUtils.hasText(text(properties, "expression"))) {
            config.setExpression(text(properties, "expression"));
        }
        if (StringUtils.hasText(text(properties, "initiatorSelectMultiple"))) {
            config.setInitiatorSelectMultiple(Boolean.parseBoolean(text(properties, "initiatorSelectMultiple")));
        }
        config.setFormPermissions(formPermissions(properties.get("formPermissions"), config.getFormPermissions()));
        config.setEventNotify(eventNotify(properties.get("eventNotify"), config.getEventNotify()));
    }

    private WorkflowApprovalNodeConfig readApprovalConfig(Object value) {
        if (value instanceof WorkflowApprovalNodeConfig config) {
            return config;
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return objectMapper.readValue(text, WorkflowApprovalNodeConfig.class);
            } catch (JsonProcessingException ignored) {
                return new WorkflowApprovalNodeConfig();
            }
        }
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            return objectMapper.convertValue(map, WorkflowApprovalNodeConfig.class);
        }
        return new WorkflowApprovalNodeConfig();
    }

    @SuppressWarnings("unchecked")
    private Map<String, WorkflowFormPermission> formPermissions(Object value, Map<String, WorkflowFormPermission> fallback) {
        Map<String, WorkflowFormPermission> permissions = new LinkedHashMap<>();
        if (fallback != null) {
            permissions.putAll(fallback);
        }
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, permission) -> {
                if (key != null) {
                    permissions.put(String.valueOf(key), WorkflowFormPermission.fromCode(String.valueOf(permission), WorkflowFormPermission.READONLY));
                }
            });
        }
        return permissions;
    }

    private WorkflowEventNotifyConfig eventNotify(Object value, WorkflowEventNotifyConfig fallback) {
        if (value instanceof WorkflowEventNotifyConfig config) {
            return config;
        }
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            return objectMapper.convertValue(map, WorkflowEventNotifyConfig.class);
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return objectMapper.readValue(text, WorkflowEventNotifyConfig.class);
            } catch (JsonProcessingException ignored) {
                return fallback == null ? new WorkflowEventNotifyConfig() : fallback;
            }
        }
        return fallback == null ? new WorkflowEventNotifyConfig() : fallback;
    }

    private List<String> list(Map<String, Object> properties, String... keys) {
        for (String key : keys) {
            List<String> values = valueList(properties.get(key));
            if (!values.isEmpty()) {
                return values;
            }
        }
        return List.of();
    }

    private void replaceIfPresent(Map<String, Object> properties, Consumer<List<String>> setter, String... keys) {
        List<String> values = list(properties, keys);
        if (!values.isEmpty()) {
            setter.accept(values);
        }
    }

    private List<String> valueList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    values.add(String.valueOf(item).trim());
                }
            }
            return values;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                return objectMapper.readValue(text, STRING_LIST_TYPE);
            } catch (JsonProcessingException ignored) {
                return List.of(text);
            }
        }
        return List.of(text.split("\\s*,\\s*"));
    }

    private String text(Map<String, Object> properties, String key) {
        Object value = properties.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private ExtensionElement extension(String name, String value) {
        ExtensionElement element = new ExtensionElement();
        element.setName(name);
        element.setNamespacePrefix("mango");
        element.setNamespace(MANGO_EXTENSION_NAMESPACE);
        element.setElementText(value == null ? "{}" : value);
        return element;
    }

    private String resolveApprovalConfigJson(WorkflowApprovalNodeConfig config) {
        return toJson(config == null ? new WorkflowApprovalNodeConfig() : config);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String safeExpressionText(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
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
