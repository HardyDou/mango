package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 套餐-菜单关联。
 */
@Data
@TableName("authorization_menu_package_item")
public class MenuPackageItem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long packageId;
    private Long menuId;
    private Integer sort;
}
