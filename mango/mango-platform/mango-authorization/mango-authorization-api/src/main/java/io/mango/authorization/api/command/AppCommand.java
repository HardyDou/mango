package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 授权应用入口创建或修改命令。
 */
@Data
@Schema(description = "授权应用创建或修改命令，包含前端运行配置字段")
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

    @Schema(description = "前端入口类型：LOCAL/MICRO_APP/IFRAME/EXTERNAL_LINK，属于 frontend_app_registry 运行配置")
    @Size(max = 32, message = "应用类型最多32个字符")
    private String appType;

    @Schema(description = "前端部署形态：EMBEDDED/REMOTE/HYBRID，属于 frontend_app_registry 运行配置")
    @Size(max = 32, message = "部署模式最多32个字符")
    private String deployMode;

    @Schema(description = "前端运行配置的远程入口地址")
    @Size(max = 500, message = "入口地址最多500个字符")
    private String entryUrl;

    @Schema(description = "前端运行配置的主框架挂载路径")
    @Size(max = 255, message = "挂载路径最多255个字符")
    private String mountPath;

    @Schema(description = "前端运行配置的激活规则")
    @Size(max = 255, message = "激活规则最多255个字符")
    private String activeRule;

    @Schema(description = "前端运行配置的框架标识")
    @Size(max = 64, message = "前端框架最多64个字符")
    private String framework;

    @Schema(description = "前端运行配置的当前版本")
    @Size(max = 64, message = "版本最多64个字符")
    private String version;

    @Schema(description = "前端运行配置的健康检查地址")
    @Size(max = 500, message = "健康检查地址最多500个字符")
    private String healthCheckUrl;

    @Schema(description = "前端运行配置是否启用沙箱")
    private Boolean sandboxEnabled;

    @Schema(description = "前端运行配置的样式隔离：NONE/SCOPED/SHADOW_DOM/IFRAME")
    @Size(max = 32, message = "样式隔离最多32个字符")
    private String styleIsolation;

    @Schema(description = "应用允许的登录上下文列表")
    @Valid
    @NotEmpty(message = "应用至少需要一个登录上下文")
    private List<AppLoginContextCommand> loginContexts = new ArrayList<>();

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
