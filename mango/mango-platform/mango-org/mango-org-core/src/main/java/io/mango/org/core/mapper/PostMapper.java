package io.mango.org.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.org.core.entity.PostEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<PostEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("select id from org_post where tenant_id = #{tenantId} and post_code = #{postCode} limit 1")
    Long selectIdByTenantAndCode(@Param("tenantId") Long tenantId, @Param("postCode") String postCode);
}
