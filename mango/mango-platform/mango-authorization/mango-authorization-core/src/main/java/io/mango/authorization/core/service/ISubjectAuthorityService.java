package io.mango.authorization.core.service;

import java.util.List;

/**
 * 授权主体权限查询服务。
 */
public interface ISubjectAuthorityService {

    default List<String> listSubjectRoles(Long subjectId) {
        return listSubjectRoles(subjectId, null);
    }

    List<String> listSubjectRoles(Long subjectId, String appCode);

    default List<String> listSubjectPermissions(Long subjectId) {
        return listSubjectPermissions(subjectId, null);
    }

    List<String> listSubjectPermissions(Long subjectId, String appCode);
}
