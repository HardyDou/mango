package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.service.ISysOrgService;
import io.mango.org.core.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Organization service implementation
 * <p>
 * Uses lazy loading for tree structure to handle large datasets.
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
        wrapper.eq(parentId != null, SysOrg::getPid, parentId)
               .eq(type != null, SysOrg::getOrgType, type)
               .eq(SysOrg::getOrgStatus, "1")
               .orderByAsc(SysOrg::getOrgSort)
               .orderByAsc(SysOrg::getId);

        return orgMapper.selectList(wrapper);
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

    @Override
    public SysOrg getById(Long id) {
        return orgMapper.selectById(id);
    }
}
