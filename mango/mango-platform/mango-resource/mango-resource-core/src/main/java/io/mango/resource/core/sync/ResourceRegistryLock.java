package io.mango.resource.core.sync;

import io.mango.infra.kv.api.ILocker;
import lombok.RequiredArgsConstructor;

/**
 * 资源同步锁。
 */
@RequiredArgsConstructor
public class ResourceRegistryLock {

    public static final String LOCK_NAME = "mango-resource-sync";

    private final ILocker locker;

    public boolean tryLock(String owner, int ttlSeconds) {
        return locker.tryLock(LOCK_NAME, ttlSeconds);
    }

    public void unlock(String owner) {
        locker.unlock(LOCK_NAME);
    }
}
