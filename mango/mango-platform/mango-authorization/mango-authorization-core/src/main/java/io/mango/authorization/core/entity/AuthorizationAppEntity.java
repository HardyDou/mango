package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 授权应用实体。
 * <p>
 * 仅承载授权域中的应用编码、登录上下文和管理元数据；前端入口运行配置保存在 authorization_frontend_app_registry。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_app")
public class AuthorizationAppEntity extends AuthorizationBaseEntity {
    /** 应用 ID 兼容访问器，底层统一使用 Mango 主键。 */
    public Long getAppId() {
        return getId();
    }

    public void setAppId(Long appId) {
        setId(appId);
    }

    /** 应用编码。 */
    private String appCode;

    /** 应用名称。 */
    private String appName;

    /** 前端入口类型：LOCAL/MICRO_APP/IFRAME/EXTERNAL_LINK，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String appType;

    /** 前端入口部署模式：EMBEDDED/REMOTE/HYBRID，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String deployMode;

    /** 前端远程入口地址，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String entryUrl;

    /** 前端主框架挂载路径，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String mountPath;

    /** 前端入口激活规则，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String activeRule;

    /** 前端运行框架，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String framework;

    /** 前端入口当前版本，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String version;

    /** 前端入口健康检查地址，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private String healthCheckUrl;

    /** 前端入口是否启用沙箱，来自 authorization_frontend_app_registry。 */
    @TableField(exist = false)
    private Boolean sandboxEnabled;

    /** 前端入口样式隔离模式，来自 authorization_frontend_app_registry。 */
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

    /** 更新时间。 */
}
