package io.mango.org.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.core.entity.SysOrgEntity;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource handler for organization unit declarations.
 */
@Component
@RequiredArgsConstructor
public class OrgUnitResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "sys_org";

    private final SysOrgMapper orgMapper;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.ORG_UNIT);

    @Override
    public String resourceType() {
        return ResourceTypes.ORG_UNIT;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("orgCode")
                .requiredField("orgName")
                .requiredField("orgType")
                .fieldDescription("parentOrgCode", "父组织编码；未配置时 parentId 默认为 0。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        SysOrgEntity org = findByTargetOrBusinessKey(resource);
        boolean creating = org == null;
        if (creating) {
            org = new SysOrgEntity();
            org.setId(fields.longField(resource, "targetId"));
            org.setTenantId(fields.requiredLong(resource, "tenantId"));
            org.setOrgCode(fields.requiredString(resource, "orgCode"));
        }
        org.setPid(parentId(resource));
        org.setOrgName(fields.requiredString(resource, "orgName"));
        org.setOrgType(fields.intField(resource, "orgType", null));
        org.setOrgSort(fields.intField(resource, "sort", 0));
        org.setOrgStatus(statusValue(resource));
        if (creating) {
            orgMapper.insert(org);
        } else {
            orgMapper.updateById(org);
        }
        return ResourceSyncResult.of(org.getId(), TARGET_TABLE, "Org unit synced: " + org.getOrgCode());
    }

    @Override
    public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
        List<ResourceDeclaration> pending = resources.stream()
                .filter(this::isManagedSync)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
        Set<String> syncedOrgCodes = new HashSet<>();
        Set<String> declaredOrgCodes = collectDeclaredOrgCodes(resources);
        collectProtectedOrgCodes(resources, syncedOrgCodes);
        while (!pending.isEmpty()) {
            boolean progressed = false;
            for (int i = 0; i < pending.size(); i++) {
                ResourceDeclaration resource = pending.get(i);
                if (!dependsOnPendingParent(resource, declaredOrgCodes, syncedOrgCodes)) {
                    results.put(resource.getId(), upsert(resource));
                    syncedOrgCodes.add(orgCode(resource));
                    pending.remove(i);
                    i--;
                    progressed = true;
                }
            }
            if (!progressed) {
                throw new IllegalStateException("ORG_UNIT resources have unresolved parent dependencies");
            }
        }
        return results;
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        SysOrgEntity org = findByTargetOrBusinessKey(resource);
        boolean changed = false;
        if (org != null && !"0".equals(org.getOrgStatus())) {
            org.setOrgStatus("0");
            changed = orgMapper.updateById(org) > 0;
        }
        Long targetId = null;
        if (org != null) {
            targetId = org.getId();
        }
        return ResourceSyncResult.of(targetId, TARGET_TABLE,
                "Org unit disabled: changed=" + changed);
    }

    private boolean isManagedSync(ResourceDeclaration resource) {
        return resource.getSyncMode() == null
                || resource.getSyncMode() == ResourceSyncMode.AUTO
                || resource.getSyncMode() == ResourceSyncMode.INIT_ONLY;
    }

    private Set<String> collectDeclaredOrgCodes(List<ResourceDeclaration> resources) {
        Set<String> orgCodes = new HashSet<>();
        for (ResourceDeclaration resource : resources) {
            orgCodes.add(orgCode(resource));
        }
        return orgCodes;
    }

    private void collectProtectedOrgCodes(List<ResourceDeclaration> resources, Set<String> orgCodes) {
        for (ResourceDeclaration resource : resources) {
            if (!isManagedSync(resource)) {
                orgCodes.add(orgCode(resource));
            }
        }
    }

    private boolean dependsOnPendingParent(
            ResourceDeclaration resource,
            Set<String> declaredOrgCodes,
            Set<String> syncedOrgCodes) {
        String parentCode = fields.stringField(resource, "parentOrgCode");
        if (!StringUtils.hasText(parentCode)) {
            return false;
        }
        String normalizedParentCode = parentCode.trim();
        return declaredOrgCodes.contains(normalizedParentCode) && !syncedOrgCodes.contains(normalizedParentCode);
    }

    private String orgCode(ResourceDeclaration resource) {
        return fields.requiredString(resource, "orgCode");
    }

    private Long parentId(ResourceDeclaration resource) {
        Long parentId = fields.longField(resource, "parentId");
        if (parentId != null) {
            return parentId;
        }
        String parentCode = fields.stringField(resource, "parentOrgCode");
        if (!StringUtils.hasText(parentCode)) {
            return 0L;
        }
        SysOrgEntity parent = orgMapper.selectOne(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getTenantId, fields.requiredLong(resource, "tenantId"))
                .eq(SysOrgEntity::getOrgCode, parentCode.trim())
                .last("LIMIT 1"));
        if (parent == null) {
            throw new IllegalStateException("ORG_UNIT parent org does not exist: " + parentCode);
        }
        return parent.getId();
    }

    private SysOrgEntity findByTargetOrBusinessKey(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            SysOrgEntity org = orgMapper.selectById(targetId);
            if (org != null) {
                return org;
            }
        }
        return findByBusinessKey(resource);
    }

    private SysOrgEntity findByBusinessKey(ResourceDeclaration resource) {
        return orgMapper.selectOne(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getTenantId, fields.requiredLong(resource, "tenantId"))
                .eq(SysOrgEntity::getOrgCode, fields.requiredString(resource, "orgCode"))
                .last("LIMIT 1"));
    }

    private String statusValue(ResourceDeclaration resource) {
        String status = fields.stringField(resource, "status");
        if (StringUtils.hasText(status)) {
            return status.trim();
        }
        if (resource.getStatus() == ResourceStatus.DISABLED) {
            return "0";
        }
        return "1";
    }
}
