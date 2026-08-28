package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.common.result.Require;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.workflow.api.command.ReplaceWorkflowBusinessParticipantsCommand;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.enums.WorkflowParticipantType;
import io.mango.workflow.api.query.WorkflowParticipationAccessQuery;
import io.mango.workflow.api.query.WorkflowParticipationPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessParticipantsVO;
import io.mango.workflow.api.vo.WorkflowParticipationAccessVO;
import io.mango.workflow.api.vo.WorkflowParticipationBusinessVO;
import io.mango.workflow.core.entity.WorkflowProcessParticipantEntity;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowProcessParticipantMapper;
import io.mango.workflow.core.service.IWorkflowParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 工作流参与关系投影实现。 */
@Service
@RequiredArgsConstructor
public class WorkflowParticipationService implements IWorkflowParticipationService {
    private final WorkflowProcessParticipantMapper mapper;
    private final WorkflowFormInstanceMapper formInstanceMapper;
    private final TenantMemberProvider tenantMemberProvider;
    private final AuthUserProvider authUserProvider;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public WorkflowParticipationAccessVO access(WorkflowParticipationAccessQuery query) {
        Long userId = requireUser();
        String tenantId = requireTenant();
        List<WorkflowProcessParticipantEntity> rows = mapper.selectList(new LambdaQueryWrapper<WorkflowProcessParticipantEntity>()
                .eq(WorkflowProcessParticipantEntity::getTenantId, tenantId)
                .eq(WorkflowProcessParticipantEntity::getProcessKey, query.getProcessKey().trim())
                .eq(WorkflowProcessParticipantEntity::getBusinessKey, query.getBusinessKey().trim())
                .eq(WorkflowProcessParticipantEntity::getUserId, userId)
                .eq(WorkflowProcessParticipantEntity::getActive, true));
        WorkflowParticipationAccessVO vo = new WorkflowParticipationAccessVO();
        vo.setReadable(!rows.isEmpty());
        vo.setParticipantTypes(rows.stream().map(WorkflowProcessParticipantEntity::getParticipantType)
                .map(WorkflowParticipantType::valueOf).distinct().sorted().toList());
        vo.setLatestProcessInstanceId(rows.stream().max(Comparator.comparing(WorkflowProcessParticipantEntity::getLastParticipatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))).map(WorkflowProcessParticipantEntity::getProcessInstanceId).orElse(null));
        return vo;
    }

    @Override
    public PageResult<WorkflowParticipationBusinessVO> my(WorkflowParticipationPageQuery query) {
        Long userId = requireUser();
        String tenantId = requireTenant();
        long size = Math.min(query.getSize(), 100);
        QueryWrapper<WorkflowProcessParticipantEntity> wrapper = new QueryWrapper<WorkflowProcessParticipantEntity>()
                .select("process_key", "business_key", "MAX(last_participated_at) AS last_participated_at")
                .eq("tenant_id", tenantId)
                .eq("user_id", userId)
                .eq("active", true);
        if (StringUtils.hasText(query.getProcessKey())) {
            wrapper.eq("process_key", query.getProcessKey().trim());
        }
        if (query.getStartTime() != null) {
            wrapper.having("MAX(last_participated_at) >= {0}", query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.having("MAX(last_participated_at) <= {0}", query.getEndTime());
        }
        wrapper.groupBy("process_key", "business_key")
                .orderByDesc("last_participated_at")
                .orderByAsc("process_key", "business_key");
        Page<WorkflowProcessParticipantEntity> summaries = mapper.selectPage(
                new Page<>(query.getPage(), size), wrapper);
        if (summaries.getRecords().isEmpty()) {
            return PageResult.of(List.of(), summaries.getTotal(), summaries.getCurrent(), summaries.getSize());
        }

        LambdaQueryWrapper<WorkflowProcessParticipantEntity> details = participationRows(query, tenantId, userId);
        details.and(coordinates -> {
            for (WorkflowProcessParticipantEntity summary : summaries.getRecords()) {
                coordinates.or(group -> group
                        .eq(WorkflowProcessParticipantEntity::getProcessKey, summary.getProcessKey())
                        .eq(WorkflowProcessParticipantEntity::getBusinessKey, summary.getBusinessKey()));
            }
        }).orderByDesc(WorkflowProcessParticipantEntity::getLastParticipatedAt)
                .orderByAsc(WorkflowProcessParticipantEntity::getId);
        List<WorkflowProcessParticipantEntity> rows = mapper.selectList(details);
        Map<String, WorkflowParticipationBusinessVO> grouped = new LinkedHashMap<>();
        for (WorkflowProcessParticipantEntity row : rows) {
            String key = businessCoordinate(row);
            WorkflowParticipationBusinessVO vo = grouped.computeIfAbsent(key, ignored -> toBusiness(row));
            List<WorkflowParticipantType> types = new ArrayList<>(vo.getParticipantTypes());
            types.add(WorkflowParticipantType.valueOf(row.getParticipantType()));
            vo.setParticipantTypes(types.stream().distinct().sorted().toList());
            if (vo.getLastParticipatedAt() == null || (row.getLastParticipatedAt() != null
                    && row.getLastParticipatedAt().isAfter(vo.getLastParticipatedAt()))) {
                vo.setLastParticipatedAt(row.getLastParticipatedAt());
            }
        }
        List<WorkflowParticipationBusinessVO> result = summaries.getRecords().stream()
                .map(summary -> grouped.get(businessCoordinate(summary)))
                .filter(Objects::nonNull)
                .toList();
        return PageResult.of(result, summaries.getTotal(), summaries.getCurrent(), summaries.getSize());
    }

    private LambdaQueryWrapper<WorkflowProcessParticipantEntity> participationRows(
            WorkflowParticipationPageQuery query, String tenantId, Long userId) {
        LambdaQueryWrapper<WorkflowProcessParticipantEntity> wrapper = new LambdaQueryWrapper<WorkflowProcessParticipantEntity>()
                .eq(WorkflowProcessParticipantEntity::getTenantId, tenantId)
                .eq(WorkflowProcessParticipantEntity::getUserId, userId)
                .eq(WorkflowProcessParticipantEntity::getActive, true);
        if (StringUtils.hasText(query.getProcessKey())) {
            wrapper.eq(WorkflowProcessParticipantEntity::getProcessKey, query.getProcessKey().trim());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(WorkflowProcessParticipantEntity::getLastParticipatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(WorkflowProcessParticipantEntity::getLastParticipatedAt, query.getEndTime());
        }
        return wrapper;
    }

    private String businessCoordinate(WorkflowProcessParticipantEntity row) {
        return row.getProcessKey() + "\u0000" + row.getBusinessKey();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBusinessParticipantsVO replaceBusinessParticipants(ReplaceWorkflowBusinessParticipantsCommand command) {
        Require.notNull(command, WorkflowCode.PARTICIPANT_INVALID);
        Require.notBlank(command.getProcessKey(), WorkflowCode.PARTICIPANT_INVALID, "流程编码不能为空");
        Require.notBlank(command.getBusinessKey(), WorkflowCode.PARTICIPANT_INVALID, "业务主键不能为空");
        Require.notBlank(command.getProcessInstanceId(), WorkflowCode.PARTICIPANT_INVALID, "流程实例ID不能为空");
        Long tenantUser = requireUser();
        String tenantId = requireTenant();
        Long tenantIdValue = requireTenantLong();
        List<Long> ids = command.getParticipantUserIds() == null ? List.of() : command.getParticipantUserIds().stream()
                .filter(Objects::nonNull).distinct().toList();
        Require.isTrue(ids.size() <= 200, WorkflowCode.PARTICIPANT_INVALID, "工作流参与用户最多200个");
        Map<Long, TenantMemberVO> members = new LinkedHashMap<>();
        for (Long id : ids) {
            TenantMemberVO member = tenantMemberProvider.getEnabledMember(id, tenantIdValue);
            Require.notNull(member, WorkflowCode.PARTICIPANT_INVALID, "参与用户不属于当前租户或已停用: " + id);
            Require.isTrue(Objects.equals(member.getUserId(), id)
                    && Objects.equals(member.getTenantId(), tenantIdValue)
                    && Objects.equals(member.getStatus(), 1)
                    && member.getMemberId() != null
                    && isActiveMember(tenantIdValue, member.getMemberId()),
                    WorkflowCode.PARTICIPANT_INVALID, "参与用户不属于当前租户或已停用: " + id);
            AuthUserVO user = authUserProvider.getByIdForAuth(id);
            Require.isTrue(user != null && user.getStatus() == 1,
                    WorkflowCode.PARTICIPANT_INVALID, "参与用户不存在或账号已停用: " + id);
            members.put(id, member);
        }
        lockBusinessProcess(command, tenantId);
        List<WorkflowProcessParticipantEntity> existing = mapper.selectList(new LambdaQueryWrapper<WorkflowProcessParticipantEntity>()
                .eq(WorkflowProcessParticipantEntity::getTenantId, tenantId)
                .eq(WorkflowProcessParticipantEntity::getProcessKey, command.getProcessKey().trim())
                .eq(WorkflowProcessParticipantEntity::getBusinessKey, command.getBusinessKey().trim())
                .eq(WorkflowProcessParticipantEntity::getProcessInstanceId, command.getProcessInstanceId())
                .eq(WorkflowProcessParticipantEntity::getParticipantType, WorkflowParticipantType.BUSINESS_PARTICIPANT.name()));
        LocalDateTime now = LocalDateTime.now();
        for (WorkflowProcessParticipantEntity row : existing) {
            boolean keep = ids.contains(row.getUserId());
            row.setActive(keep);
            if (keep) {
                TenantMemberVO member = members.get(row.getUserId());
                if (member != null) {
                    row.setMemberId(member.getMemberId());
                    row.setDisplayNameSnapshot(member.getDisplayName());
                }
                AuthUserVO user = row.getUserId() == null ? null : authUserProvider.getByIdForAuth(row.getUserId());
                if (user != null) {
                    row.setUsernameSnapshot(user.getUsername());
                }
            }
            row.setUpdatedBy(tenantUser);
            row.setLastParticipatedAt(keep ? now : row.getLastParticipatedAt());
            mapper.updateById(row);
        }
        for (Long id : ids) {
            WorkflowProcessParticipantEntity row = existing.stream().filter(item -> id.equals(item.getUserId())).findFirst().orElse(null);
            if (row == null) {
                row = new WorkflowProcessParticipantEntity();
                row.setTenantId(requireTenant());
                row.setProcessKey(command.getProcessKey().trim());
                row.setBusinessKey(command.getBusinessKey().trim());
                row.setProcessInstanceId(command.getProcessInstanceId());
                row.setUserId(id);
                TenantMemberVO member = members.get(id);
                row.setMemberId(member.getMemberId());
                row.setDisplayNameSnapshot(member.getDisplayName());
                AuthUserVO user = authUserProvider.getByIdForAuth(id);
                row.setUsernameSnapshot(user == null ? null : user.getUsername());
                row.setParticipantType(WorkflowParticipantType.BUSINESS_PARTICIPANT.name());
                row.setFirstParticipatedAt(now);
                row.setCreatedBy(tenantUser);
                row.setCreatedAt(now);
            }
            row.setActive(true);
            row.setLastParticipatedAt(now);
            row.setUpdatedBy(tenantUser);
            row.setUpdatedAt(now);
            mapper.insertOrUpdate(row);
        }
        WorkflowBusinessParticipantsVO vo = new WorkflowBusinessParticipantsVO();
        vo.setProcessKey(command.getProcessKey().trim());
        vo.setBusinessKey(command.getBusinessKey().trim());
        vo.setProcessInstanceId(command.getProcessInstanceId());
        vo.setParticipantUserIds(ids);
        return vo;
    }

    private void lockBusinessProcess(ReplaceWorkflowBusinessParticipantsCommand command, String tenantId) {
        WorkflowFormInstanceEntity form = formInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowFormInstanceEntity>()
                .eq(WorkflowFormInstanceEntity::getTenantId, tenantId)
                .eq(WorkflowFormInstanceEntity::getProcessInstanceId, command.getProcessInstanceId())
                .last("for update"));
        Require.notNull(form, WorkflowCode.PARTICIPANT_INVALID, "流程实例不存在或不属于当前租户");
        Require.isTrue(command.getProcessKey().trim().equals(form.getDefinitionKey())
                        && command.getBusinessKey().trim().equals(form.getBusinessKey()),
                WorkflowCode.PARTICIPANT_INVALID, "流程实例与业务坐标不匹配");
    }

    private boolean isActiveMember(Long tenantId, Long memberId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from tenant_member where tenant_id = ? and id = ? and status = 1 and left_at is null",
                Long.class, tenantId, memberId);
        return count != null && count > 0;
    }

    @Override
    public void recordInitiator(String processKey, String businessKey, String processInstanceId,
                                Long userId, Long memberId, String username, String displayName) {
        recordParticipant(processKey, businessKey, processInstanceId, userId, memberId, username, displayName,
                WorkflowParticipantType.INITIATOR);
    }

    @Override
    public void recordParticipant(String processKey, String businessKey, String processInstanceId,
                                  Long userId, Long memberId, String username, String displayName,
                                  WorkflowParticipantType type) {
        if (userId == null || !StringUtils.hasText(processKey) || !StringUtils.hasText(businessKey)
                || !StringUtils.hasText(processInstanceId) || type == null) {
            return;
        }
        String tenantId = requireTenant();
        WorkflowProcessParticipantEntity row = mapper.selectOne(new LambdaQueryWrapper<WorkflowProcessParticipantEntity>()
                .eq(WorkflowProcessParticipantEntity::getTenantId, tenantId)
                .eq(WorkflowProcessParticipantEntity::getProcessInstanceId, processInstanceId)
                .eq(WorkflowProcessParticipantEntity::getUserId, userId)
                .eq(WorkflowProcessParticipantEntity::getParticipantType, type.name())
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new WorkflowProcessParticipantEntity();
            row.setTenantId(tenantId);
            row.setProcessKey(processKey.trim());
            row.setBusinessKey(businessKey.trim());
            row.setProcessInstanceId(processInstanceId);
            row.setUserId(userId);
            row.setParticipantType(type.name());
            row.setFirstParticipatedAt(now);
            row.setCreatedBy(MangoContextHolder.userId());
            row.setCreatedAt(now);
            mapper.insert(row);
        }
        row.setMemberId(memberId);
        row.setUsernameSnapshot(username);
        row.setDisplayNameSnapshot(displayName);
        row.setActive(true);
        row.setLastParticipatedAt(now);
        row.setUpdatedBy(MangoContextHolder.userId());
        row.setUpdatedAt(now);
        mapper.updateById(row);
    }

    @Override
    public void deactivateCurrentAssignee(String processInstanceId, Long userId) {
        if (userId == null) return;
        mapper.update(null, new LambdaUpdateWrapper<WorkflowProcessParticipantEntity>()
                .eq(WorkflowProcessParticipantEntity::getTenantId, requireTenant())
                .eq(WorkflowProcessParticipantEntity::getProcessInstanceId, processInstanceId)
                .eq(WorkflowProcessParticipantEntity::getUserId, userId)
                .eq(WorkflowProcessParticipantEntity::getParticipantType, WorkflowParticipantType.CURRENT_ASSIGNEE.name())
                .set(WorkflowProcessParticipantEntity::getActive, false)
                .set(WorkflowProcessParticipantEntity::getUpdatedBy, MangoContextHolder.userId()));
    }

    @Override
    public void deactivateCurrentAssignees(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return;
        }
        mapper.update(null, new LambdaUpdateWrapper<WorkflowProcessParticipantEntity>()
                .eq(WorkflowProcessParticipantEntity::getTenantId, requireTenant())
                .eq(WorkflowProcessParticipantEntity::getProcessInstanceId, processInstanceId)
                .eq(WorkflowProcessParticipantEntity::getParticipantType, WorkflowParticipantType.CURRENT_ASSIGNEE.name())
                .set(WorkflowProcessParticipantEntity::getActive, false)
                .set(WorkflowProcessParticipantEntity::getUpdatedBy, MangoContextHolder.userId())
                .set(WorkflowProcessParticipantEntity::getUpdatedAt, LocalDateTime.now()));
    }

    private WorkflowParticipationBusinessVO toBusiness(WorkflowProcessParticipantEntity row) {
        WorkflowParticipationBusinessVO vo = new WorkflowParticipationBusinessVO();
        vo.setProcessKey(row.getProcessKey());
        vo.setBusinessKey(row.getBusinessKey());
        vo.setProcessInstanceId(row.getProcessInstanceId());
        vo.setParticipantTypes(List.of(WorkflowParticipantType.valueOf(row.getParticipantType())));
        vo.setLastParticipatedAt(row.getLastParticipatedAt());
        return vo;
    }

    private Long requireUser() {
        Long id = MangoContextHolder.userId();
        Require.notNull(id, WorkflowCode.PARTICIPATION_CONTEXT_INVALID);
        return id;
    }

    private String requireTenant() {
        String id = MangoContextHolder.tenantId();
        Require.notBlank(id, WorkflowCode.PARTICIPATION_CONTEXT_INVALID);
        return id;
    }

    private Long requireTenantLong() {
        try {
            return Long.valueOf(requireTenant());
        } catch (NumberFormatException ex) {
            throw new io.mango.common.exception.BizException(
                    WorkflowCode.PARTICIPATION_CONTEXT_INVALID.getCode(),
                    WorkflowCode.PARTICIPATION_CONTEXT_INVALID.getMessage());
        }
    }
}
