package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 授权应用登录上下文实体。
 */
@Data
@TableName("authorization_app_login_context")
public class AuthorizationAppLoginContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 上下文 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long contextId;

    /** 应用 ID。 */
    private Long appId;

    /** 应用编码。 */
    private String appCode;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 是否默认上下文：0-否，1-是。 */
    private Integer defaultFlag;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 排序号。 */
    private Integer sort;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
