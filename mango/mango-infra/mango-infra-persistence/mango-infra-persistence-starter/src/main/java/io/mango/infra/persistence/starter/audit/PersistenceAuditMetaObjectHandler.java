package io.mango.infra.persistence.starter.audit;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.mango.infra.persistence.api.context.PersistenceContext;
import io.mango.infra.persistence.api.context.PersistenceContextProvider;
import org.apache.ibatis.reflection.MetaObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * MyBatis-Plus 审计字段自动填充器。
 */
public class PersistenceAuditMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATED_BY = "createdBy";
    private static final String CREATED_AT = "createdAt";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATED_BY = "updatedBy";
    private static final String UPDATED_AT = "updatedAt";
    private static final String UPDATE_TIME = "updateTime";
    private static final String TENANT_ID = "tenantId";

    private final PersistenceContextProvider contextProvider;

    public PersistenceAuditMetaObjectHandler(PersistenceContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        PersistenceContext context = currentContext();
        Object now = nowFor(metaObject, CREATED_AT);
        setIfEmpty(metaObject, CREATED_BY, context.userId());
        setIfEmpty(metaObject, CREATED_AT, now);
        setIfEmpty(metaObject, CREATE_TIME, nowFor(metaObject, CREATE_TIME));
        setIfEmpty(metaObject, UPDATED_BY, context.userId());
        setIfEmpty(metaObject, UPDATED_AT, nowFor(metaObject, UPDATED_AT));
        setIfEmpty(metaObject, UPDATE_TIME, nowFor(metaObject, UPDATE_TIME));
        setTenantIfEmpty(metaObject, context.tenantId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        PersistenceContext context = currentContext();
        setIfPresent(metaObject, UPDATED_BY, context.userId());
        setIfPresent(metaObject, UPDATED_AT, nowFor(metaObject, UPDATED_AT));
        setIfPresent(metaObject, UPDATE_TIME, nowFor(metaObject, UPDATE_TIME));
    }

    private PersistenceContext currentContext() {
        if (contextProvider == null || contextProvider.currentContext() == null) {
            return PersistenceContext.empty();
        }
        return contextProvider.currentContext();
    }

    private void setTenantIfEmpty(MetaObject metaObject, String tenantId) {
        if (tenantId == null || tenantId.isBlank() || !metaObject.hasSetter(TENANT_ID)) {
            return;
        }
        Class<?> setterType = metaObject.getSetterType(TENANT_ID);
        if (Long.class.equals(setterType) || long.class.equals(setterType)) {
            try {
                setIfEmpty(metaObject, TENANT_ID, Long.valueOf(tenantId));
            } catch (NumberFormatException ignored) {
                return;
            }
            return;
        }
        if (String.class.equals(setterType)) {
            setIfEmpty(metaObject, TENANT_ID, tenantId);
        }
    }

    private void setIfEmpty(MetaObject metaObject, String fieldName, Object value) {
        if (value == null || !metaObject.hasSetter(fieldName)) {
            return;
        }
        Object current = metaObject.getValue(fieldName);
        if (current == null) {
            metaObject.setValue(fieldName, value);
        }
    }

    private void setIfPresent(MetaObject metaObject, String fieldName, Object value) {
        if (value == null || !metaObject.hasSetter(fieldName)) {
            return;
        }
        metaObject.setValue(fieldName, value);
    }

    private Object nowFor(MetaObject metaObject, String fieldName) {
        if (!metaObject.hasSetter(fieldName)) {
            return null;
        }
        Class<?> setterType = metaObject.getSetterType(fieldName);
        if (LocalDateTime.class.equals(setterType)) {
            return LocalDateTime.now();
        }
        if (Instant.class.equals(setterType)) {
            return Instant.now();
        }
        if (Date.class.equals(setterType)) {
            return new Date();
        }
        return null;
    }
}
