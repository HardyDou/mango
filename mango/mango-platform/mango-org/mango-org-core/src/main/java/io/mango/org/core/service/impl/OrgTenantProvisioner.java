package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.entity.SysOrgEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantProvisionCommand;
import io.mango.system.api.tenant.TenantProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 组织模块租户初始化。
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class OrgTenantProvisioner implements TenantProvisioner, TenantDependencyChecker {

    private static final int EMPLOYEE_POST_SORT = 3;

    private final SysOrgMapper sysOrgMapper;
    private final PostMapper postMapper;

    @Override
    public void provision(TenantProvisionCommand context) {
        ensureRootOrg(context);
        ensureDefaultPost(context, "INSTITUTION_ADMIN", "机构管理员", 1, "机构默认管理员岗位");
        ensureDefaultPost(context, "DEPT_MANAGER", "部门负责人", 2, "工作流部门主管审批默认岗位");
        ensureDefaultPost(context, "EMPLOYEE", "普通员工", EMPLOYEE_POST_SORT, "机构默认员工岗位");
    }

    @Override
    public Optional<String> check(Long tenantId) {
        Long orgCount = sysOrgMapper.selectCount(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getTenantId, tenantId));
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

    private void ensureRootOrg(TenantProvisionCommand context) {
        Long count = sysOrgMapper.selectCount(new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getTenantId, context.getTenantId())
                .eq(SysOrgEntity::getPid, 0L));
        if (count != null && count > 0) {
            return;
        }
        SysOrgEntity root = new SysOrgEntity();
        root.setTenantId(context.getTenantId());
        root.setPid(0L);
        root.setOrgName(context.getTenantName());
        root.setOrgCode(context.getTenantCode().toUpperCase() + "_ROOT");
        root.setOrgType(2);
        root.setOrgSort(0);
        root.setOrgStatus("1");
        try {
            sysOrgMapper.insert(root);
        } catch (DuplicateKeyException exception) {
            Long winnerId = sysOrgMapper.selectIdByTenantAndCodeForUpdate(
                    context.getTenantId(), root.getOrgCode());
            if (winnerId == null) {
                throw exception;
            }
        }
    }

    private void ensureDefaultPost(TenantProvisionCommand context,
                                   String code,
                                   String name,
                                   int sort,
                                   String remark) {
        String postCode = context.getTenantCode().toUpperCase() + "_" + code;
        Long count = postMapper.selectCount(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getTenantId, context.getTenantId())
                .eq(PostEntity::getPostCode, postCode));
        if (count != null && count > 0) {
            return;
        }
        PostEntity post = new PostEntity();
        post.setTenantId(context.getTenantId());
        post.setPostCode(postCode);
        post.setPostName(name);
        post.setPostSort(sort);
        post.setPostStatus("1");
        post.setRemark(remark);
        try {
            postMapper.insert(post);
        } catch (DuplicateKeyException exception) {
            if (postMapper.selectByTenantAndCodeForUpdate(context.getTenantId(), postCode) == null) {
                throw exception;
            }
        }
    }
}
