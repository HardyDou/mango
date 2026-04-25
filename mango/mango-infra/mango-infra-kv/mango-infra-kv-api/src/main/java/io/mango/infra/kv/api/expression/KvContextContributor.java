package io.mango.infra.kv.api.expression;

/**
 * Contributes variables into KV context.
 */
public interface KvContextContributor {

    /**
     * Contribute variables for KV annotation expressions.
     *
     * @param context mutable KV context
     */
    void contribute(KvContext context);
}
