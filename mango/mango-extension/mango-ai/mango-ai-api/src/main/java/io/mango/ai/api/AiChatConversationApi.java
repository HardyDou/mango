package io.mango.ai.api;

import io.mango.ai.api.vo.AiChatConversationDetailVO;
import io.mango.ai.api.vo.AiChatConversationVO;
import io.mango.common.result.R;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** AI 聊天会话查询与删除契约。 */
public interface AiChatConversationApi {

    R<List<AiChatConversationVO>> conversations(
            @NotBlank(message = "服务编码不能为空")
            @Size(max = 64, message = "服务编码长度不能超过64个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$", message = "服务编码格式不正确")
            String serviceCode);

    R<AiChatConversationDetailVO> conversation(
            @NotBlank(message = "服务编码不能为空")
            @Size(max = 64, message = "服务编码长度不能超过64个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$", message = "服务编码格式不正确")
            String serviceCode,
            @NotBlank(message = "会话标识不能为空")
            @Size(max = 128, message = "会话标识长度不能超过128个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$", message = "会话标识格式不正确")
            String sessionId);

    R<Boolean> deleteConversation(
            @NotBlank(message = "服务编码不能为空")
            @Size(max = 64, message = "服务编码长度不能超过64个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$", message = "服务编码格式不正确")
            String serviceCode,
            @NotBlank(message = "会话标识不能为空")
            @Size(max = 128, message = "会话标识长度不能超过128个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$", message = "会话标识格式不正确")
            String sessionId);
}
