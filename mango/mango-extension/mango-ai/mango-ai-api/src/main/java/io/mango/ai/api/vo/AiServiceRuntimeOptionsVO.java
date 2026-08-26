package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** AI 服务统一运行选项。 */
@Getter
@Setter
public class AiServiceRuntimeOptionsVO {
    @Schema(description = "默认模型标识")
    private Long defaultModelId;
    @Schema(description = "当前可调用模型选项")
    private List<AiServiceModelOptionVO> models;

    public List<AiServiceModelOptionVO> getModels() {
        return models == null ? null : List.copyOf(models);
    }

    public void setModels(List<AiServiceModelOptionVO> models) {
        this.models = models == null ? null : List.copyOf(models);
    }
}
