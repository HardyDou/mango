package io.mango.authorization.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型枚举。
 */
@Getter
@AllArgsConstructor
public enum MenuTypeEnum {

    CATALOG(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private final int code;
    private final String description;

    /**
     * 根据编码获取枚举。
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
