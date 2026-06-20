package io.mango.infra.persistence.api.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.po.PageQuery;
import io.mango.infra.persistence.api.context.PersistenceContext;
import io.mango.infra.persistence.api.context.PersistenceContextProvider;

/**
 * 多表读模型查询服务基类。
 * <p>
 * 该基类只收敛分页、结果包装和上下文读取，不承载具体 JOIN 语义。
 */
public abstract class MangoQueryServiceSupport {

    private final PersistenceContextProvider contextProvider;

    protected MangoQueryServiceSupport(PersistenceContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    protected <T> Page<T> page(PageQuery query) {
        if (query == null) {
            return new Page<>(1, 10);
        }
        return new Page<>(query.getPage(), query.getSize());
    }

    protected <T> PersistencePageResult<T> pageResult(IPage<T> page) {
        return PersistencePageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    protected PersistenceContext currentContext() {
        if (contextProvider == null || contextProvider.currentContext() == null) {
            return PersistenceContext.empty();
        }
        return contextProvider.currentContext();
    }

    protected String tenantId() {
        return currentContext().tenantId();
    }
}
