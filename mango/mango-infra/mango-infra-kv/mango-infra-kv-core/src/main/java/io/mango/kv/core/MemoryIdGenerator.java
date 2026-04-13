package io.mango.kv.core;

import io.mango.kv.api.IIdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory implementation of IIdGenerator using atomic counter.
 * WARNING: This generates local IDs only, NOT globally unique across instances.
 * For distributed ID generation, use SnowflakeIdGenerator in mango-infra-idgen.
 */
public class MemoryIdGenerator implements IIdGenerator {

    private final AtomicLong currentId = new AtomicLong(0);

    public MemoryIdGenerator() {
        this(0);
    }

    public MemoryIdGenerator(long initialId) {
        this.currentId.set(initialId);
    }

    @Override
    public long nextId() {
        return currentId.incrementAndGet();
    }
}