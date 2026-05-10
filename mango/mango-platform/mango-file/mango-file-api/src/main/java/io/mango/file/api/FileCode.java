package io.mango.file.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件模块业务码。
 */
@Getter
@AllArgsConstructor
public enum FileCode implements BizCode {

    /** 文件不存在。 */
    FILE_NOT_FOUND(3404, "文件不存在"),

    /** 文件为空。 */
    FILE_EMPTY(3405, "文件不能为空"),

    /** 文件访问被拒绝。 */
    FILE_ACCESS_DENIED(3406, "无权访问该文件"),

    /** 文件状态非法。 */
    FILE_STATUS_INVALID(3407, "文件状态非法"),

    /** 文件存储失败。 */
    FILE_STORE_FAILED(3501, "文件存储失败"),

    /** 文件读取失败。 */
    FILE_READ_FAILED(3502, "文件读取失败"),

    /** 文件存储配置不存在。 */
    STORAGE_CONFIG_NOT_FOUND(3510, "文件存储配置不存在"),

    /** 文件存储配置不可用。 */
    STORAGE_CONFIG_DISABLED(3511, "文件存储配置不可用"),

    /** 文件存储类型不支持。 */
    STORAGE_TYPE_UNSUPPORTED(3512, "文件存储类型不支持"),

    /** 文件存储配置校验失败。 */
    STORAGE_CONFIG_INVALID(3513, "文件存储配置校验失败"),

    /** 文件存储连接测试失败。 */
    STORAGE_CONFIG_TEST_FAILED(3514, "文件存储连接测试失败");

    private final int code;
    private final String message;
}
