package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 授权应用 VO。
 * <p>
 * 基础字段来自 authorization_app；前端运行配置字段来自 authorization_frontend_app_registry。
 */
@Data
@Schema(description = "授权应用信息")
public class AppVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "应用ID")
    private Long appId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "应用名称")
    private String appName;
    @Schema(description = "前端入口类型")
    private String appType;
    @Schema(description = "前端部署形态")
    private String deployMode;
    @Schema(description = "前端入口地址")
    private String entryUrl;
    @Schema(description = "主框架挂载路径")
    private String mountPath;
    @Schema(description = "前端入口激活规则")
    private String activeRule;
    @Schema(description = "前端运行框架")
    private String framework;
    @Schema(description = "前端入口版本")
    private String version;
    @Schema(description = "健康检查地址")
    private String healthCheckUrl;
    @Schema(description = "是否启用沙箱")
    private Boolean sandboxEnabled;
    @Schema(description = "样式隔离模式")
    private String styleIsolation;
    @Schema(description = "允许的登录上下文")
    private List<AppLoginContextVO> loginContexts = new ArrayList<>();
    @Schema(description = "应用图标")
    private String icon;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
