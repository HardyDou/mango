package io.mango.identity.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.identity.core.entity.IdentityUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 身份用户 Mapper。
 */
@Mapper
public interface IdentityUserMapper extends BaseMapper<IdentityUserEntity> {
}
