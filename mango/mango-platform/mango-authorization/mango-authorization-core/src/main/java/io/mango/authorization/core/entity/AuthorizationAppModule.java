package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 逻辑应用集成能力模块关系。
 */
@Data
@TableName("authorization_app_module")
public class AuthorizationAppModule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 绑定 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long bindingId;

    /** 逻辑应用编码。 */
    private String appCode;

    /** 能力模块编码，来自 module.properties 的 module-name。 */
    private String moduleCode;

    /** 能力模块名称。 */
    private String moduleName;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

    /** 排序号。 */
    private Integer sort;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
