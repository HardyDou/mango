package io.mango.i18n.api.vo;

import io.mango.common.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * i18n value object
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysI18nVO extends BaseVO {

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
