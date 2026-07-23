package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.authorization.core.entity.ApiResourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * API 资源 Mapper。
 *
 * @author hardy
 */
@Mapper
public interface ApiResourceMapper extends BaseMapper<ApiResourceEntity> {

    int DIAGNOSTIC_QUERY_TIMEOUT_SECONDS = 2;

    /** Reads the global API resource authority domain for bounded module diagnostics. */
    @InterceptorIgnore(tenantLine = "true")
    List<ApiResourceEntity> selectDiagnosticApis(
            @Param("tenantId") String tenantId,
            @Param("moduleName") String moduleName);
}
