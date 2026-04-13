package io.mango.auth.api;

import java.util.Set;

public interface IAuthPermissionServiceProvider {

    Set<String> getAllPermissionCodes();

    void addPermission(String permCode, String module, String action);
}
