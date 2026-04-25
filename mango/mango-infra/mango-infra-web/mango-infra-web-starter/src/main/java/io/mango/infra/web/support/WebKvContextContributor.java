package io.mango.infra.web.support;

import io.mango.infra.kv.api.expression.KvContext;
import io.mango.infra.kv.api.expression.KvContextContributor;
import io.mango.infra.web.api.IRequestContextProvider;

import java.util.Objects;

/**
 * Adds selected web request variables into KV context.
 */
public class WebKvContextContributor implements KvContextContributor {

    private final IRequestContextProvider requestContextProvider;

    public WebKvContextContributor(IRequestContextProvider requestContextProvider) {
        this.requestContextProvider = Objects.requireNonNull(requestContextProvider, "requestContextProvider");
    }

    @Override
    public void contribute(KvContext kvContext) {
        kvContext.setVariable("req", requestContextProvider.currentContext());
    }
}
