package io.mango.org.api.entity;

import io.mango.org.api.vo.SysOrgVO;

/**
 * 兼容旧版组织数据对象，不再承载数据库映射职责。
 *
 * @deprecated 请使用 {@link SysOrgVO}。
 */
@Deprecated(forRemoval = false)
public class SysOrg extends SysOrgVO {

    /**
     * 从当前组织视图创建兼容对象。
     *
     * @param source 当前组织视图
     * @return 兼容对象
     */
    public static SysOrg from(SysOrgVO source) {
        if (source == null) {
            return null;
        }
        SysOrg target = new SysOrg();
        target.setId(source.getId());
        target.setPid(source.getPid());
        target.setOrgName(source.getOrgName());
        target.setOrgCode(source.getOrgCode());
        target.setOrgType(source.getOrgType());
        target.setOrgSort(source.getOrgSort());
        target.setOrgStatus(source.getOrgStatus());
        target.setTenantId(source.getTenantId());
        target.setChildren(source.getChildren());
        return target;
    }
}
