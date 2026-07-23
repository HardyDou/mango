package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.authorization.core.entity.MenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * System menu mapper
 *
 * @author Mango
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuEntity> {

    int DIAGNOSTIC_QUERY_TIMEOUT_SECONDS = 2;

    /** Reads the platform menu authority domain for bounded module diagnostics. */
    @InterceptorIgnore(tenantLine = "true")
    List<MenuEntity> selectDiagnosticMenus(
            @Param("tenantId") String tenantId,
            @Param("appCode") String appCode,
            @Param("menuCodes") List<String> menuCodes);
}
