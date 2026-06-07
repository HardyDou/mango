package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 执行日志索引 Mapper。
 */
@Mapper
public interface MangoJobLogIndexMapper extends BaseMapper<MangoJobLogIndexEntity> {
}
