package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.AuthorizationAppModule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 逻辑应用集成模块 Mapper。
 */
@Mapper
public interface AuthorizationAppModuleMapper extends BaseMapper<AuthorizationAppModule> {
}
