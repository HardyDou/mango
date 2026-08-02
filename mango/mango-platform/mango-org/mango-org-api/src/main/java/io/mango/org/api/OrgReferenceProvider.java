package io.mango.org.api;

/** 为跨模块资源装配提供稳定的组织与岗位引用解析边界。 */
public interface OrgReferenceProvider {

    Long resolveOrgId(Long tenantId, String orgCode);

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
