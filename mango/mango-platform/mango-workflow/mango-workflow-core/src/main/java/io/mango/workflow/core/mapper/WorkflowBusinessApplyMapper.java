package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;

/**
 * 业务工作流申请 Mapper。
 */
@Mapper
public interface WorkflowBusinessApplyMapper extends BaseMapper<WorkflowBusinessApplyEntity> {
}
