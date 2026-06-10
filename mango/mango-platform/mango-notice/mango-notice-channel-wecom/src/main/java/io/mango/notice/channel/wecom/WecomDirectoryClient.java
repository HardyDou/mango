package io.mango.notice.channel.wecom;

import java.util.List;

public interface WecomDirectoryClient {

    List<WecomDirectoryUser> listUsers(String corpId, String secret);

    List<WecomDirectoryUser> listUsers(String corpId, String secret, Long departmentId, boolean fetchChild);

    List<WecomDepartment> listDepartments(String corpId, String secret, Long departmentId);
}
