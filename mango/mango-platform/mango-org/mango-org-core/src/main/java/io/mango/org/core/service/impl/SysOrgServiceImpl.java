package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.service.ISysOrgService;
import io.mango.org.core.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOrg::getOrgStatus, "1")
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
               .eq(SysOrg::getOrgStatus, "1")
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
        return orgMapper.selectById(id);
    }
}
