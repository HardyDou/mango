package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 逻辑应用集成能力模块关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_app_module")
public class AuthorizationAppModuleEntity extends AuthorizationBaseEntity {
    /** 绑定 ID 兼容访问器，底层统一使用 Mango 主键。 */
    public Long getBindingId() {
        return getId();
    }

    public void setBindingId(Long bindingId) {
        setId(bindingId);
    }

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

    /** 更新时间。 */
}
