package io.mango.infra.kv.api.expression;

/**
 * Mutable KV context variable view.
 */
public interface KvContext {

    /**
     * Add or replace a variable for KV annotation SpEL expressions.
     *
     * @param name variable name
     * @param value variable value
     */
    void setVariable(String name, Object value);
}
