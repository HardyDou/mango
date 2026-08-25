package io.mango.ai.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** AI 服务统一运行选项。 */
@Getter
@Setter
public class AiServiceRuntimeOptionsVO {
    private Long defaultModelId;
    private List<AiServiceModelOptionVO> models;

    public List<AiServiceModelOptionVO> getModels() {
        return models == null ? null : List.copyOf(models);
    }

    public void setModels(List<AiServiceModelOptionVO> models) {
        this.models = models == null ? null : List.copyOf(models);
    }
}
