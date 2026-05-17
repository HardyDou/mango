package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 授权应用实体。
 * <p>
 * 仅承载授权域中的应用编码、登录上下文和管理元数据；前端入口运行配置保存在 frontend_app_registry。
 */
@Data
@TableName("authorization_app")
public class AuthorizationApp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应用 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long appId;

    /** 应用编码。 */
    private String appCode;

    /** 应用名称。 */
    private String appName;

    /** 前端入口类型：LOCAL/MICRO_APP/IFRAME/EXTERNAL_LINK，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String appType;

    /** 前端入口部署模式：EMBEDDED/REMOTE/HYBRID，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String deployMode;

    /** 前端远程入口地址，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String entryUrl;

    /** 前端主框架挂载路径，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String mountPath;

    /** 前端入口激活规则，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String activeRule;

    /** 前端运行框架，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String framework;

    /** 前端入口当前版本，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String version;

    /** 前端入口健康检查地址，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String healthCheckUrl;

    /** 前端入口是否启用沙箱，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private Boolean sandboxEnabled;

    /** 前端入口样式隔离模式，来自 frontend_app_registry。 */
    @TableField(exist = false)
    private String styleIsolation;

    /** 应用图标。 */
    private String icon;

    /** 排序号。 */
    private Integer sort;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
