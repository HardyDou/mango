package io.mango.infra.security.api;

/**
 * Provides the current security context without exposing platform business models.
 */
public interface ISecurityContextProvider {

    /**
     * Return the current security context.
     *
     * @return security context, or {@link SecurityContext#anonymous()} when unauthenticated
     */
    SecurityContext currentContext();
}
