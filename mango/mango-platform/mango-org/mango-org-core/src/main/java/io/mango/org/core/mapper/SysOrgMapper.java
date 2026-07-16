package io.mango.org.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.org.api.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Organization mapper
 *
 * @author Mango
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {

    @InterceptorIgnore(tenantLine = "true")
    Long selectIdByTenantAndCode(@Param("tenantId") Long tenantId, @Param("orgCode") String orgCode);
}
