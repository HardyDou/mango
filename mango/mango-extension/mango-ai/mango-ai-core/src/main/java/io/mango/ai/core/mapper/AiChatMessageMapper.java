package io.mango.ai.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.ai.core.entity.AiChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

/** AI 对话消息 Mapper。 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessageEntity> {
}
