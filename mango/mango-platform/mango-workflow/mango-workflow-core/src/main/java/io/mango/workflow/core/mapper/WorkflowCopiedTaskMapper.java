package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowCopiedTaskEntity;

/**
 * 工作流抄送待阅记录 Mapper。
 */
@Mapper
public interface WorkflowCopiedTaskMapper extends BaseMapper<WorkflowCopiedTaskEntity> {
}
