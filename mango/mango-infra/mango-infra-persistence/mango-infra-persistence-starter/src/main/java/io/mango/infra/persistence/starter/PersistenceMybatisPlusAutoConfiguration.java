package io.mango.infra.persistence.starter;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.mango.infra.context.api.MangoContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * MyBatis-Plus 自动配置。
 * <p>
 * 统一注册 MyBatis-Plus 基础插件，业务模块不直接配置底层插件。
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties(PersistenceProperties.class)
public class PersistenceMybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(PersistenceProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PersistenceProperties.MybatisPlus mybatisPlus = properties.getMybatisPlus();
        if (mybatisPlus.getTenant().isEnabled()) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler(mybatisPlus.getTenant())));
        }
        if (mybatisPlus.getPagination().isEnabled()) {
            interceptor.addInnerInterceptor(paginationInnerInterceptor(mybatisPlus.getPagination()));
        }
        return interceptor;
    }

    private PaginationInnerInterceptor paginationInnerInterceptor(PersistenceProperties.Pagination pagination) {
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setOverflow(pagination.isOverflow());
        paginationInterceptor.setMaxLimit(pagination.getMaxLimit());
        if (StringUtils.hasText(pagination.getDbType())) {
            paginationInterceptor.setDbType(com.baomidou.mybatisplus.annotation.DbType.getDbType(pagination.getDbType()));
        }
        return paginationInterceptor;
    }

    private TenantLineHandler tenantLineHandler(PersistenceProperties.Tenant tenant) {
        return new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                String tenantId = firstText(MangoContextHolder.tenantId(), tenant.getDefaultTenantId());
                if (!StringUtils.hasText(tenantId)) {
                    throw new IllegalStateException("Missing tenant context for tenant-isolated SQL. "
                            + "Set MangoContextHolder.tenantId or configure mango.persistence.mybatis-plus.tenant.default-tenant-id for non-web tasks.");
                }
                if (tenantId != null && tenantId.chars().allMatch(Character::isDigit)) {
                    return new LongValue(tenantId);
                }
                return new StringValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return tenant.getColumn();
            }

            @Override
            public boolean ignoreInsert(java.util.List<net.sf.jsqlparser.schema.Column> columns, String tenantIdColumn) {
                if (columns == null || !StringUtils.hasText(tenantIdColumn)) {
                    return false;
                }
                return columns.stream()
                        .map(column -> column.getColumnName())
                        .filter(StringUtils::hasText)
                        .map(item -> item.trim().toLowerCase(Locale.ROOT))
                        .anyMatch(item -> item.equals(tenantIdColumn.trim().toLowerCase(Locale.ROOT)));
            }

            @Override
            public boolean ignoreTable(String tableName) {
                if (!StringUtils.hasText(tableName)) {
                    return true;
                }
                String normalized = tableName.trim().toLowerCase(Locale.ROOT);
                return tenant.getExcludedTables().stream()
                        .filter(StringUtils::hasText)
                        .map(item -> item.trim().toLowerCase(Locale.ROOT))
                        .anyMatch(pattern -> matches(pattern, normalized));
            }
        };
    }

    private boolean matches(String pattern, String tableName) {
        if (pattern.endsWith("*")) {
            return tableName.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return tableName.equals(pattern);
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : second;
    }
}
