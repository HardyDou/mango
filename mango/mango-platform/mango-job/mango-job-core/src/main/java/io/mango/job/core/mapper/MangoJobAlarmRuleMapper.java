package io.mango.job.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.job.core.entity.MangoJobAlarmRuleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Job 告警规则 Mapper。
 */
@Mapper
public interface MangoJobAlarmRuleMapper extends BaseMapper<MangoJobAlarmRuleEntity> {
}
