package io.mango.i18n.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.common.po.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Internationalization entity
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_i18n")
public class SysI18n extends BasePO {

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
