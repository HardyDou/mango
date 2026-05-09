package io.mango.identity.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.identity.core.entity.TenantMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户成员 Mapper。
 */
@Mapper
public interface TenantMemberMapper extends BaseMapper<TenantMember> {
}
