package io.mango.authorization.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Menu type enumeration
 *
 * @author Mango
 */
@Getter
@AllArgsConstructor
public enum MenuTypeEnum {

    DIRECTORY(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private final int code;
    private final String description;

    /**
     * Get enum by code
     */
    public static MenuTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MenuTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
