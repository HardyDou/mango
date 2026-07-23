package io.mango.authorization.diagnostic;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.entity.MenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Bounded, explicit-scope queries used only by module diagnostics.
 */
@Mapper
public interface AuthorizationDiagnosticMapper {

    /** Query timeout in seconds for every diagnostic statement. */
    int QUERY_TIMEOUT_SECONDS = 2;

    @InterceptorIgnore(tenantLine = "true")
    @Options(timeout = QUERY_TIMEOUT_SECONDS)
    @Select({
            "<script>",
            "select app_code, module_code, menu_code, component, api_codes, status, del_flag",
            "from authorization_menu",
            "where tenant_id = #{tenantId}",
            "and app_code = #{appCode}",
            "and del_flag = 0",
            "and menu_code in",
            "<foreach collection='menuCodes' item='menuCode' open='(' separator=',' close=')'>",
            "#{menuCode}",
            "</foreach>",
            "</script>"
    })
    List<MenuEntity> selectMenus(
            @Param("tenantId") String tenantId,
            @Param("appCode") String appCode,
            @Param("menuCodes") List<String> menuCodes);

    @InterceptorIgnore(tenantLine = "true")
    @Options(timeout = QUERY_TIMEOUT_SECONDS)
    @Select({
            "select module_name, http_method, path_pattern, resource_code, permission_code,",
            "access_mode, status, deleted",
            "from authorization_api_resource",
            "where tenant_id = #{tenantId}",
            "and module_name = #{moduleName}",
            "and deleted = 0"
    })
    List<ApiResourceEntity> selectApis(
            @Param("tenantId") String tenantId,
            @Param("moduleName") String moduleName);
}
