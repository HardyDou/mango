package io.mango.authorization.api;

/** 将授权资源声明中的主体业务标识解析为机构成员ID。 */
public interface AuthorizationSubjectReferenceProvider {

    Long resolveMemberId(Long tenantId, String memberNo, String username);
}
