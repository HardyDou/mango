package io.mango.file.api.enums;

/**
 * 文件存储类型。
 */
public enum FileStorageType {

    /** 本地磁盘。 */
    LOCAL,

    /** S3 兼容存储。 */
    S3,

    /** MinIO 对象存储。 */
    MINIO,

    /** AWS S3 对象存储。 */
    AWS_S3,

    /** 阿里云 OSS 对象存储。 */
    ALIYUN_OSS,

    /** 腾讯云 COS 对象存储。 */
    TENCENT_COS,

    /** 七牛云 Kodo 对象存储。 */
    QINIU_KODO
}
