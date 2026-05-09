package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantProvisionContext;
import io.mango.system.api.tenant.TenantProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 组织模块租户初始化。
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class OrgTenantProvisioner implements TenantProvisioner, TenantDependencyChecker {

    private final SysOrgMapper sysOrgMapper;
    private final PostMapper postMapper;

    @Override
    public void provision(TenantProvisionContext context) {
        ensureRootOrg(context);
        ensureDefaultPost(context, "INSTITUTION_ADMIN", "机构管理员", 1, "机构默认管理员岗位");
        ensureDefaultPost(context, "ORG_MANAGER", "组织负责人", 2, "机构默认组织管理岗位");
        ensureDefaultPost(context, "EMPLOYEE", "普通员工", 3, "机构默认员工岗位");
    }

    @Override
    public Optional<String> check(Long tenantId) {
        Long orgCount = sysOrgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId));
        if (orgCount != null && orgCount > 0) {
            return Optional.of("机构已有组织架构数据，不能直接删除");
        }
        Long postCount = postMapper.selectCount(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getTenantId, tenantId));
        if (postCount != null && postCount > 0) {
            return Optional.of("机构已有岗位数据，不能直接删除");
        }
        return Optional.empty();
    }

    private void ensureRootOrg(TenantProvisionContext context) {
        Long count = sysOrgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, context.tenantId())
                .eq(SysOrg::getPid, 0L));
        if (count != null && count > 0) {
            return;
        }
        SysOrg root = new SysOrg();
        root.setTenantId(context.tenantId());
        root.setPid(0L);
        root.setOrgName(context.tenantName());
        root.setOrgCode(context.tenantCode().toUpperCase() + "_ROOT");
        root.setOrgType(2);
        root.setOrgSort(0);
        root.setOrgStatus("1");
        sysOrgMapper.insert(root);
    }

    private void ensureDefaultPost(TenantProvisionContext context,
                                   String code,
                                   String name,
                                   int sort,
                                   String remark) {
        Long count = postMapper.selectCount(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getTenantId, context.tenantId())
                .eq(PostEntity::getPostCode, context.tenantCode().toUpperCase() + "_" + code));
        if (count != null && count > 0) {
            return;
        }
        PostEntity post = new PostEntity();
        post.setTenantId(context.tenantId());
        post.setPostCode(context.tenantCode().toUpperCase() + "_" + code);
        post.setPostName(name);
        post.setPostSort(sort);
        post.setPostStatus("1");
        post.setRemark(remark);
        postMapper.insert(post);
    }
}
