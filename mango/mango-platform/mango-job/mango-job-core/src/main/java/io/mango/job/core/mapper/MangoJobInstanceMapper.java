package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 执行实例 Mapper。
 */
@Mapper
public interface MangoJobInstanceMapper extends BaseMapper<MangoJobInstanceEntity> {
}
