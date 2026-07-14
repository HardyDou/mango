package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowTemplateEntity;

/**
 * 流程模板 Mapper。
 */
@Mapper
public interface WorkflowTemplateMapper extends BaseMapper<WorkflowTemplateEntity> {
}
