package io.mango.org.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.org.api.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Organization mapper
 *
 * @author Mango
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("select id from sys_org where tenant_id = #{tenantId} and org_code = #{orgCode} limit 1")
    Long selectIdByTenantAndCode(@Param("tenantId") Long tenantId, @Param("orgCode") String orgCode);
}
