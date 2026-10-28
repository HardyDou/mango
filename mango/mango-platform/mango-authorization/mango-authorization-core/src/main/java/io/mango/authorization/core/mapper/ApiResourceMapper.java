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

    /**
     * 按模块名读取包含逻辑删除行的接口资源。
     *
     * <p>唯一键 {@code uk_authorization_api_resource_method_path} 不包含 {@code deleted}，
     * 逻辑删除行仍占用该路由。同步时必须能读到这些行并把它们复活，否则重新声明同一路由会插入冲突。
     *
     * @param moduleNames 模块名集合，调用方必须保证非空
     * @return 包含 {@code deleted = 1} 的资源行
     */
    @InterceptorIgnore(tenantLine = "true")
    List<ApiResourceEntity> selectIncludingDeletedByModuleNames(@Param("moduleNames") List<String> moduleNames);

    /**
     * 复活软删除行并把最新声明字段写回。
     *
     * <p>{@code deleted} 是 {@code @TableLogic} 字段，普通 update 会被自动追加 {@code deleted = 0} 条件，
     * 无法命中软删除行，因此必须使用显式 SQL。
     *
     * @param entity 已合并最新声明字段的资源行
     * @return 更新行数
     */
    @InterceptorIgnore(tenantLine = "true")
    int reviveSoftDeleted(@Param("entity") ApiResourceEntity entity);
}
