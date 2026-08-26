package io.mango.ai.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.ai.core.entity.AiChatConversationEntity;
import org.apache.ibatis.annotations.Mapper;

/** AI 对话 Mapper。 */
@Mapper
public interface AiChatConversationMapper extends BaseMapper<AiChatConversationEntity> {
}
