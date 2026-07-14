package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowBusinessApplyCurrentTaskEntity;

/**
 * 业务工作流申请当前任务 Mapper。
 */
@Mapper
public interface WorkflowBusinessApplyCurrentTaskMapper extends BaseMapper<WorkflowBusinessApplyCurrentTaskEntity> {
}
