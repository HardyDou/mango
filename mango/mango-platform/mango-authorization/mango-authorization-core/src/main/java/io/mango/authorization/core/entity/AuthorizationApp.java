package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 授权应用入口实体。
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
