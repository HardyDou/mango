package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前端应用运行配置。
 * <p>
 * 以授权应用 appCode 为关联键，独立保存前端运行单元配置，不改动 authorization_app 的授权边界语义。
 */
@Data
@TableName("frontend_app_registry")
public class FrontendAppRegistry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 前端运行配置 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long registryId;

    /** 授权应用编码。 */
    private String appCode;

    /** 前端入口类型：LOCAL/MICRO_APP/IFRAME/EXTERNAL_LINK。 */
    private String appType;

    /** 部署模式：EMBEDDED/REMOTE/HYBRID。 */
    private String deployMode;

    /** 远程入口地址。 */
    private String entryUrl;

    /** 主框架挂载路径。 */
    private String mountPath;

    /** 激活规则。 */
    private String activeRule;

    /** 前端运行框架。 */
    private String framework;

    /** 当前版本。 */
    private String version;

    /** 健康检查地址。 */
    private String healthCheckUrl;

    /** 是否启用沙箱。 */
    private Boolean sandboxEnabled;

    /** 样式隔离模式。 */
    private String styleIsolation;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
