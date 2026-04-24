package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IKvSortedSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class KvSortedSetTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#sortedSets")
    void rangeByScore_returnsOrderedMembers(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvSortedSet sortedSet = fixture.sortedSet();
            String key = fixture.key("presence:index:user:1001");

            sortedSet.add(key, "s3", 30, 60);
            sortedSet.add(key, "s1", 10, 60);
            sortedSet.add(key, "s2", 20, 60);

            assertThat(sortedSet.rangeByScore(key, 0, 100, 0)).containsExactly("s1", "s2", "s3");
            assertThat(sortedSet.rangeByScore(key, 15, 100, 1)).containsExactly("s2");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#sortedSets")
    void add_updatesMemberScore(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvSortedSet sortedSet = fixture.sortedSet();
            String key = fixture.key("presence:index:all");

            sortedSet.add(key, "s1", 30, 60);
            sortedSet.add(key, "s2", 20, 60);
            sortedSet.add(key, "s1", 10, 60);

            assertThat(sortedSet.rangeByScore(key, 0, 100, 0)).containsExactly("s1", "s2");
            assertThat(sortedSet.size(key)).isEqualTo(2);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#sortedSets")
    void removeByScore_removesMatchingMembers(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvSortedSet sortedSet = fixture.sortedSet();
            String key = fixture.key("presence:index:tenant:t1");

            sortedSet.add(key, "s1", 10, 60);
            sortedSet.add(key, "s2", 20, 60);
            sortedSet.add(key, "s3", 30, 60);

            assertThat(sortedSet.removeByScore(key, 0, 20)).isEqualTo(2);
            assertThat(sortedSet.rangeByScore(key, 0, 100, 0)).containsExactly("s3");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#sortedSets")
    void ttl_expiresWholeSortedSet(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvSortedSet sortedSet = fixture.sortedSet();
            String key = fixture.key("presence:index:ttl");

            sortedSet.add(key, "s1", 10, 1);
            assertThat(sortedSet.size(key)).isEqualTo(1);

            Thread.sleep(1200);

            assertThat(sortedSet.size(key)).isZero();
            assertThat(sortedSet.rangeByScore(key, 0, 100, 0)).isEmpty();
        }
    }
}
