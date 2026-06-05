package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobEngineMappingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 引擎映射 Mapper。
 */
@Mapper
public interface MangoJobEngineMappingMapper extends BaseMapper<MangoJobEngineMappingEntity> {
}
