package io.mango.authorization.core.service;

import java.util.List;

/**
 * Internal authority lookup for authorization subjects.
 */
public interface ISubjectAuthorityService {

    List<String> listSubjectRoles(Long subjectId);

    List<String> listSubjectPermissions(Long subjectId);
}
