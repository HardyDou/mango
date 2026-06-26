package io.mango.org.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        SysOrg org = findByBusinessKey(resource);
        if (org == null) {
            org = new SysOrg();
            org.setTenantId(fields.requiredLong(resource, "tenantId"));
            org.setOrgCode(fields.requiredString(resource, "orgCode"));
        }
        org.setPid(parentId(resource));
        org.setOrgName(fields.requiredString(resource, "orgName"));
        org.setOrgType(fields.intField(resource, "orgType", null));
        org.setOrgSort(fields.intField(resource, "sort", 0));
        org.setOrgStatus(statusValue(resource));
        if (org.getId() == null) {
            orgMapper.insert(org);
        } else {
            orgMapper.updateById(org);
        }
        return ResourceSyncResult.of(org.getId(), TARGET_TABLE, "Org unit synced: " + org.getOrgCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        SysOrg org = findByTargetOrBusinessKey(resource);
        boolean changed = false;
        if (org != null && !"0".equals(org.getOrgStatus())) {
            org.setOrgStatus("0");
            changed = orgMapper.updateById(org) > 0;
        }
        return ResourceSyncResult.of(org == null ? null : org.getId(), TARGET_TABLE,
                "Org unit disabled: changed=" + changed);
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
        SysOrg parent = orgMapper.selectOne(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, fields.requiredLong(resource, "tenantId"))
                .eq(SysOrg::getOrgCode, parentCode.trim())
                .last("LIMIT 1"));
        if (parent == null) {
            throw new IllegalStateException("ORG_UNIT parent org does not exist: " + parentCode);
        }
        return parent.getId();
    }

    private SysOrg findByTargetOrBusinessKey(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            SysOrg org = orgMapper.selectById(targetId);
            if (org != null) {
                return org;
            }
        }
        return findByBusinessKey(resource);
    }

    private SysOrg findByBusinessKey(ResourceDeclaration resource) {
        return orgMapper.selectOne(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, fields.requiredLong(resource, "tenantId"))
                .eq(SysOrg::getOrgCode, fields.requiredString(resource, "orgCode"))
                .last("LIMIT 1"));
    }

    private String statusValue(ResourceDeclaration resource) {
        String status = fields.stringField(resource, "status");
        if (StringUtils.hasText(status)) {
            return status.trim();
        }
        return resource.getStatus() == ResourceStatus.DISABLED ? "0" : "1";
    }
}
