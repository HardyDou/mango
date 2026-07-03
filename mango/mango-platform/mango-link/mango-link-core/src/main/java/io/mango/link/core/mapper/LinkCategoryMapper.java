package io.mango.link.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.link.core.entity.LinkCategoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LinkCategoryMapper extends BaseMapper<LinkCategoryEntity> {

    @Select("SELECT * FROM link_category WHERE tenant_id = #{tenantId} AND scope = 'COMPANY' AND owner_user_id = 0 AND name = #{name} LIMIT 1")
    LinkCategoryEntity selectByName(@Param("tenantId") Long tenantId, @Param("name") String name);

    @Select("SELECT * FROM link_category WHERE tenant_id = #{tenantId} AND scope = #{scope} AND owner_user_id = #{ownerUserId} AND name = #{name} LIMIT 1")
    LinkCategoryEntity selectByScopeOwnerAndName(@Param("tenantId") Long tenantId,
                                                 @Param("scope") String scope,
                                                 @Param("ownerUserId") Long ownerUserId,
                                                 @Param("name") String name);

    @Select("SELECT * FROM link_category WHERE tenant_id = #{tenantId} AND id = #{id} LIMIT 1")
    LinkCategoryEntity selectByTenantAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
