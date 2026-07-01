package io.mango.link.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.link.core.entity.LinkItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LinkItemMapper extends BaseMapper<LinkItemEntity> {

    @Select("SELECT * FROM link_item WHERE tenant_id = #{tenantId} AND id = #{id} LIMIT 1")
    LinkItemEntity selectByTenantAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Select("SELECT * FROM link_item WHERE tenant_id = #{tenantId} AND url = #{url} AND status = 'ENABLED' ORDER BY updated_at DESC LIMIT 1")
    LinkItemEntity selectEnabledByTenantAndUrl(@Param("tenantId") Long tenantId, @Param("url") String url);
}
