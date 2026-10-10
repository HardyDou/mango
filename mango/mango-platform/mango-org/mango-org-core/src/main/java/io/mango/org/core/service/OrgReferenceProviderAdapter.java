package io.mango.org.core.service;

import io.mango.org.api.OrgReferenceProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.core.entity.SysOrgEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 组织域引用解析的本地实现。 */
@Component
@RequiredArgsConstructor
public class OrgReferenceProviderAdapter implements OrgReferenceProvider {

    private final SysOrgMapper orgMapper;
    private final PostMapper postMapper;

    @Override
    public Long resolveOrgId(Long tenantId, String orgCode) {
        if (tenantId == null || orgCode == null || orgCode.isBlank()) {
            return null;
        }
        return orgMapper.selectIdByTenantAndCode(tenantId, orgCode.trim());
    }

    @Override
    public Long resolveRootOrgId(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return orgMapper.selectRootIdByTenant(tenantId);
    }

    @Override
    public List<Long> resolveOrgScope(Long tenantId, Long orgId, boolean includeDescendants) {
        if (tenantId == null || orgId == null) {
            return List.of();
        }
        List<SysOrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getTenantId, tenantId)
                .eq(SysOrgEntity::getOrgStatus, "1"));
        Map<Long, List<SysOrgEntity>> children = orgs.stream()
                .collect(Collectors.groupingBy(org -> org.getPid() == null ? 0L : org.getPid()));
        if (!orgs.stream().anyMatch(org -> org.getId().equals(orgId))) {
            return List.of();
        }
        if (!includeDescendants) {
            return List.of(orgId);
        }
        List<Long> result = new java.util.ArrayList<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(orgId);
        while (!pending.isEmpty()) {
            Long current = pending.removeFirst();
            result.add(current);
            children.getOrDefault(current, List.of()).stream()
                    .map(SysOrgEntity::getId)
                    .forEach(pending::addLast);
        }
        return result;
    }

    @Override
    public Long resolvePostId(Long tenantId, String postCode) {
        if (tenantId == null || postCode == null || postCode.isBlank()) {
            return null;
        }
        return postMapper.selectIdByTenantAndCode(tenantId, postCode.trim());
    }

    @Override
    public String resolveOrgName(Long tenantId, Long orgId) {
        if (tenantId == null || orgId == null) {
            return null;
        }
        return orgMapper.selectNameByTenantAndId(tenantId, orgId);
    }
}
