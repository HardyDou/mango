package io.mango.integration.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.integration.core.entity.IntegrationCallLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IntegrationCallLog Mapper。
 */
@Mapper
public interface IntegrationCallLogMapper extends BaseMapper<IntegrationCallLogEntity> {
}
