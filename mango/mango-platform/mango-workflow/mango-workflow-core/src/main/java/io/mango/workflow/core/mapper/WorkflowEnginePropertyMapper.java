package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.workflow.core.entity.WorkflowEnginePropertyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Flowable 引擎必需元数据访问器。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface WorkflowEnginePropertyMapper extends BaseMapper<WorkflowEnginePropertyEntity> {
}
