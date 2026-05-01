package io.mango.authorization.core.service;

import io.mango.authorization.api.AuthorizationQuery;

import java.util.List;

/**
 * 授权主体权限查询服务。
 */
public interface ISubjectAuthorityService {

    default List<String> listSubjectRoles(Long subjectId) {
        return listSubjectRoles(subjectId, null);
    }

    default List<String> listSubjectRoles(Long subjectId, String appCode) {
        return listSubjectRoles(new AuthorizationQuery(
                subjectId,
                AuthorizationQuery.SUBJECT_TYPE_USER,
                null,
                appCode));
    }

    List<String> listSubjectRoles(AuthorizationQuery query);

    default List<String> listSubjectPermissions(Long subjectId) {
        return listSubjectPermissions(subjectId, null);
    }

    default List<String> listSubjectPermissions(Long subjectId, String appCode) {
        return listSubjectPermissions(new AuthorizationQuery(
                subjectId,
                AuthorizationQuery.SUBJECT_TYPE_USER,
                null,
                appCode));
    }

    List<String> listSubjectPermissions(AuthorizationQuery query);
}
