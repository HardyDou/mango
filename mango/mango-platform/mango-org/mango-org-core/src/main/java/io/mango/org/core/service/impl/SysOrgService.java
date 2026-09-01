package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.common.result.Require;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.CreateTenantMemberInOrgCommand;
import io.mango.identity.api.command.RestoreTenantMemberInOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateSysOrgCommand;
import io.mango.org.api.command.CreateOrgMemberAccountCommand;
import io.mango.org.api.command.RestoreOrgMemberAccountCommand;
import io.mango.org.api.command.UpdateSysOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.enums.PostCode;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.entity.SysOrgEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.org.core.service.ISysOrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 组织业务服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysOrgService extends MangoCrudServiceImpl<SysOrgMapper, SysOrgEntity>
        implements ISysOrgService {

    private static final String DEPT_MANAGER_POST_CODE = "DEPT_MANAGER";
    private static final String ORG_MANAGER_POST_CODE = "ORG_MANAGER";
    private static final String TEAM_LEADER_POST_CODE = "TEAM_LEADER";
    private static final int MIN_ORG_TYPE = 1;
    private static final int MAX_ORG_TYPE = 4;

    private final PostMapper postMapper;
    private final TenantMemberProvider tenantMemberProvider;

    @Override
    public List<SysOrgVO> tree(SysOrgTreeQuery query) {
        SysOrgTreeQuery resolved = resolveQuery(query);
        LambdaQueryWrapper<SysOrgEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(!Boolean.TRUE.equals(resolved.getIncludeDisabled()), SysOrgEntity::getOrgStatus, "1")
                .orderByAsc(SysOrgEntity::getOrgSort)
                .orderByAsc(SysOrgEntity::getId);

        List<SysOrgEntity> orgs = list(wrapper);
        if (orgs.isEmpty()) {
            return List.of();
        }

        Map<Long, List<SysOrgEntity>> childrenByParentId = orgs.stream()
                .collect(Collectors.groupingBy(org -> defaultParentId(org.getPid())));
        Long rootParentId = defaultParentId(resolved.getParentId());
        return childrenByParentId.getOrDefault(rootParentId, List.of()).stream()
                .map(org -> buildTreeNode(org, childrenByParentId, resolved.getType()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<SysOrgVO> children(Long parentId) {
        return list(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getPid, parentId)
                .orderByAsc(SysOrgEntity::getOrgSort)
                .orderByAsc(SysOrgEntity::getId)).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateSysOrgCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "组织新增命令不能为空");
        Object id = createByCommand(command);
        Require.isTrue(id instanceof Long, PostCode.VALIDATION_ERROR, "组织ID生成失败");
        return (Long) id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(UpdateSysOrgCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "组织修改命令不能为空");
        return updateByCommand(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "组织删除命令不能为空");
        Require.notNull(command.getId(), PostCode.VALIDATION_ERROR, "组织ID不能为空");
        return deleteById(command.getId());
    }

    @Override
    public PersistencePageResult<SysOrgVO> page(SysOrgTreeQuery query) {
        SysOrgTreeQuery resolved = resolveQuery(query);
        PersistencePageResult<?> source = pageByQuery(resolved);
        List<SysOrgVO> records = source.getRecords().stream().map(SysOrgVO.class::cast).toList();
        return PersistencePageResult.of(records, source.getTotal(), source.getPage(), source.getSize());
    }

    @Override
    public SysOrgVO detail(Long id) {
        return toVO(requireOrg(id));
    }

    @Override
    public List<OrgMemberVO> members(Long orgId) {
        SysOrgEntity org = requireOrg(orgId);
        Long tenantId = org.getTenantIdAsLong();
        List<TenantMemberOrgRelationVO> relations = tenantMemberProvider.listOrgRelations(tenantId, orgId);
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        return relations.stream().map(this::toMemberVO).toList();
    }

    @Override
    public List<Long> memberScope(Long orgId) {
        SysOrgEntity root = requireEnabledOrg(orgId);
        List<SysOrgEntity> orgs = list(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getOrgStatus, "1")
                .orderByAsc(SysOrgEntity::getOrgSort)
                .orderByAsc(SysOrgEntity::getId));
        Map<Long, List<SysOrgEntity>> childrenByParentId = orgs.stream()
                .collect(Collectors.groupingBy(org -> defaultParentId(org.getPid())));
        List<Long> scope = new ArrayList<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(root.getId());
        while (!pending.isEmpty()) {
            Long currentId = pending.removeFirst();
            scope.add(currentId);
            childrenByParentId.getOrDefault(currentId, List.of()).stream()
                    .map(SysOrgEntity::getId)
                    .forEach(pending::addLast);
        }
        return scope;
    }

    @Override
    public Long createMemberAccount(CreateOrgMemberAccountCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "组织成员账号创建命令不能为空");
        SysOrgEntity org = requireEnabledOrg(command.getOrgId());
        Long tenantId = org.getTenantIdAsLong();
        if (command.getPostId() != null) {
            validatePost(tenantId, command.getPostId());
        }
        CreateTenantMemberInOrgCommand identityCommand = new CreateTenantMemberInOrgCommand();
        identityCommand.setTenantId(tenantId);
        identityCommand.setOrgId(org.getId());
        identityCommand.setPostId(command.getPostId());
        identityCommand.setUsername(command.getUsername());
        identityCommand.setPassword(command.getPassword());
        identityCommand.setNickname(command.getNickname());
        identityCommand.setEmail(command.getEmail());
        identityCommand.setPhone(command.getPhone());
        identityCommand.setStatus(command.getStatus());
        identityCommand.setRemark(command.getRemark());
        identityCommand.setPrimaryFlag(command.getPrimaryFlag() == null || command.getPrimaryFlag());
        identityCommand.setLeaderFlag(Boolean.TRUE.equals(command.getLeaderFlag()));
        identityCommand.setOperatorUserId(MangoContextHolder.userId());
        return tenantMemberProvider.createMemberInOrg(identityCommand);
    }

    @Override
    public Long restoreMemberAccount(RestoreOrgMemberAccountCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "原成员恢复命令不能为空");
        SysOrgEntity org = requireEnabledOrg(command.getOrgId());
        Long tenantId = org.getTenantIdAsLong();
        if (command.getPostId() != null) {
            validatePost(tenantId, command.getPostId());
        }
        RestoreTenantMemberInOrgCommand identityCommand = new RestoreTenantMemberInOrgCommand();
        identityCommand.setTenantId(tenantId);
        identityCommand.setOrgId(org.getId());
        identityCommand.setPostId(command.getPostId());
        identityCommand.setUsername(command.getUsername());
        identityCommand.setRealm(command.getRealm());
        identityCommand.setOperatorUserId(MangoContextHolder.userId());
        return tenantMemberProvider.restoreMemberInOrg(identityCommand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMember(AddOrgMemberCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "组织成员新增命令不能为空");
        SysOrgEntity org = requireOrg(command.getOrgId());
        Long tenantId = org.getTenantIdAsLong();
        TenantMemberVO member = tenantMemberProvider.getMember(command.getMemberId());
        Require.notNull(member, PostCode.ORG_MEMBER_NOT_FOUND);
        Require.isTrue(tenantId.equals(member.getTenantId()), PostCode.ORG_MEMBER_NOT_FOUND);
        if (command.getPostId() != null) {
            validatePost(tenantId, command.getPostId());
        }
        Require.isFalse(tenantMemberProvider.existsOrgRelation(
                tenantId, command.getMemberId(), command.getOrgId()), PostCode.ORG_MEMBER_EXISTS);

        AddTenantMemberOrgCommand addCommand = new AddTenantMemberOrgCommand();
        addCommand.setTenantId(tenantId);
        addCommand.setMemberId(command.getMemberId());
        addCommand.setOrgId(command.getOrgId());
        addCommand.setPostId(command.getPostId());
        addCommand.setPrimaryFlag(command.getPrimaryFlag());
        addCommand.setLeaderFlag(command.getLeaderFlag());
        addCommand.setOperatorUserId(MangoContextHolder.userId());
        tenantMemberProvider.addOrgRelation(addCommand);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMember(UpdateOrgMemberCommand command) {
        Require.notNull(command, PostCode.VALIDATION_ERROR, "组织成员修改命令不能为空");
        TenantMemberOrgRelationVO relation = tenantMemberProvider.getOrgRelation(command.getRelationId());
        Require.notNull(relation, PostCode.ORG_MEMBER_RELATION_NOT_FOUND);
        if (command.getPostId() != null) {
            validatePost(relation.getTenantId(), command.getPostId());
        }
        Require.notNull(tenantMemberProvider.getMember(relation.getMemberId()), PostCode.ORG_MEMBER_NOT_FOUND);
        boolean primary = Boolean.TRUE.equals(command.getPrimaryFlag());
        if (!primary && isPrimaryRelation(relation)) {
            Require.isTrue(hasOtherPrimaryCandidate(relation), PostCode.ORG_MEMBER_PRIMARY_REQUIRED);
        }

        UpdateTenantMemberOrgCommand updateCommand = new UpdateTenantMemberOrgCommand();
        updateCommand.setRelationId(command.getRelationId());
        updateCommand.setPostId(command.getPostId());
        updateCommand.setPrimaryFlag(command.getPrimaryFlag());
        updateCommand.setLeaderFlag(command.getLeaderFlag());
        updateCommand.setOperatorUserId(MangoContextHolder.userId());
        tenantMemberProvider.updateOrgRelation(updateCommand);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeMember(Long relationId) {
        TenantMemberOrgRelationVO relation = tenantMemberProvider.getOrgRelation(relationId);
        Require.notNull(relation, PostCode.ORG_MEMBER_RELATION_NOT_FOUND);
        tenantMemberProvider.removeOrgRelation(relationId);
        return true;
    }

    @Override
    public List<Long> leaderUserIds(Long orgId) {
        SysOrgEntity org = requireOrg(orgId);
        Long tenantId = org.getTenantIdAsLong();
        List<Long> leaderPostIds = leaderPostIds(tenantId);
        List<TenantMemberOrgRelationVO> relations = tenantMemberProvider.listOrgRelations(tenantId, orgId);
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        List<Long> memberIds = relations.stream()
                .filter(relation -> Boolean.TRUE.equals(relation.getLeaderFlag())
                        || relation.getPostId() != null && leaderPostIds.contains(relation.getPostId()))
                .map(TenantMemberOrgRelationVO::getMemberId)
                .distinct()
                .toList();
        if (memberIds.isEmpty()) {
            return List.of();
        }
        List<TenantMemberVO> members = tenantMemberProvider.listMembers(memberIds);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream()
                .filter(member -> member != null && Integer.valueOf(1).equals(member.getStatus()))
                .map(TenantMemberVO::getUserId)
                .distinct()
                .toList();
    }

    @Override
    protected Class<SysOrgEntity> entityType() {
        return SysOrgEntity.class;
    }

    @Override
    protected QueryWrapper<SysOrgEntity> buildQueryWrapper(Object queryObject) {
        SysOrgTreeQuery query = (SysOrgTreeQuery) queryObject;
        QueryWrapper<SysOrgEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(query.getParentId() != null, SysOrgEntity::getPid, query.getParentId())
                .eq(query.getType() != null, SysOrgEntity::getOrgType, query.getType())
                .eq(!Boolean.TRUE.equals(query.getIncludeDisabled()), SysOrgEntity::getOrgStatus, "1")
                .orderByAsc(SysOrgEntity::getOrgSort)
                .orderByAsc(SysOrgEntity::getId);
        return wrapper;
    }

    @Override
    protected void beforeCreate(Object commandObject, SysOrgEntity entity) {
        CreateSysOrgCommand command = (CreateSysOrgCommand) commandObject;
        validateOrg(command.getPid(), command.getOrgType(), command.getOrgCode(), null);
        entity.setPid(command.getPid());
        entity.setOrgName(command.getOrgName().trim());
        entity.setOrgCode(command.getOrgCode().trim());
        entity.setOrgType(command.getOrgType());
        entity.setOrgSort(defaultSort(command.getOrgSort()));
        entity.setOrgStatus(defaultStatus(command.getOrgStatus()));
    }

    @Override
    protected void beforeUpdate(Object commandObject, SysOrgEntity entity) {
        UpdateSysOrgCommand command = (UpdateSysOrgCommand) commandObject;
        SysOrgEntity existing = requireOrg(command.getId());
        validateOrg(command.getPid(), command.getOrgType(), command.getOrgCode(), command.getId());
        String orgStatus = defaultStatus(command.getOrgStatus());
        validateMove(existing, command.getPid(), orgStatus);
        entity.setPid(command.getPid());
        entity.setOrgName(command.getOrgName().trim());
        entity.setOrgCode(command.getOrgCode().trim());
        entity.setOrgType(command.getOrgType());
        entity.setOrgSort(defaultSort(command.getOrgSort()));
        entity.setOrgStatus(orgStatus);
    }

    @Override
    protected void beforeDelete(Object id) {
        SysOrgEntity org = requireOrg((Long) id);
        Require.isFalse(isRoot(org), PostCode.ORG_ROOT_DELETE_FORBIDDEN);
        long childCount = count(new LambdaQueryWrapper<SysOrgEntity>().eq(SysOrgEntity::getPid, id));
        Require.isTrue(childCount == 0, PostCode.ORG_HAS_CHILDREN);
    }

    @Override
    protected SysOrgVO toVO(SysOrgEntity entity) {
        if (entity == null) {
            return null;
        }
        SysOrgVO vo = new SysOrgVO();
        vo.setId(entity.getId());
        vo.setPid(entity.getPid());
        vo.setOrgName(entity.getOrgName());
        vo.setOrgCode(entity.getOrgCode());
        vo.setOrgType(entity.getOrgType());
        vo.setOrgSort(entity.getOrgSort());
        vo.setOrgStatus(entity.getOrgStatus());
        vo.setTenantId(entity.getTenantIdAsLong());
        return vo;
    }

    private SysOrgVO buildTreeNode(SysOrgEntity org,
                                Map<Long, List<SysOrgEntity>> childrenByParentId,
                                Integer type) {
        List<SysOrgVO> matchedChildren = childrenByParentId.getOrDefault(org.getId(), List.of()).stream()
                .map(child -> buildTreeNode(child, childrenByParentId, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        boolean matchedCurrent = type == null || type.equals(org.getOrgType());
        if (!matchedCurrent && matchedChildren.isEmpty()) {
            return null;
        }
        SysOrgVO node = toVO(org);
        node.setChildren(matchedChildren);
        return node;
    }

    private SysOrgEntity requireOrg(Long id) {
        Require.notNull(id, PostCode.VALIDATION_ERROR, "组织ID不能为空");
        SysOrgEntity org = getById(id);
        Require.notNull(org, PostCode.ORG_NOT_FOUND);
        return org;
    }

    private SysOrgEntity requireEnabledOrg(Long id) {
        SysOrgEntity org = requireOrg(id);
        Require.isTrue(Objects.equals(org.getTenantId(), MangoContextHolder.tenantId()), PostCode.ORG_NOT_FOUND);
        Require.isFalse("0".equals(org.getOrgStatus()), PostCode.ORG_NOT_FOUND);
        return org;
    }

    private void validateOrg(Long pid, Integer orgType, String orgCode, Long currentId) {
        Require.notNull(pid, PostCode.ORG_PARENT_REQUIRED);
        Require.notNull(orgType, PostCode.ORG_TYPE_REQUIRED);
        Require.isTrue(orgType >= MIN_ORG_TYPE && orgType <= MAX_ORG_TYPE, PostCode.ORG_TYPE_INVALID);
        Require.notBlank(orgCode, PostCode.ORG_CODE_REQUIRED);
        Require.isFalse(currentId == null && Long.valueOf(0L).equals(pid),
                PostCode.ORG_ROOT_MANUAL_CREATE_FORBIDDEN);
        if (pid != 0L) {
            Require.isFalse("0".equals(requireOrg(pid).getOrgStatus()), PostCode.ORG_PARENT_DISABLED);
        }
        LambdaQueryWrapper<SysOrgEntity> codeWrapper = new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getOrgCode, orgCode.trim());
        codeWrapper.ne(currentId != null, SysOrgEntity::getId, currentId);
        Require.isTrue(count(codeWrapper) == 0, PostCode.ORG_CODE_EXISTS);
    }

    private void validateMove(SysOrgEntity existing, Long targetPid, String orgStatus) {
        if (isRoot(existing)) {
            Require.isTrue(targetPid == 0L, PostCode.ORG_ROOT_MOVE_FORBIDDEN);
            Require.isFalse("0".equals(orgStatus), PostCode.ORG_ROOT_DISABLE_FORBIDDEN);
            return;
        }
        Require.isFalse(existing.getId().equals(targetPid), PostCode.ORG_PARENT_SELF_FORBIDDEN);
        Long cursor = targetPid;
        while (cursor != null && cursor != 0L) {
            Require.isFalse(existing.getId().equals(cursor), PostCode.ORG_PARENT_DESCENDANT_FORBIDDEN);
            SysOrgEntity parent = getById(cursor);
            Require.notNull(parent, PostCode.ORG_NOT_FOUND);
            cursor = parent.getPid();
        }
    }

    private boolean isRoot(SysOrgEntity org) {
        return org != null && (org.getPid() == null || org.getPid() == 0L);
    }

    private void validatePost(Long tenantId, Long postId) {
        PostEntity post = postMapper.selectById(postId);
        Require.notNull(post, PostCode.POST_NOT_FOUND);
        Require.isTrue(tenantId.equals(post.getTenantIdAsLong()), PostCode.POST_NOT_FOUND);
        Require.isFalse("0".equals(post.getPostStatus()), PostCode.POST_NOT_FOUND);
    }

    private OrgMemberVO toMemberVO(TenantMemberOrgRelationVO relation) {
        OrgMemberVO vo = new OrgMemberVO();
        vo.setRelationId(relation.getRelationId());
        vo.setMemberId(relation.getMemberId());
        vo.setOrgId(relation.getOrgId());
        vo.setPostId(relation.getPostId());
        vo.setPrimaryFlag(Boolean.TRUE.equals(relation.getPrimaryFlag()));
        vo.setUserId(relation.getUserId());
        vo.setMemberName(relation.getDisplayName());
        vo.setMemberType(relation.getMemberType());
        vo.setStatus(relation.getStatus());
        vo.setUsername(relation.getUsername());
        vo.setNickname(relation.getNickname());
        PostEntity post = null;
        if (relation.getPostId() != null) {
            post = postMapper.selectById(relation.getPostId());
        }
        if (post != null) {
            vo.setPostName(post.getPostName());
            vo.setPostCode(post.getPostCode());
        }
        vo.setLeaderFlag(isLeaderRelation(relation, post));
        return vo;
    }

    private boolean isPrimaryRelation(TenantMemberOrgRelationVO relation) {
        return Boolean.TRUE.equals(relation.getPrimaryFlag());
    }

    private boolean hasOtherPrimaryCandidate(TenantMemberOrgRelationVO relation) {
        return tenantMemberProvider.countOtherOrgRelations(
                relation.getTenantId(), relation.getMemberId(), relation.getRelationId()) > 0;
    }

    private List<Long> leaderPostIds(Long tenantId) {
        List<PostEntity> posts = postMapper.selectList(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getTenantId, tenantId)
                .eq(PostEntity::getPostStatus, "1"));
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }
        return posts.stream().filter(this::isLeaderPost).map(PostEntity::getId).distinct().toList();
    }

    private boolean isLeaderPost(PostEntity post) {
        if (post == null || !StringUtils.hasText(post.getPostCode())) {
            return false;
        }
        String code = post.getPostCode().trim().toUpperCase();
        if (code.equals(DEPT_MANAGER_POST_CODE)
                || code.equals(ORG_MANAGER_POST_CODE)
                || code.equals(TEAM_LEADER_POST_CODE)) {
            return true;
        }
        return code.endsWith("_" + DEPT_MANAGER_POST_CODE)
                || code.endsWith("_" + ORG_MANAGER_POST_CODE)
                || code.endsWith("_" + TEAM_LEADER_POST_CODE);
    }

    private Long defaultParentId(Long parentId) {
        if (parentId == null) {
            return Long.valueOf(0L);
        }
        return parentId;
    }

    private SysOrgTreeQuery resolveQuery(SysOrgTreeQuery query) {
        if (query == null) {
            return new SysOrgTreeQuery();
        }
        return query;
    }

    private Integer defaultSort(Integer sort) {
        if (sort == null) {
            return Integer.valueOf(0);
        }
        return sort;
    }

    private String defaultStatus(String status) {
        if (StringUtils.hasText(status)) {
            return status;
        }
        return "1";
    }

    private boolean isLeaderRelation(TenantMemberOrgRelationVO relation, PostEntity post) {
        return relation != null && Boolean.TRUE.equals(relation.getLeaderFlag()) || isLeaderPost(post);
    }

}
