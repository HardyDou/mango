package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobScheduleCursorEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 调度游标 Mapper。
 */
@Mapper
public interface MangoJobScheduleCursorMapper extends BaseMapper<MangoJobScheduleCursorEntity> {
}
