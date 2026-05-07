package io.mango.i18n.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Internationalization entity
 *
 * @author Mango
 */
@Data
@TableName("sys_i18n")
@Schema(description = "国际化条目")
public class SysI18n implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @Schema(description = "国际化条目ID")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * i18n key
     */
    @Schema(description = "国际化键名")
    private String name;

    /**
     * Chinese content
     */
    @Schema(description = "中文内容")
    @TableField("zh_cn")
    private String zhCn;

    /**
     * English content
     */
    @Schema(description = "英文内容")
    private String en;

    /**
     * Description
     */
    @Schema(description = "描述")
    private String description;
}
