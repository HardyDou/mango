package io.mango.authorization.api;

/** 将授权资源声明中的组织编码解析为组织ID。 */
public interface AuthorizationOrgReferenceProvider {

    Long resolveOrgId(Long tenantId, String orgCode);
}
