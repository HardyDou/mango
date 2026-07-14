package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowTemplateCategoryEntity;

/**
 * 流程模板分类 Mapper。
 */
@Mapper
public interface WorkflowTemplateCategoryMapper extends BaseMapper<WorkflowTemplateCategoryEntity> {
}
