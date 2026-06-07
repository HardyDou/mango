package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 事件 Mapper。
 */
@Mapper
public interface MangoJobEventMapper extends BaseMapper<MangoJobEventEntity> {
}
