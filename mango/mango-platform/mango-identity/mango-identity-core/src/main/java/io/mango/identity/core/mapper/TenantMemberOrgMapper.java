package io.mango.identity.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMemberOrgMapper extends BaseMapper<TenantMemberOrgEntity> {
}
