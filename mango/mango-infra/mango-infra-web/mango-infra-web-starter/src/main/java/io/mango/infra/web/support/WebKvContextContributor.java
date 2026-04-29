package io.mango.infra.web.support;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.kv.api.expression.KvContext;
import io.mango.infra.kv.api.expression.KvContextContributor;
import io.mango.infra.web.api.IRequestContextProvider;

import java.util.Objects;

/**
 * 将选定的 Web 请求变量写入 KV 表达式上下文。
 */
public class WebKvContextContributor implements KvContextContributor {

    private final IRequestContextProvider requestContextProvider;

    public WebKvContextContributor(IRequestContextProvider requestContextProvider) {
        this.requestContextProvider = Objects.requireNonNull(requestContextProvider, "requestContextProvider");
    }

    @Override
    public void contribute(KvContext kvContext) {
        kvContext.setVariable("req", requestContextProvider.currentContext());
        kvContext.setVariable("mango", MangoContextHolder.get());
    }
}
