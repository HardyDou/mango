package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.enums.PostCode;
import io.mango.org.core.service.ISysOrgService;
import io.mango.org.core.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * Organization service implementation
 * <p>
 * Builds a complete organization tree. Organization data is small enough to
 * load as a tree for selector and management pages.
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOrgServiceImpl implements ISysOrgService {

    private final SysOrgMapper orgMapper;

    @Override
    public List<SysOrg> tree(Long parentId, Integer type) {
        return tree(parentId, type, false);
    }

    @Override
    public List<SysOrg> tree(Long parentId, Integer type, boolean includeDisabled) {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(!includeDisabled, SysOrg::getOrgStatus, "1")
               .orderByAsc(SysOrg::getOrgSort)
               .orderByAsc(SysOrg::getId);

        List<SysOrg> orgs = orgMapper.selectList(wrapper);
        if (orgs == null || orgs.isEmpty()) {
            return List.of();
        }

        Map<Long, List<SysOrg>> childrenByParentId = orgs.stream()
                .collect(Collectors.groupingBy(org -> org.getPid() == null ? 0L : org.getPid()));

        Long rootParentId = parentId == null ? 0L : parentId;
        return childrenByParentId.getOrDefault(rootParentId, List.of()).stream()
                .filter(org -> type == null || type.equals(org.getOrgType()))
                .map(org -> buildTreeNode(org, childrenByParentId))
                .collect(Collectors.toList());
    }

    @Override
    public List<SysOrg> children(Long parentId) {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOrg::getPid, parentId)
               .orderByAsc(SysOrg::getOrgSort)
               .orderByAsc(SysOrg::getId);

        return orgMapper.selectList(wrapper);
    }

    private SysOrg buildTreeNode(SysOrg org, Map<Long, List<SysOrg>> childrenByParentId) {
        org.setChildren(childrenByParentId.getOrDefault(org.getId(), List.of()).stream()
                .map(child -> buildTreeNode(child, childrenByParentId))
                .collect(Collectors.toCollection(ArrayList::new)));
        return org;
    }

    @Override
    public SysOrg getById(Long id) {
        SysOrg org = orgMapper.selectById(id);
        Require.notNull(org, PostCode.ORG_NOT_FOUND);
        return org;
    }

    @Override
    public Long create(CreateOrgCommand command) {
        validateOrg(command.getPid(), command.getOrgType(), command.getOrgCode(), null);
        SysOrg org = new SysOrg();
        org.setPid(command.getPid());
        org.setOrgName(command.getOrgName());
        org.setOrgCode(command.getOrgCode());
        org.setOrgType(command.getOrgType());
        org.setOrgSort(command.getOrgSort() == null ? 0 : command.getOrgSort());
        org.setOrgStatus(StringUtils.hasText(command.getOrgStatus()) ? command.getOrgStatus() : "1");
        orgMapper.insert(org);
        return org.getId();
    }

    @Override
    public void update(UpdateOrgCommand command) {
        SysOrg existing = getById(command.getId());
        validateOrg(command.getPid(), command.getOrgType(), command.getOrgCode(), command.getId());
        String orgStatus = StringUtils.hasText(command.getOrgStatus()) ? command.getOrgStatus() : "1";
        validateMove(existing, command.getPid(), orgStatus);

        SysOrg org = new SysOrg();
        org.setId(command.getId());
        org.setPid(command.getPid());
        org.setOrgName(command.getOrgName());
        org.setOrgCode(command.getOrgCode());
        org.setOrgType(command.getOrgType());
        org.setOrgSort(command.getOrgSort() == null ? 0 : command.getOrgSort());
        org.setOrgStatus(orgStatus);
        orgMapper.updateById(org);
    }

    @Override
    public void delete(Long id) {
        SysOrg org = getById(id);
        Require.isFalse(isRoot(org), PostCode.ORG_ROOT_DELETE_FORBIDDEN);
        Long childCount = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getPid, id));
        Require.isTrue(childCount == null || childCount == 0, PostCode.ORG_HAS_CHILDREN);
        orgMapper.deleteById(id);
    }

    private void validateOrg(Long pid, Integer orgType, String orgCode, Long currentId) {
        Require.notNull(pid, PostCode.ORG_PARENT_REQUIRED);
        Require.notNull(orgType, PostCode.ORG_TYPE_REQUIRED);
        Require.isTrue(orgType >= 1 && orgType <= 4, PostCode.ORG_TYPE_INVALID);
        Require.notBlank(orgCode, PostCode.ORG_CODE_REQUIRED);
        Require.isFalse(currentId == null && Long.valueOf(0L).equals(pid), PostCode.ORG_ROOT_MANUAL_CREATE_FORBIDDEN);

        if (pid != 0L) {
            SysOrg parent = getById(pid);
            Require.isFalse("0".equals(parent.getOrgStatus()), PostCode.ORG_PARENT_DISABLED);
        }

        LambdaQueryWrapper<SysOrg> codeWrapper = new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getOrgCode, orgCode);
        if (currentId != null) {
            codeWrapper.ne(SysOrg::getId, currentId);
        }
        Long count = orgMapper.selectCount(codeWrapper);
        Require.isTrue(count == null || count == 0, PostCode.ORG_CODE_EXISTS);
    }

    private void validateMove(SysOrg existing, Long targetPid, String orgStatus) {
        if (isRoot(existing)) {
            Require.isTrue(targetPid == 0L, PostCode.ORG_ROOT_MOVE_FORBIDDEN);
            Require.isFalse("0".equals(orgStatus), PostCode.ORG_ROOT_DISABLE_FORBIDDEN);
            return;
        }
        Require.isFalse(existing.getId().equals(targetPid), PostCode.ORG_PARENT_SELF_FORBIDDEN);
        Long cursor = targetPid;
        while (cursor != null && cursor != 0L) {
            Require.isFalse(existing.getId().equals(cursor), PostCode.ORG_PARENT_DESCENDANT_FORBIDDEN);
            SysOrg parent = orgMapper.selectById(cursor);
            if (parent == null) {
                return;
            }
            cursor = parent.getPid();
        }
    }

    private boolean isRoot(SysOrg org) {
        return org != null && (org.getPid() == null || org.getPid() == 0L);
    }
}
