package io.mango.authorization.api.command;

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
public class AppCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long appId;

    @NotBlank(message = "应用编码不能为空")
    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 100, message = "应用名称最多100个字符")
    private String appName;

    @NotBlank(message = "登录域不能为空")
    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Size(max = 32, message = "操作者类型最多32个字符")
    private String actorType;

    @Size(max = 64, message = "图标最多64个字符")
    private String icon;

    @Min(value = 0, message = "排序号最小值为0")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    private Integer status;

    @Size(max = 500, message = "备注最多500个字符")
    private String remark;
}
