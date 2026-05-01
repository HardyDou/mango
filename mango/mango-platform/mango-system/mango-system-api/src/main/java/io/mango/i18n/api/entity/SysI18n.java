package io.mango.i18n.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * Internationalization entity
 *
 * @author Mango
 */
@Data
@TableName("sys_i18n")
public class SysI18n implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * i18n key
     */
    private String name;

    /**
     * Chinese content
     */
    @TableField("zh_cn")
    private String zhCn;

    /**
     * English content
     */
    private String en;

    /**
     * Description
     */
    private String description;
}
