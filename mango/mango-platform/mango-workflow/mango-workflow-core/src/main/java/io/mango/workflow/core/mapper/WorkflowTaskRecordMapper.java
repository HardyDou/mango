package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.mango.workflow.core.entity.WorkflowTaskRecordEntity;

/**
 * 工作流任务处理记录 Mapper。
 */
@Mapper
public interface WorkflowTaskRecordMapper extends BaseMapper<WorkflowTaskRecordEntity> {
}
