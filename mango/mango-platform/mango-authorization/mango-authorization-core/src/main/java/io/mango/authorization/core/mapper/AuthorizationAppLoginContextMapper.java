package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.AuthorizationAppLoginContext;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权应用登录上下文 Mapper。
 */
@Mapper
public interface AuthorizationAppLoginContextMapper extends BaseMapper<AuthorizationAppLoginContext> {
}
