package io.mango.infra.persistence.starter.scope;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * MyBatis-Plus 数据权限条件应用器。
 */
@RequiredArgsConstructor
public class MybatisPlusDataScopeApplier implements DataScopeApplier {

    private final DataScopeProvider dataScopeProvider;
    private final Optional<DataSource> dataSource;

    public MybatisPlusDataScopeApplier(DataScopeProvider dataScopeProvider) {
        this(dataScopeProvider, Optional.empty());
    }

    @Override
    public <T> void apply(QueryWrapper<T> wrapper, String resourceCode, DataScopeMapping mapping) {
        if (wrapper == null) {
            throw new IllegalArgumentException("QueryWrapper must not be null");
        }
        if (!StringUtils.hasText(resourceCode)) {
            throw new IllegalArgumentException("resourceCode must not be blank");
        }
        DataScopeRule rule = dataScopeProvider.resolve(resourceCode)
                .orElseGet(() -> new DataScopeRule(DataScopeRule.Mode.SELF, java.util.Set.of(), true));
        validateMapping(rule, mapping);
        applyRule(wrapper, rule, mapping);
    }

    private void validateMapping(DataScopeRule rule, DataScopeMapping mapping) {
        DataScopeRule.Mode mode = rule.mode() == null ? DataScopeRule.Mode.SELF : rule.mode();
        if (mode == DataScopeRule.Mode.SELF) {
            validateFieldExists(mapping, requiredField(mapping == null ? null : mapping.selfField(), "selfField"));
            return;
        }
        if (mode == DataScopeRule.Mode.ORG) {
            boolean hasOrgValues = rule.values() != null && !rule.values().isEmpty();
            if (!hasOrgValues && !rule.selfIncluded()) {
                return;
            }
            if (hasOrgValues) {
                validateFieldExists(mapping, requiredField(mapping == null ? null : mapping.orgField(), "orgField"));
            }
            if (rule.selfIncluded()) {
                validateFieldExists(mapping, requiredField(mapping == null ? null : mapping.selfField(), "selfField"));
            }
        }
    }

    private <T> void applyRule(QueryWrapper<T> wrapper, DataScopeRule rule, DataScopeMapping mapping) {
        DataScopeRule.Mode mode = rule.mode() == null ? DataScopeRule.Mode.SELF : rule.mode();
        switch (mode) {
            case ALL -> {
                return;
            }
            case SELF -> applySelf(wrapper, mapping);
            case ORG -> applyOrg(wrapper, rule, mapping);
            case TENANT, CUSTOM -> throw new IllegalStateException("Unsupported data scope mode: " + mode);
        }
    }

    private <T> void applySelf(QueryWrapper<T> wrapper, DataScopeMapping mapping) {
        String selfField = requiredField(mapping == null ? null : mapping.selfField(), "selfField");
        Long userId = MangoContextHolder.userId();
        if (userId == null) {
            throw new IllegalStateException("Missing user context for SELF data scope.");
        }
        wrapper.eq(selfField, userId);
    }

    private <T> void applyOrg(QueryWrapper<T> wrapper, DataScopeRule rule, DataScopeMapping mapping) {
        boolean hasOrgValues = rule.values() != null && !rule.values().isEmpty();
        boolean selfIncluded = rule.selfIncluded();
        if (!hasOrgValues && !selfIncluded) {
            wrapper.apply("1 = 0");
            return;
        }
        String orgField = requiredField(mapping == null ? null : mapping.orgField(), "orgField");
        if (hasOrgValues && selfIncluded) {
            String selfField = requiredField(mapping == null ? null : mapping.selfField(), "selfField");
            Long userId = MangoContextHolder.userId();
            if (userId == null) {
                throw new IllegalStateException("Missing user context for SELF data scope.");
            }
            wrapper.and(condition -> condition.in(orgField, rule.values()).or().eq(selfField, userId));
            return;
        }
        if (hasOrgValues) {
            wrapper.in(orgField, rule.values());
            return;
        }
        applySelf(wrapper, mapping);
    }

    private String requiredField(String field, String name) {
        if (!StringUtils.hasText(field)) {
            throw new IllegalArgumentException("Data scope " + name + " must not be blank.");
        }
        return field.trim();
    }

    private void validateFieldExists(DataScopeMapping mapping, String field) {
        if (mapping == null || !StringUtils.hasText(mapping.tableName()) || dataSource.isEmpty()) {
            return;
        }
        String tableName = normalize(mapping.tableName());
        String columnName = normalize(extractColumnName(field));
        if (!hasColumn(tableName, columnName)) {
            throw new IllegalStateException("Data scope field " + field
                    + " does not exist in table " + mapping.tableName() + ".");
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try (Connection connection = dataSource.get().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();
            if (hasColumn(metaData, catalog, schema, tableName, columnName)) {
                return true;
            }
            if (schema != null && hasColumn(metaData, catalog, null, tableName, columnName)) {
                return true;
            }
            return hasColumn(metaData, null, null, tableName, columnName);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to validate data scope field " + columnName
                    + " in table " + tableName + ".", ex);
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData,
                              String catalog,
                              String schema,
                              String tableName,
                              String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (columns.next()) {
                if (columnName.equals(normalize(columns.getString("COLUMN_NAME")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractColumnName(String field) {
        String value = field.trim();
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < value.length() - 1) {
            return value.substring(dotIndex + 1);
        }
        return value;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
