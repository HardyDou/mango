package io.mango.infra.kv.api;

/**
 * Outbox dispatch worker contract.
 */
public interface IOutboxDispatcher {

    /**
     * Dispatch once.
     *
     * @return handled message count
     */
    int dispatchOnce();
}
