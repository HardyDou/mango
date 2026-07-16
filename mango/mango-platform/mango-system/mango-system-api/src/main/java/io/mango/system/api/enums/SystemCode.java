package io.mango.system.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 系统模块业务码。 */
@Getter
@AllArgsConstructor
public enum SystemCode implements BizCode {
    SYSTEM_INVALID(2400, "系统参数不合法"),
    DICT_TYPE_NOT_FOUND(2401, "字典类型不存在"),
    DICT_DATA_NOT_FOUND(2402, "字典数据不存在"),
    CONFIG_NOT_FOUND(2403, "配置不存在"),
    INSTITUTION_NOT_FOUND(2404, "机构不存在"),
    INSTITUTION_STATUS_INVALID(2408, "机构状态非法"),
    INSTITUTION_DELETE_BLOCKED(2409, "机构已初始化业务数据，请改为归档处理"),
    LOG_NOT_FOUND(2413, "日志记录不存在"),
    DICT_TYPE_IN_USE(2414, "请先删除该类型下的字典数据"),
    CONFIG_NOT_EDITABLE(2415, "配置不可编辑");

    private final int code;
    private final String message;
}
