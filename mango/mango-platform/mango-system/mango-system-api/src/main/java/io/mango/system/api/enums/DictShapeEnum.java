package io.mango.system.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/** 系统字典的数据形态。 */
@Getter
@AllArgsConstructor
public enum DictShapeEnum {
    /** 平铺键值选项，兼容现有字典。 */
    FLAT("FLAT"),
    /** 具有父子关系的层级选项。 */
    TREE("TREE");

    private final String code;

    public static DictShapeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return FLAT;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (DictShapeEnum shape : values()) {
            if (shape.code.equals(normalized)) {
                return shape;
            }
        }
        return null;
    }
}
