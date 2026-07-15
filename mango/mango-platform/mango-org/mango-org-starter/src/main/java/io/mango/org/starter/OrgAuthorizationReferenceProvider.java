package io.mango.org.starter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationOrgReferenceProvider;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 基于组织域数据解析授权资源中的组织引用。 */
@Component
@RequiredArgsConstructor
public class OrgAuthorizationReferenceProvider implements AuthorizationOrgReferenceProvider {

    private final SysOrgMapper orgMapper;

    @Override
    public Long resolveOrgId(Long tenantId, String orgCode) {
        if (tenantId == null || orgCode == null || orgCode.isBlank()) {
            return null;
        }
        SysOrg org = orgMapper.selectOne(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getOrgCode, orgCode.trim())
                .last("LIMIT 1"));
        return org == null ? null : org.getId();
    }
}
