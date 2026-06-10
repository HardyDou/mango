package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobAttemptEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 执行尝试 Mapper。
 */
@Mapper
public interface MangoJobAttemptMapper extends BaseMapper<MangoJobAttemptEntity> {
}
