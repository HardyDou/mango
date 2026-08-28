package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.workflow.core.entity.WorkflowProcessParticipantEntity;
import org.apache.ibatis.annotations.Mapper;

/** 工作流参与关系 Mapper。 */
@Mapper
public interface WorkflowProcessParticipantMapper extends BaseMapper<WorkflowProcessParticipantEntity> {
}
