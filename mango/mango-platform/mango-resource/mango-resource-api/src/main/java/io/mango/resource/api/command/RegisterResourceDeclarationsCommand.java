package io.mango.resource.api.command;

import io.mango.resource.api.model.ResourceDeclaration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    @NotEmpty(message = "资源声明不能为空")
    @Valid
    @Schema(description = "资源声明列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ResourceDeclaration> declarations = new ArrayList<>();
}
