package io.mango.ai.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** AI 模型列表查询。 */
@Getter
@Setter
@Schema(description = "AI 模型列表查询")
public class AiModelQuery {

    @NotNull(message = "供应商连接标识不能为空")
    @Positive(message = "供应商连接标识必须大于0")
    @Schema(description = "供应商连接标识")
    private Long providerConnectionId;

    @Size(max = 100, message = "模型关键词长度不能超过100个字符")
    @Schema(description = "模型名称或别名关键词")
    private String keyword;

    @Schema(description = "是否仅查询启用模型；为空时查询全部")
    private Boolean enabled;
}
