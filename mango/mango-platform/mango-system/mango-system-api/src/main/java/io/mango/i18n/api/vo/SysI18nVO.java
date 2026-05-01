package io.mango.i18n.api.vo;

import lombok.Data;

/**
 * i18n value object
 *
 * @author Mango
 */
@Data
public class SysI18nVO {

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
