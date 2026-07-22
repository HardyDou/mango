package io.mango.org.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.org.core.entity.PostEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper extends BaseMapper<PostEntity> {

    @InterceptorIgnore(tenantLine = "true")
    Long selectIdByTenantAndCode(@Param("tenantId") Long tenantId, @Param("postCode") String postCode);

    @InterceptorIgnore(tenantLine = "true")
    PostEntity selectByTenantAndCodeForUpdate(@Param("tenantId") Long tenantId,
                                              @Param("postCode") String postCode);
}
