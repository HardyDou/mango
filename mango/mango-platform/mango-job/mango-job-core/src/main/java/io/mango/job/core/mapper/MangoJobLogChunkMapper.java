package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobLogChunkEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 日志分片 Mapper。
 */
@Mapper
public interface MangoJobLogChunkMapper extends BaseMapper<MangoJobLogChunkEntity> {
}
