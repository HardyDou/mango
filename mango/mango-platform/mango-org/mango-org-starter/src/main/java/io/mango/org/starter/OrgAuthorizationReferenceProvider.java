package io.mango.org.starter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationOrgReferenceProvider;
import io.mango.org.core.entity.SysOrgEntity;
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
        SysOrgEntity org = orgMapper.selectOne(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getTenantId, tenantId)
                .eq(SysOrgEntity::getOrgCode, orgCode.trim())
                .last("LIMIT 1"));
        if (org == null) {
            return null;
        }
        return org.getId();
    }
}
