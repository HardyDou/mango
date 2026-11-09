package io.mango.integration.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IntegrationConnector Mapper。
 */
@Mapper
public interface IntegrationConnectorMapper extends BaseMapper<IntegrationConnectorEntity> {
}
