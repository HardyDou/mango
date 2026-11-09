package io.mango.integration.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.integration.core.entity.IntegrationInstanceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IntegrationInstance Mapper。
 */
@Mapper
public interface IntegrationInstanceMapper extends BaseMapper<IntegrationInstanceEntity> {
}
