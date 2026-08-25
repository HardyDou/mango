package io.mango.resource.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 一个 Resource 模块的完整期望状态。
 */
@Data
@Schema(description = "Resource 模块完整期望状态")
public class ResourceModuleManifestCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Resource 模块编码不能为空")
    @Schema(description = "Resource 模块编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String moduleCode;

    @NotBlank(message = "Resource 模块 Hash 不能为空")
    @Pattern(regexp = "[0-9a-f]{64}", message = "Resource 模块 Hash 必须为64位小写十六进制")
    @Schema(description = "Resource 模块内容 Hash", requiredMode = Schema.RequiredMode.REQUIRED)
    private String moduleHash;

    @NotNull(message = "Resource 模块依赖不能为空")
    @Schema(description = "当前模块依赖的 Resource 模块编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> dependencies = new ArrayList<>();

    @NotBlank(message = "Resource 模块声明不能为空")
    @Schema(description = "当前模块的资源声明 JSON 数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private String declarations;

    @PositiveOrZero(message = "Resource 模块声明数量不能小于0")
    @Schema(description = "当前模块的资源声明数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private int declarationCount;

    public List<String> getDependencies() {
        return List.copyOf(dependencies);
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies == null ? new ArrayList<>() : new ArrayList<>(dependencies);
    }
}
