package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 授权应用入口创建或修改命令。
 */
@Data
@Schema(description = "授权应用入口创建或修改命令")
public class AppCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "应用ID，创建时为空，修改时必填")
    private Long appId;

    @Schema(description = "应用编码")
    @NotBlank(message = "应用编码不能为空")
    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;

    @Schema(description = "应用名称")
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 100, message = "应用名称最多100个字符")
    private String appName;

    @Schema(description = "登录域")
    @NotBlank(message = "登录域不能为空")
    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Schema(description = "操作者类型")
    @Size(max = 32, message = "操作者类型最多32个字符")
    private String actorType;

    @Schema(description = "应用图标")
    @Size(max = 64, message = "图标最多64个字符")
    private String icon;

    @Schema(description = "排序号")
    @Min(value = 0, message = "排序号最小值为0")
    private Integer sort;

    @Schema(description = "状态：0-禁用，1-启用")
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注最多500个字符")
    private String remark;
}
