package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.core.KvStoreTestFixtures;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

abstract class KvStoreCapabilityTestSupport {

    static Stream<Arguments> kvStores() {
        return KvStoreTestFixtures.kvStores();
    }
}
