package io.mango.ai.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.ai.core.entity.AiPromptEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiPromptMapper extends BaseMapper<AiPromptEntity> {
}
