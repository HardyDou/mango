package io.mango.org.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.org.core.entity.SysOrgEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Organization mapper
 *
 * @author Mango
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrgEntity> {

    @InterceptorIgnore(tenantLine = "true")
    Long selectIdByTenantAndCode(@Param("tenantId") Long tenantId, @Param("orgCode") String orgCode);

    @InterceptorIgnore(tenantLine = "true")
    Long selectIdByTenantAndCodeForUpdate(@Param("tenantId") Long tenantId,
                                          @Param("orgCode") String orgCode);

    @InterceptorIgnore(tenantLine = "true")
    String selectNameByTenantAndId(@Param("tenantId") Long tenantId, @Param("orgId") Long orgId);
}
