package io.mango.resource.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
    private String declarations;

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
}
