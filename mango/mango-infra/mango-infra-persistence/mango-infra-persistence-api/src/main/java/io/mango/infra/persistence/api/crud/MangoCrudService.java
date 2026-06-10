package io.mango.infra.persistence.api.crud;

import io.mango.infra.persistence.api.query.PersistencePageResult;

import java.util.List;

/**
 * Mango 标准 CRUD 服务契约。
 * <p>
 * Web 层只依赖本契约，默认 CRUD 入参按同名属性复制到实体或查询条件。
 */
public interface MangoCrudService {

    /**
     * 创建数据。
     */
    Object createByCommand(Object command);

    /**
     * 更新数据。
     */
    boolean updateByCommand(Object command);

    /**
     * 删除数据。
     */
    boolean deleteById(Object id);

    /**
     * 批量删除数据。
     */
    boolean batchDeleteByIds(List<?> ids);

    /**
     * 查询详情。
     */
    Object detailById(Object id);

    /**
     * 列表查询。
     */
    List<?> listByQuery(Object query);

    /**
     * 分页查询。
     */
    PersistencePageResult<?> pageByQuery(Object query);
}
