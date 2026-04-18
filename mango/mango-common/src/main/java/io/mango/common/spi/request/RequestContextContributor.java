package io.mango.common.spi.request;

/**
 * Contributes request-scoped attributes into a shared request context.
 *
 * This protocol stays in mango-common so web, security, trace, or RPC
 * infrastructure can expose request-scoped data without forcing those
 * technical dependencies into shared contracts.
 */
public interface RequestContextContributor {

    /**
     * Contribute request-scoped attributes.
     *
     * @param requestContext mutable request-context attribute view
     */
    void contribute(RequestContext requestContext);
}
