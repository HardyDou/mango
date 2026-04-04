package io.mango.captcha.api.constant;

/**
 * 存储策略类型
 *
 * @author Mango
 */
public enum StorageType {

    /**
     * Redis存储 - 分布式环境首选
     */
    REDIS("Redis存储"),

    /**
     * 数据库存储 - 无Redis时使用
     */
    DB("数据库存储"),

    /**
     * 内存存储 - 开发/测试环境
     */
    MEMORY("内存存储");

    private final String description;

    StorageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
