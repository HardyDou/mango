package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowCategoryEntity;

/**
 * 流程分类 Mapper。
 */
@Mapper
public interface WorkflowCategoryMapper extends BaseMapper<WorkflowCategoryEntity> {
}
