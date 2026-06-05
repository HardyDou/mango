package io.mango.job.core.service.impl;

import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceContext;
import io.mango.infra.persistence.starter.datasource.PersistenceModuleDataSourceResolver;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Job 模块数据源路由。
 */
@Component
public class MangoJobDataSourceRouter {

    static final String MODULE_NAME = "mango-job";

    private final PersistenceModuleDataSourceResolver resolver;

    public MangoJobDataSourceRouter(PersistenceModuleDataSourceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 按模块映射执行 Job 持久化逻辑。
     */
    public <T> T route(Supplier<T> supplier) {
        String datasource = resolver.resolveDataSource(MODULE_NAME)
                .orElseThrow(() -> new IllegalStateException("Missing Mango datasource for module: " + MODULE_NAME));
        try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use(datasource)) {
            return supplier.get();
        }
    }
}
