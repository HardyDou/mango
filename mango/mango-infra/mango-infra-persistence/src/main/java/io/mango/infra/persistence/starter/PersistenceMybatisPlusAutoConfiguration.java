package io.mango.infra.persistence.starter;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * MyBatis-Plus 自动配置。
 * <p>
 * 统一注册 MyBatis-Plus 基础插件，业务模块不直接配置底层插件。
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties(PersistenceProperties.class)
public class PersistenceMybatisPlusAutoConfiguration {

    /**
     * MyBatis-Plus 分页插件。
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    @ConditionalOnProperty(prefix = "mango.persistence.mybatis-plus.pagination",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public MybatisPlusInterceptor mybatisPlusInterceptor(PersistenceProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        PersistenceProperties.Pagination pagination = properties.getMybatisPlus().getPagination();
        paginationInterceptor.setOverflow(pagination.isOverflow());
        paginationInterceptor.setMaxLimit(pagination.getMaxLimit());
        if (StringUtils.hasText(pagination.getDbType())) {
            paginationInterceptor.setDbType(com.baomidou.mybatisplus.annotation.DbType.getDbType(pagination.getDbType()));
        }
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
