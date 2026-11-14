package io.mango.workflow.core.engine;

import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 维护候选组与租户成员的双向映射。
 *
 * <p>正向把当前用户上下文转换为 Flowable 候选组；反向把 {@code ROLE:}、{@code POST:}、
 * {@code ORG:}、{@code ORG_LEADER:} 候选组展开为当前租户启用成员的 user id。
 */
@Component
@RequiredArgsConstructor
public class WorkflowCandidateGroupProvider {

    private final JdbcTemplate jdbcTemplate;

    public List<String> currentCandidateGroups() {
        Set<String> groups = new LinkedHashSet<>();
        Long tenantId = currentTenantId();
        Long userId = MangoContextHolder.userId();
        Long memberId = MangoContextHolder.memberId();

        add(groups, WorkflowAssigneeResolver.GROUP_ROLE_PREFIX, roles(tenantId, memberId));
        add(groups, WorkflowAssigneeResolver.GROUP_POST_PREFIX, posts(tenantId, memberId));
        add(groups, WorkflowAssigneeResolver.GROUP_ORG_PREFIX, orgs(tenantId, memberId));
        add(groups, WorkflowAssigneeResolver.GROUP_ORG_LEADER_PREFIX, leaderOrgs(tenantId, memberId));
        if (userId != null) {
            groups.add(WorkflowAssigneeResolver.GROUP_ROLE_PREFIX + userId);
            groups.add(WorkflowAssigneeResolver.GROUP_POST_PREFIX + userId);
            groups.add(WorkflowAssigneeResolver.GROUP_ORG_PREFIX + userId);
            groups.add(WorkflowAssigneeResolver.GROUP_ORG_LEADER_PREFIX + userId);
        }
        if (memberId != null) {
            groups.add(WorkflowAssigneeResolver.GROUP_ROLE_PREFIX + memberId);
            groups.add(WorkflowAssigneeResolver.GROUP_POST_PREFIX + memberId);
            groups.add(WorkflowAssigneeResolver.GROUP_ORG_PREFIX + memberId);
            groups.add(WorkflowAssigneeResolver.GROUP_ORG_LEADER_PREFIX + memberId);
        }
        return List.copyOf(groups);
    }

    /**
     * 把单个候选组展开为当前租户启用成员的 user id。
     *
     * <p>未知前缀、非法 id 或查询失败返回空列表。
     */
    public List<Long> memberUserIds(String group) {
        if (!StringUtils.hasText(group)) {
            return List.of();
        }
        String tenant = String.valueOf(currentTenantId());
        int separator = group.indexOf(':');
        if (separator < 0) {
            return List.of();
        }
        String value = group.substring(separator + 1).trim();
        try {
            Long id = Long.valueOf(value);
            if (group.startsWith(WorkflowAssigneeResolver.GROUP_ROLE_PREFIX)) {
                return queryIds("""
                        select distinct tm.user_id
                        from authorization_subject_role sr
                        join tenant_member tm on tm.id = sr.subject_id and tm.tenant_id = sr.tenant_id
                        where sr.tenant_id = ? and sr.subject_type = 'TENANT_MEMBER'
                          and sr.role_id = ? and tm.status = 1 and tm.left_at is null
                        """, tenant, id);
            }
            if (group.startsWith(WorkflowAssigneeResolver.GROUP_POST_PREFIX)) {
                return queryIds("""
                        select distinct tm.user_id from tenant_member_org tmo
                        join tenant_member tm on tm.id = tmo.member_id and tm.tenant_id = tmo.tenant_id
                        where tmo.tenant_id = ? and tmo.post_id = ? and tm.status = 1 and tm.left_at is null
                        """, tenant, id);
            }
            if (group.startsWith(WorkflowAssigneeResolver.GROUP_ORG_LEADER_PREFIX)) {
                return queryIds("""
                        select distinct tm.user_id from tenant_member_org tmo
                        join tenant_member tm on tm.id = tmo.member_id and tm.tenant_id = tmo.tenant_id
                        where tmo.tenant_id = ? and tmo.org_id = ? and tmo.leader_flag = 1
                          and tm.status = 1 and tm.left_at is null
                        """, tenant, id);
            }
            if (group.startsWith(WorkflowAssigneeResolver.GROUP_ORG_PREFIX)) {
                return queryIds("""
                        select distinct tm.user_id from tenant_member_org tmo
                        join tenant_member tm on tm.id = tmo.member_id and tm.tenant_id = tmo.tenant_id
                        where tmo.tenant_id = ? and tmo.org_id = ? and tm.status = 1 and tm.left_at is null
                        """, tenant, id);
            }
        } catch (NumberFormatException ignored) {
            return List.of();
        }
        return List.of();
    }

    private List<Long> roles(Long tenantId, Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return queryIds("""
                select role_id
                from authorization_subject_role
                where tenant_id = ? and subject_id = ?
                """, tenantId, memberId);
    }

    private List<Long> posts(Long tenantId, Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return queryIds("""
                select primary_post_id
                from tenant_member
                where tenant_id = ? and id = ? and primary_post_id is not null
                union
                select post_id
                from tenant_member_org
                where tenant_id = ? and member_id = ? and post_id is not null
                """, tenantId, memberId, tenantId, memberId);
    }

    private List<Long> orgs(Long tenantId, Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return queryIds("""
                select primary_org_id
                from tenant_member
                where tenant_id = ? and id = ? and primary_org_id is not null
                union
                select org_id
                from tenant_member_org
                where tenant_id = ? and member_id = ?
                """, tenantId, memberId, tenantId, memberId);
    }

    private List<Long> leaderOrgs(Long tenantId, Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return queryIds("""
                select primary_org_id
                from tenant_member
                where tenant_id = ? and id = ? and primary_org_id is not null
                  and member_type in ('INSTITUTION_ADMIN','ORG_LEADER','ORG_MANAGER')
                union
                select tmo.org_id
                from tenant_member_org tmo
                where tmo.tenant_id = ? and tmo.member_id = ? and tmo.leader_flag = 1
                union
                select tmo.org_id
                from tenant_member_org tmo
                join org_post op on op.id = tmo.post_id and op.tenant_id = tmo.tenant_id
                where tmo.tenant_id = ? and tmo.member_id = ?
                  and op.post_status = '1'
                  and (
                    upper(op.post_code) in ('DEPT_MANAGER','ORG_MANAGER','TEAM_LEADER')
                    or right(upper(op.post_code), 13) = '_DEPT_MANAGER'
                    or right(upper(op.post_code), 12) = '_ORG_MANAGER'
                    or right(upper(op.post_code), 12) = '_TEAM_LEADER'
                  )
                """, tenantId, memberId, tenantId, memberId, tenantId, memberId);
    }

    private List<Long> queryIds(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, Long.class, args);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private void add(Set<String> groups, String prefix, List<Long> ids) {
        ids.stream().distinct().forEach(id -> groups.add(prefix + id));
    }

    private Long currentTenantId() {
        try {
            return Long.valueOf(MangoContextHolder.tenantId());
        } catch (RuntimeException e) {
            return 1L;
        }
    }
}
