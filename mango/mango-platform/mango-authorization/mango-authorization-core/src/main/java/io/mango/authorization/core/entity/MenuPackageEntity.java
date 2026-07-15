package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 菜单授权套餐主档。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_menu_package")
public class MenuPackageEntity extends AuthorizationBaseEntity {
    public Long getPackageId() {
        return getId();
    }

    public void setPackageId(Long packageId) {
        setId(packageId);
    }

    private String packageName;
    private String packageCode;
    private String appCode;
    private Integer status;
    private Integer sort;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
