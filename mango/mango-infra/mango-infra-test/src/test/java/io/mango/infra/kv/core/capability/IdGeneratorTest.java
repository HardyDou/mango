package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.IIdGenerator;
import io.mango.infra.kv.core.KvStoreTestFixtures.StoreFixture;
import io.mango.infra.kv.core.support.KvStoreIdGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void nextId_usesStoreIncrement(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            IIdGenerator idGenerator = new KvStoreIdGenerator(fixture.namespacedStore());

            assertThat(idGenerator.nextId()).isEqualTo(1);
            assertThat(idGenerator.nextId()).isEqualTo(2);
            assertThat(idGenerator.nextId()).isEqualTo(3);
        }
    }
}
