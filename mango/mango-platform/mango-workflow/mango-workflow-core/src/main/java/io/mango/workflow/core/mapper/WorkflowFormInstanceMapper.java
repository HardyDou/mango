package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;

/**
 * 流程实例表单快照 Mapper。
 */
@Mapper
public interface WorkflowFormInstanceMapper extends BaseMapper<WorkflowFormInstanceEntity> {
}
