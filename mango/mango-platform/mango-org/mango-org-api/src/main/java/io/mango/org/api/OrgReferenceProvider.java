package io.mango.org.api;

/** 为跨模块资源装配提供稳定的组织与岗位引用解析边界。 */
public interface OrgReferenceProvider {

    Long resolveOrgId(Long tenantId, String orgCode);

    Long resolvePostId(Long tenantId, String postCode);
}
