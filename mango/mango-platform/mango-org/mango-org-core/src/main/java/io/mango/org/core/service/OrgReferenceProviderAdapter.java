package io.mango.org.core.service;

import io.mango.org.api.OrgReferenceProvider;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
