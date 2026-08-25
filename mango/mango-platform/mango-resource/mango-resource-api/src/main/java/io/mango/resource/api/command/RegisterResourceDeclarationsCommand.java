package io.mango.resource.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.mango.resource.api.enums.ResourceApplyMode;
import jakarta.validation.constraints.Min;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 远程资源声明注册命令。
 */
@Data
@Schema(description = "远程资源声明注册命令")
public class RegisterResourceDeclarationsCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "来源应用不能为空")
    @Size(max = 128, message = "来源应用不能超过128个字符")
    @Schema(description = "来源应用", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Size(max = 128, message = "来源服务不能超过128个字符")
    @Schema(description = "来源服务")
    private String serviceCode;

    @NotNull(message = "管理模块列表不能为空")
    @Schema(description = "本次上报服务管理的模块编码列表；声明为空时用于判定缺失资源")
    private List<String> moduleCodes = new ArrayList<>();

    @NotBlank(message = "资源声明JSON不能为空")
    @Schema(description = "资源声明JSON数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private String declarations = "[]";

    @NotNull(message = "Resource 模块清单不能为空")
    @Valid
    @Schema(description = "按模块拆分的完整期望状态；Bootstrap 优先使用此字段")
    private List<ResourceModuleManifestCommand> moduleManifests = new ArrayList<>();

    @NotBlank(message = "Bootstrap 环境标识不能为空")
    @Size(max = 128, message = "Bootstrap 环境标识不能超过128个字符")
    @Schema(description = "Bootstrap 环境标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentKey;

    @NotNull(message = "Release generation 不能为空")
    @Min(value = 1, message = "Release generation 必须大于0")
    @Schema(description = "发布世代号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long generation;

    @NotBlank(message = "Manifest fingerprint 不能为空")
    @Size(min = 64, max = 64, message = "Manifest fingerprint 必须为64位")
    @Schema(description = "发布清单指纹", requiredMode = Schema.RequiredMode.REQUIRED)
    private String manifestFingerprint;

    @NotNull(message = "Fencing token 不能为空")
    @Min(value = 1, message = "Fencing token 必须大于0")
    @Schema(description = "写入围栏令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fencingToken;

    @NotNull(message = "Resource apply mode 不能为空")
    @Schema(description = "资源应用模式", requiredMode = Schema.RequiredMode.REQUIRED)
    private ResourceApplyMode applyMode;

    public List<String> getModuleCodes() {
        return List.copyOf(moduleCodes);
    }

    public void setModuleCodes(List<String> moduleCodes) {
        if (moduleCodes == null) {
            this.moduleCodes = new ArrayList<>();
            return;
        }
        this.moduleCodes = new ArrayList<>(moduleCodes);
    }

    public List<ResourceModuleManifestCommand> getModuleManifests() {
        return List.copyOf(moduleManifests);
    }

    public void setModuleManifests(List<ResourceModuleManifestCommand> moduleManifests) {
        this.moduleManifests = moduleManifests == null ? new ArrayList<>() : new ArrayList<>(moduleManifests);
    }
}
