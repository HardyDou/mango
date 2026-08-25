package io.mango.ai.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.ai.core.entity.AiInvocationAuditEntity;
import org.apache.ibatis.annotations.Mapper;

/** AI 服务调用审计 Mapper。 */
@Mapper
public interface AiInvocationAuditMapper extends BaseMapper<AiInvocationAuditEntity> {
}
