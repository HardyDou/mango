package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.AuthorizationApp;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权应用入口 Mapper。
 */
@Mapper
public interface AuthorizationAppMapper extends BaseMapper<AuthorizationApp> {
}
