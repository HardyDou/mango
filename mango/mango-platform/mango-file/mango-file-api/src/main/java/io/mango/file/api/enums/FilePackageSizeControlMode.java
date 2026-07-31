package io.mango.file.api.enums;

/**
 * 文件打包大小控制模式。
 */
public enum FilePackageSizeControlMode {

    /** 按最终 ZIP 目标自动为可压缩文件分配目标大小。 */
    AUTO,

    /** 严格使用每个打包条目声明的目标大小。 */
    MANUAL
}
