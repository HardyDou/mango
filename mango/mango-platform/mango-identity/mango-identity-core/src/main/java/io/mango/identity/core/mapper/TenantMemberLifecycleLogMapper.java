package io.mango.identity.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.identity.core.entity.TenantMemberLifecycleLogEntity;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for append-only tenant member lifecycle events. */
@Mapper
public interface TenantMemberLifecycleLogMapper extends BaseMapper<TenantMemberLifecycleLogEntity> {
}
