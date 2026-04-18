package io.mango.common.spi.request;

/**
 * Mutable request-context attribute view used by cross-module contributors.
 */
public interface RequestContext {

    /**
     * Add or replace a request-context attribute.
     *
     * @param name attribute name
     * @param value attribute value
     */
    void setAttribute(String name, Object value);
}
