package io.mango.resource.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    private String moduleCode;

    @NotBlank(message = "Resource 模块 Hash 不能为空")
    @Pattern(regexp = "[0-9a-f]{64}", message = "Resource 模块 Hash 必须为64位小写十六进制")
    private String moduleHash;

    @NotNull(message = "Resource 模块依赖不能为空")
    private List<String> dependencies = new ArrayList<>();

    @NotBlank(message = "Resource 模块声明不能为空")
    private String declarations;

    private int declarationCount;

    public List<String> getDependencies() {
        return List.copyOf(dependencies);
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies == null ? new ArrayList<>() : new ArrayList<>(dependencies);
    }
}
