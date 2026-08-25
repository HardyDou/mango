package io.mango.ai.api;

import io.mango.ai.api.command.AiServiceChatCommand;
import io.mango.ai.api.vo.AiServiceChatStartVO;
import io.mango.ai.api.vo.AiServiceRuntimeOptionsVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** AI 服务统一会话运行契约。 */
public interface AiServiceChatApi {

    R<AiServiceRuntimeOptionsVO> options(
            @NotBlank(message = "服务编码不能为空")
            @Size(max = 64, message = "服务编码长度不能超过64个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$", message = "服务编码格式不正确")
            String serviceCode);

    R<AiServiceChatStartVO> chat(
            @NotBlank(message = "服务编码不能为空")
            @Size(max = 64, message = "服务编码长度不能超过64个字符")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$", message = "服务编码格式不正确")
            String serviceCode,
            @Valid AiServiceChatCommand command);

    R<Boolean> cancel(
            @NotBlank(message = "请求标识不能为空")
            @Pattern(
                    regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                    message = "请求标识格式不正确")
            String requestId);
}
