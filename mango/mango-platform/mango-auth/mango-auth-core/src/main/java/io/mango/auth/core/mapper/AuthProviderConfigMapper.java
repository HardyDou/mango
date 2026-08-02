package io.mango.auth.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.auth.core.entity.AuthProviderConfigEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthProviderConfigMapper extends BaseMapper<AuthProviderConfigEntity> {
}
