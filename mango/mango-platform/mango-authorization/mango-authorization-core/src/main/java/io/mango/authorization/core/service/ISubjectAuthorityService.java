package io.mango.authorization.core.service;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.ButtonDisplayRuleVO;

import java.util.List;

/**
 * 授权主体权限查询服务。
 */
public interface ISubjectAuthorityService {

    List<String> listSubjectRoles(Long subjectId);

    List<String> listSubjectRoles(Long subjectId, String appCode);

    List<String> listSubjectRoles(AuthorizationQuery query);

    List<String> listSubjectPermissions(Long subjectId);

    List<String> listSubjectPermissions(Long subjectId, String appCode);

    List<String> listSubjectPermissions(AuthorizationQuery query);

    List<ButtonDisplayRuleVO> listSubjectButtonRules(AuthorizationQuery query);
}
