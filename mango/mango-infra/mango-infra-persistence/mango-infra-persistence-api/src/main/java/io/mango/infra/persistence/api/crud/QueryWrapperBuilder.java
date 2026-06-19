package io.mango.infra.persistence.api.crud;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.common.po.PageQuery;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基于查询对象注解构造 MyBatis-Plus QueryWrapper。
 */
public class QueryWrapperBuilder {

    public <E> QueryWrapper<E> build(Object query) {
        QueryWrapper<E> wrapper = new QueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        if (query instanceof Map<?, ?> map) {
            appendMapConditions(wrapper, map);
            return wrapper;
        }
        ReflectionUtils.doWithFields(query.getClass(), field -> appendCondition(wrapper, query, field),
                this::shouldUseField);
        return wrapper;
    }

    private <E> void appendMapConditions(QueryWrapper<E> wrapper, Map<?, ?> query) {
        query.forEach((key, value) -> {
            if (!(key instanceof String fieldName) || isPageField(fieldName) || isEmpty(value)) {
                return;
            }
            wrapper.eq(camelToUnderline(fieldName), normalizeValue(value));
        });
    }

    private boolean shouldUseField(Field field) {
        int modifiers = field.getModifiers();
        return !field.isSynthetic()
                && !Modifier.isStatic(modifiers)
                && !field.isAnnotationPresent(QueryIgnore.class)
                && !PageQuery.class.equals(field.getDeclaringClass());
    }

    private boolean isPageField(String fieldName) {
        return "page".equals(fieldName) || "size".equals(fieldName) || "sorts".equals(fieldName);
    }

    private <E> void appendCondition(QueryWrapper<E> wrapper, Object query, Field field) {
        ReflectionUtils.makeAccessible(field);
        Object value = ReflectionUtils.getField(field, query);
        if (isEmpty(value)) {
            return;
        }
        QueryField queryField = field.getAnnotation(QueryField.class);
        QueryType type = queryField == null ? QueryType.EQ : queryField.type();
        String column = queryField == null || queryField.column().isBlank()
                ? camelToUnderline(field.getName())
                : queryField.column();
        switch (type) {
            case EQ -> wrapper.eq(column, value);
            case NE -> wrapper.ne(column, value);
            case LIKE -> wrapper.like(column, value);
            case LEFT_LIKE -> wrapper.likeLeft(column, value);
            case RIGHT_LIKE -> wrapper.likeRight(column, value);
            case IN -> appendIn(wrapper, column, value);
            case BETWEEN -> appendBetween(wrapper, column, value);
            case GE -> wrapper.ge(column, value);
            case GT -> wrapper.gt(column, value);
            case LE -> wrapper.le(column, value);
            case LT -> wrapper.lt(column, value);
            default -> wrapper.eq(column, value);
        }
    }

    private <E> void appendIn(QueryWrapper<E> wrapper, String column, Object value) {
        if (value instanceof Collection<?> collection) {
            if (!collection.isEmpty()) {
                wrapper.in(column, collection);
            }
            return;
        }
        if (value.getClass().isArray()) {
            wrapper.in(column, List.of((Object[]) value));
            return;
        }
        wrapper.eq(column, value);
    }

    private <E> void appendBetween(QueryWrapper<E> wrapper, String column, Object value) {
        if (value instanceof List<?> list && list.size() >= 2) {
            wrapper.between(column, list.get(0), list.get(1));
        }
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String string) {
            return string.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Collection<?> collection && collection.size() == 1) {
            return collection.iterator().next();
        }
        return value;
    }

    private String camelToUnderline(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append('_').append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
