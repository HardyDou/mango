package io.mango.org.api;

import java.util.List;

/** 为跨模块资源装配提供稳定的组织与岗位引用解析边界。 */
public interface OrgReferenceProvider {

    Long resolveOrgId(Long tenantId, String orgCode);

    /**
     * Resolve the enabled root organization for a tenant.
     *
     * @param tenantId tenant ID
     * @return root organization ID, or {@code null} when it does not exist
     */
    default Long resolveRootOrgId(Long tenantId) {
        return null;
    }

    /**
     * Resolve an enabled organization and, when requested, its enabled descendants.
     *
     * @param tenantId tenant ID
     * @param orgId selected organization ID
     * @param includeDescendants whether descendants are included
     * @return organization IDs in the requested scope
     */
    default List<Long> resolveOrgScope(Long tenantId, Long orgId, boolean includeDescendants) {
        return orgId == null ? List.of() : List.of(orgId);
    }

    Long resolvePostId(Long tenantId, String postCode);

    /**
     * 按机构和组织 ID 解析组织名称。
     *
     * <p>默认返回空，允许只实现编码解析的既有业务适配器保持兼容。</p>
     */
    default String resolveOrgName(Long tenantId, Long orgId) {
        return null;
    }
}
