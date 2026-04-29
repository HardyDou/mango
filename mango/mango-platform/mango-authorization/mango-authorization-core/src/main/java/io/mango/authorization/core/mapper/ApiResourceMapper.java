package io.mango.authorization.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.authorization.core.entity.ApiResource;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 资源 Mapper。
 *
 * @author hardy
 */
@Mapper
public interface ApiResourceMapper extends BaseMapper<ApiResource> {
}
