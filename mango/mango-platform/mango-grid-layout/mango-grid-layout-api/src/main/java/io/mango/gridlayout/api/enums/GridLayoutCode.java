package io.mango.gridlayout.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义栅格布局业务码。
 */
@Getter
@AllArgsConstructor
public enum GridLayoutCode implements BizCode {

    /** 栅格布局请求或运行上下文不正确。 */
    GRID_LAYOUT_INVALID(400, "栅格布局校验失败");

    private final int code;
    private final String message;
}
