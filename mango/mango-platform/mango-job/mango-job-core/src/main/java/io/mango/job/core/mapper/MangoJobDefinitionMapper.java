package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 任务定义 Mapper。
 */
@Mapper
public interface MangoJobDefinitionMapper extends BaseMapper<MangoJobDefinitionEntity> {
}
