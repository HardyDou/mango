package io.mango.infra.persistence.api;

/**
 * 持久化仓储基础契约。
 * <p>
 * 面向领域仓储定义最小通用操作，不绑定具体映射框架或数据库实现。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface PersistenceRepository<T, ID> {

    /**
     * 根据主键查询实体。
     */
    T findById(ID id);

    /**
     * 保存或更新实体。
     */
    T save(T entity);

    /**
     * 根据主键删除实体。
     */
    void deleteById(ID id);
}
