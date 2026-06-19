package io.mango.workflow.core.engine;

import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 Mango 当前用户上下文转换为 Flowable 候选组。
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
