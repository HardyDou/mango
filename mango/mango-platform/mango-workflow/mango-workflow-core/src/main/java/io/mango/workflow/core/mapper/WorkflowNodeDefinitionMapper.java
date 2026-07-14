package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowNodeDefinitionEntity;

/**
 * 流程节点定义 Mapper。
 */
@Mapper
public interface WorkflowNodeDefinitionMapper extends BaseMapper<WorkflowNodeDefinitionEntity> {
}
