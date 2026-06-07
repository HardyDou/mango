package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 操作日志 Mapper。
 */
@Mapper
public interface MangoJobOperationLogMapper extends BaseMapper<MangoJobOperationLogEntity> {
}
