package io.mango.infra.kv.core.support;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.infra.kv.api.IIdGenerator;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

/**
 * Distributed ID generator backed by IKvStore.
 */
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The constructor intentionally retains the shared IKvStore capability dependency"))
public class KvStoreIdGenerator implements IIdGenerator {

    private static final long DEFAULT_WINDOW_SECONDS = 86400;

    private final IKvStore kvStore;

    @Override
    public long nextId() {
        return kvStore.incrementBy("global", 1, DEFAULT_WINDOW_SECONDS);
    }
}
