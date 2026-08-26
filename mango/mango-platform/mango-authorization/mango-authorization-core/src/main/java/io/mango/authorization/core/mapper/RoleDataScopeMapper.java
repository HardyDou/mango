package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.RoleDataScopeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色数据权限 Mapper。
 */
@Mapper
public interface RoleDataScopeMapper extends BaseMapper<RoleDataScopeEntity> {

    /**
     * Reads the exact Resource Registry target before a tenant context can be established.
     *
     * @param id stable target ID returned by the original resource synchronization
     * @return role data scope target, or {@code null} when the registry target no longer exists
     */
    @InterceptorIgnore(tenantLine = "true")
    RoleDataScopeEntity selectRegistryTargetById(@Param("id") Long id);
}
