package io.mango.infra.persistence.api.crud;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * 基于 MyBatis-Plus ServiceImpl 的 Mango CRUD 薄封装。
 *
 * @param <M> Mapper 类型。
 * @param <E> 实体类型。
 */
public abstract class MangoCrudServiceImpl<M extends BaseMapper<E>, E>
        extends ServiceImpl<M, E>
        implements MangoCrudService<E> {

    private final QueryWrapperBuilder queryWrapperBuilder = new QueryWrapperBuilder();

    @Override
    public Object createByCommand(Object command) {
        E entity = toEntity(command);
        beforeCreate(command, entity);
        save(entity);
        afterCreate(command, entity);
        return readId(entity);
    }

    @Override
    public boolean updateByCommand(Object command) {
        E entity = toEntity(command);
        beforeUpdate(command, entity);
        boolean updated = updateById(entity);
        afterUpdate(command, entity, updated);
        return updated;
    }

    @Override
    public boolean deleteById(Object id) {
        beforeDelete(id);
        boolean deleted = removeById(asSerializable(convertId(id)));
        afterDelete(id, deleted);
        return deleted;
    }

    @Override
    public boolean batchDeleteByIds(List<?> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        beforeBatchDelete(ids);
        boolean deleted = removeBatchByIds(ids);
        afterBatchDelete(ids, deleted);
        return deleted;
    }

    @Override
    public Object detailById(Object id) {
        E entity = getById(asSerializable(convertId(id)));
        return entity == null ? null : toVO(entity);
    }

    @Override
    public List<?> listByQuery(Object query) {
        QueryWrapper<E> wrapper = buildQueryWrapper(query);
        return super.list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public PersistencePageResult<?> pageByQuery(Object query) {
        QueryWrapper<E> wrapper = buildQueryWrapper(query);
        Page<E> page = page(new Page<>(readLong(query, "page", 1L), readLong(query, "size", 10L)), wrapper);
        List<?> records = page.getRecords().stream().map(this::toVO).toList();
        return PersistencePageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    protected QueryWrapper<E> buildQueryWrapper(Object query) {
        QueryWrapper<E> wrapper = queryWrapperBuilder.build(query);
        applyDataScope(wrapper, query);
        return wrapper;
    }

    protected void applyDataScope(QueryWrapper<E> wrapper, Object query) {
        // 业务模块可覆写接入 DataScopeProvider。
    }

    protected E toEntity(Object source) {
        if (entityType().isInstance(source)) {
            return entityType().cast(source);
        }
        E entity = BeanUtils.instantiateClass(entityType());
        if (source != null) {
            BeanUtils.copyProperties(source, entity);
        }
        return entity;
    }

    protected Object toVO(E entity) {
        return entity;
    }

    protected void beforeCreate(Object command, E entity) {
    }

    protected void afterCreate(Object command, E entity) {
    }

    protected void beforeUpdate(Object command, E entity) {
    }

    protected void afterUpdate(Object command, E entity, boolean updated) {
    }

    protected void beforeDelete(Object id) {
    }

    protected void afterDelete(Object id, boolean deleted) {
    }

    protected void beforeBatchDelete(List<?> ids) {
    }

    protected void afterBatchDelete(List<?> ids, boolean deleted) {
    }

    protected abstract Class<E> entityType();

    private long readLong(Object source, String fieldName, long defaultValue) {
        Object value = readProperty(source, fieldName);
        if (value instanceof Number number) {
            return Math.max(number.longValue(), 1L);
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Math.max(Long.parseLong(string), 1L);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Object readProperty(Object source, String fieldName) {
        if (source == null) {
            return null;
        }
        if (source instanceof java.util.Map<?, ?> map) {
            return map.get(fieldName);
        }
        Field field = ReflectionUtils.findField(source.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, source);
    }

    private Object readId(E entity) {
        Field idField = findIdField(entity.getClass());
        if (idField == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(idField);
        return ReflectionUtils.getField(idField, entity);
    }

    private Object convertId(Object id) {
        if (id == null) {
            return null;
        }
        Field idField = findIdField(entityType());
        if (idField == null || idField.getType().isInstance(id)) {
            return id;
        }
        Class<?> idType = idField.getType();
        if (id instanceof String string) {
            if (Long.class.equals(idType) || long.class.equals(idType)) {
                return Long.valueOf(string);
            }
            if (Integer.class.equals(idType) || int.class.equals(idType)) {
                return Integer.valueOf(string);
            }
        }
        if (id instanceof Number number) {
            if (Long.class.equals(idType) || long.class.equals(idType)) {
                return number.longValue();
            }
            if (Integer.class.equals(idType) || int.class.equals(idType)) {
                return number.intValue();
            }
        }
        return id;
    }

    private Field findIdField(Class<?> type) {
        final Field[] result = new Field[1];
        ReflectionUtils.doWithFields(type, field -> {
            if (result[0] == null && (field.isAnnotationPresent(TableId.class) || Objects.equals("id", field.getName()))) {
                result[0] = field;
            }
        });
        return result[0];
    }

    private Serializable asSerializable(Object id) {
        if (id instanceof Serializable serializable) {
            return serializable;
        }
        throw new IllegalArgumentException("主键必须实现 Serializable");
    }
}
