package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.FrontendAppRegistry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 前端应用入口注册 Mapper。
 */
@Mapper
public interface FrontendAppRegistryMapper extends BaseMapper<FrontendAppRegistry> {
}
