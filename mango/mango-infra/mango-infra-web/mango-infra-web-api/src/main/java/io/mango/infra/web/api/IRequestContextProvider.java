package io.mango.infra.web.api;

/**
 * Provides the current HTTP request context as an infrastructure contract.
 */
public interface IRequestContextProvider {

    /**
     * Return the current HTTP request context.
     *
     * @return request context, or {@link RequestContextSnapshot#empty()} outside an HTTP request
     */
    RequestContextSnapshot currentContext();
}
