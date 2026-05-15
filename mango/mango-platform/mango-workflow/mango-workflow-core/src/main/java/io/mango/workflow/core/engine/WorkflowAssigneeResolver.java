package io.mango.workflow.core.engine;

import io.mango.workflow.api.enums.WorkflowAssigneeType;
import io.mango.workflow.api.enums.WorkflowEmptyAssigneeStrategy;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 审批人配置解析器。
 */
@Component
@RequiredArgsConstructor
public class WorkflowAssigneeResolver {

    public static final String GROUP_ROLE_PREFIX = "ROLE:";
    public static final String GROUP_POST_PREFIX = "POST:";
    public static final String GROUP_ORG_PREFIX = "ORG:";
    public static final String GROUP_ORG_LEADER_PREFIX = "ORG_LEADER:";
    public static final String ADMIN_USER = "admin";

    private final JdbcTemplate jdbcTemplate;

    public ResolvedAssignees resolve(WorkflowApprovalNodeConfig config,
                                     Map<String, Object> variables,
                                     String initiator,
                                     String nodeId) {
        WorkflowApprovalNodeConfig resolvedConfig = config == null ? new WorkflowApprovalNodeConfig() : config;
        WorkflowAssigneeType type = resolvedConfig.getAssigneeType() == null
                ? WorkflowAssigneeType.SPECIFIED_USER
                : resolvedConfig.getAssigneeType();
        return switch (type) {
            case SPECIFIED_USER -> users(resolvedConfig.getAssigneeIds());
            case SPECIFIED_ROLE -> groups(prefix(resolvedConfig.getRoleIds(), GROUP_ROLE_PREFIX));
            case SPECIFIED_POST -> groups(prefix(resolvedConfig.getPostIds(), GROUP_POST_PREFIX));
            case SPECIFIED_ORG -> groups(prefix(resolvedConfig.getOrgIds(), GROUP_ORG_PREFIX));
            case ORG_LEADER -> groups(resolveOrgLeaderGroups(resolvedConfig, initiator));
            case INITIATOR -> users(List.of(initiator));
            case INITIATOR_SELECT -> users(resolveSelectedAssignees(variables, nodeId));
            case FORM_USER -> resolveFormAssignee(resolvedConfig, variables, initiator);
            case EXPRESSION -> expression(resolvedConfig.getExpression());
        };
    }

    private Collection<String> resolveOrgLeaderGroups(WorkflowApprovalNodeConfig config, String initiator) {
        if (config == null) {
            return List.of();
        }
        if (!config.isOrgLeaderUseInitiatorOrg() && config.getOrgIds() != null && !config.getOrgIds().isEmpty()) {
            return prefix(config.getOrgIds(), GROUP_ORG_LEADER_PREFIX);
        }
        return prefix(resolveInitiatorOrgIds(initiator), GROUP_ORG_LEADER_PREFIX);
    }

    private ResolvedAssignees resolveFormAssignee(WorkflowApprovalNodeConfig config, Map<String, Object> variables, String initiator) {
        String sourceType = StringUtils.hasText(config.getFormUserFieldType()) ? config.getFormUserFieldType().trim().toUpperCase() : "USER";
        if ("USER".equals(sourceType)) {
            if ("initiator".equals(config.getFormUserField())) {
                return users(List.of(initiator));
            }
            return users(valueList(variables == null ? null : variables.get(config.getFormUserField())));
        }
        Collection<String> values = valueList(variables == null ? null : variables.get(config.getFormUserField()));
        return switch (sourceType) {
            case "ORG" -> groups(prefix(values, GROUP_ORG_PREFIX));
            case "ROLE" -> groups(prefix(values, GROUP_ROLE_PREFIX));
            case "POST" -> groups(prefix(values, GROUP_POST_PREFIX));
            default -> users(values);
        };
    }

    public ResolvedAssignees applyEmptyStrategy(WorkflowApprovalNodeConfig config, ResolvedAssignees resolved) {
        if (resolved != null && !resolved.empty()) {
            return resolved;
        }
        WorkflowEmptyAssigneeStrategy strategy = config == null || config.getEmptyAssigneeStrategy() == null
                ? WorkflowEmptyAssigneeStrategy.TO_ADMIN
                : config.getEmptyAssigneeStrategy();
        if (strategy == WorkflowEmptyAssigneeStrategy.TO_USER && config != null) {
            return users(config.getEmptyAssigneeUserIds());
        }
        if (strategy == WorkflowEmptyAssigneeStrategy.TO_ADMIN) {
            return users(List.of(ADMIN_USER));
        }
        return ResolvedAssignees.empty(strategy);
    }

    public List<String> currentCandidateGroups(String currentUser, Long userId, Long memberId) {
        Set<String> groups = new LinkedHashSet<>();
        addIfText(groups, GROUP_ROLE_PREFIX + currentUser);
        addIfText(groups, GROUP_POST_PREFIX + currentUser);
        addIfText(groups, GROUP_ORG_PREFIX + currentUser);
        addIfText(groups, GROUP_ORG_LEADER_PREFIX + currentUser);
        if (userId != null) {
            addIfText(groups, GROUP_ROLE_PREFIX + userId);
            addIfText(groups, GROUP_POST_PREFIX + userId);
            addIfText(groups, GROUP_ORG_PREFIX + userId);
            addIfText(groups, GROUP_ORG_LEADER_PREFIX + userId);
        }
        if (memberId != null) {
            addIfText(groups, GROUP_ROLE_PREFIX + memberId);
            addIfText(groups, GROUP_POST_PREFIX + memberId);
            addIfText(groups, GROUP_ORG_PREFIX + memberId);
            addIfText(groups, GROUP_ORG_LEADER_PREFIX + memberId);
        }
        return new ArrayList<>(groups);
    }

    private ResolvedAssignees users(Collection<String> values) {
        return new ResolvedAssignees(clean(values), List.of(), null, null);
    }

    private ResolvedAssignees groups(Collection<String> values) {
        return new ResolvedAssignees(List.of(), clean(values), null, null);
    }

    private ResolvedAssignees expression(String expression) {
        if (!StringUtils.hasText(expression)) {
            return ResolvedAssignees.none();
        }
        return new ResolvedAssignees(List.of(), List.of(), expression.trim(), null);
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveSelectedAssignees(Map<String, Object> variables, String nodeId) {
        if (variables == null || variables.isEmpty()) {
            return List.of();
        }
        Object selected = variables.get("mangoSelectedAssignees");
        if (selected instanceof Map<?, ?> map) {
            Object value = StringUtils.hasText(nodeId) ? map.get(nodeId) : null;
            if (value == null) {
                value = map.get("default");
            }
            return valueList(value);
        }
        return valueList(variables.get("mangoSelectedAssignee_" + nodeId));
    }

    private List<String> prefix(Collection<String> values, String prefix) {
        return clean(values).stream()
                .map(value -> value.startsWith(prefix) ? value : prefix + value)
                .toList();
    }

    private List<String> resolveInitiatorOrgIds(String initiator) {
        if (!StringUtils.hasText(initiator)) {
            return List.of();
        }
        try {
            Long tenantId = currentTenantId();
            return jdbcTemplate.queryForList("""
                    select cast(tm.primary_org_id as char)
                    from tenant_member tm
                    join identity_user iu on iu.id = tm.user_id
                    where tm.tenant_id = ? and iu.username = ? and tm.primary_org_id is not null
                    """, String.class, tenantId, initiator);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<String> valueList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            List<String> values = new ArrayList<>();
            for (Object item : array) {
                values.add(String.valueOf(item));
            }
            return values;
        }
        return List.of(String.valueOf(value).split("\\s*,\\s*"));
    }

    private List<String> clean(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                set.add(value.trim());
            }
        }
        return new ArrayList<>(set);
    }

    private void addIfText(Set<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value);
        }
    }

    private Long currentTenantId() {
        try {
            return Long.valueOf(io.mango.infra.context.core.MangoContextHolder.tenantId());
        } catch (RuntimeException e) {
            return 1L;
        }
    }

    public record ResolvedAssignees(List<String> users,
                                    List<String> groups,
                                    String expression,
                                    WorkflowEmptyAssigneeStrategy emptyStrategy) {

        public static ResolvedAssignees none() {
            return new ResolvedAssignees(List.of(), List.of(), null, null);
        }

        public static ResolvedAssignees empty(WorkflowEmptyAssigneeStrategy strategy) {
            return new ResolvedAssignees(List.of(), List.of(), null, strategy);
        }

        public boolean empty() {
            return (users == null || users.isEmpty())
                    && (groups == null || groups.isEmpty())
                    && !StringUtils.hasText(expression);
        }
    }
}
