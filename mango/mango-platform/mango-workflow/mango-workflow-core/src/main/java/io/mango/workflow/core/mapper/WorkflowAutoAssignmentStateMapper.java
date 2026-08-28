package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.workflow.core.entity.WorkflowAutoAssignmentStateEntity;
import org.apache.ibatis.annotations.Mapper;

/** 自动派单游标 Mapper。 */
@Mapper
public interface WorkflowAutoAssignmentStateMapper extends BaseMapper<WorkflowAutoAssignmentStateEntity> {
}
