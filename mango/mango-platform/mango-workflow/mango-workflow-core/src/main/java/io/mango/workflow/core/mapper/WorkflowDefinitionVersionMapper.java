package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowDefinitionVersionEntity;

/**
 * 流程定义发布版本 Mapper。
 */
@Mapper
public interface WorkflowDefinitionVersionMapper extends BaseMapper<WorkflowDefinitionVersionEntity> {
}
