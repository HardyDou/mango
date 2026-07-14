package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowBusinessApplyStatusLogEntity;

/**
 * 业务工作流申请状态流水 Mapper。
 */
@Mapper
public interface WorkflowBusinessApplyStatusLogMapper extends BaseMapper<WorkflowBusinessApplyStatusLogEntity> {
}
