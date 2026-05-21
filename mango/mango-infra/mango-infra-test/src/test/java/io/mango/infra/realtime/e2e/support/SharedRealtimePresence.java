package io.mango.infra.realtime.e2e.support;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.memory.MemoryKvStore;

public final class SharedRealtimePresence {

    private static IKvStore kvStore = new MemoryKvStore();

    private SharedRealtimePresence() {
    }

    public static synchronized void reset() {
        kvStore = new MemoryKvStore();
    }

    public static synchronized IKvStore get() {
        return kvStore;
    }
}
