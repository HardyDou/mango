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

    /** 文件大小超过限制。 */
    FILE_SIZE_EXCEEDED(3406, "文件大小超过限制"),

    /** 文件类型禁止上传。 */
    FILE_EXTENSION_BLOCKED(3407, "该文件类型禁止上传"),

    /** 文件类型不允许上传。 */
    FILE_EXTENSION_NOT_ALLOWED(3408, "该文件类型不允许上传"),

    /** 文件访问被拒绝。 */
    FILE_ACCESS_DENIED(3409, "无权访问该文件"),

    /** 文件状态非法。 */
    FILE_STATUS_INVALID(3410, "文件状态非法"),

    /** 文件存储失败。 */
    FILE_STORE_FAILED(3501, "文件存储失败"),

    /** 文件读取失败。 */
    FILE_READ_FAILED(3502, "文件读取失败"),

    /** 同目录文件名重复。 */
    FILE_NAME_DUPLICATED(3503, "同目录下文件名已存在"),

    /** 文件目录不存在。 */
    FILE_DIRECTORY_NOT_FOUND(3504, "文件目录不存在"),

    /** 文件目录参数非法。 */
    FILE_DIRECTORY_INVALID(3505, "文件目录参数非法"),

    /** 文件目录非空。 */
    FILE_DIRECTORY_NOT_EMPTY(3506, "文件目录非空"),

    /** 文件目录名称重复。 */
    FILE_DIRECTORY_NAME_DUPLICATED(3507, "同级目录名称已存在"),

    /** 根目录不能删除。 */
    FILE_ROOT_DIRECTORY_DELETE_FORBIDDEN(3508, "根目录不能删除"),

    /** 文件存储路径非法。 */
    STORAGE_PATH_INVALID(3509, "文件存储路径非法"),

    /** 文件存储配置保存冲突。 */
    STORAGE_SETTINGS_SAVE_CONFLICT(3515, "文件中心配置保存冲突，请重试"),

    /** 文件存储配置不能为空。 */
    STORAGE_SETTINGS_INVALID(3516, "文件中心配置非法"),

    /** 文件存储配置不存在。 */
    STORAGE_CONFIG_NOT_FOUND(3510, "文件存储配置不存在"),

    /** 文件存储配置不可用。 */
    STORAGE_CONFIG_DISABLED(3511, "文件存储配置不可用"),

    /** 默认启用配置不能删除。 */
    STORAGE_CONFIG_ACTIVE_DELETE_FORBIDDEN(3518, "默认启用配置不能删除"),

    /** 文件存储类型不支持。 */
    STORAGE_TYPE_UNSUPPORTED(3512, "文件存储类型不支持"),

    /** 文件存储配置校验失败。 */
    STORAGE_CONFIG_INVALID(3513, "文件存储配置校验失败"),

    /** 文件存储连接测试失败。 */
    STORAGE_CONFIG_TEST_FAILED(3514, "文件存储连接测试失败");

    private final int code;
    private final String message;
}
