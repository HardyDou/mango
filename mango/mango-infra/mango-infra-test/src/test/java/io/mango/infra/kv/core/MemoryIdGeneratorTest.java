package io.mango.infra.kv.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryIdGenerator.
 */
class MemoryIdGeneratorTest {

    // ==================== nextId() tests ====================

    @Test
    void nextId_defaultStart_returnsIncrementingIds() {
        MemoryIdGenerator idGenerator = new MemoryIdGenerator();
        long firstId = idGenerator.nextId();
        long secondId = idGenerator.nextId();
        long thirdId = idGenerator.nextId();

        assertEquals(1, firstId);
        assertEquals(2, secondId);
        assertEquals(3, thirdId);
    }

    @Test
    void nextId_withInitialValue_startsFromInitial() {
        MemoryIdGenerator idGenerator = new MemoryIdGenerator(100);
        assertEquals(101, idGenerator.nextId());
        assertEquals(102, idGenerator.nextId());
    }

    @Test
    void nextId_manyIds_incrementingCorrectly() {
        MemoryIdGenerator idGenerator = new MemoryIdGenerator();
        long lastId = 0;
        for (int i = 0; i < 1000; i++) {
            long newId = idGenerator.nextId();
            assertEquals(lastId + 1, newId);
            lastId = newId;
        }
    }

    @Test
    void nextId_uniqueIds_noCollisions() {
        MemoryIdGenerator idGenerator = new MemoryIdGenerator();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(idGenerator.nextId());
        }
        assertEquals(1000, ids.size());
    }

    @Test
    void nextId_fromZero_returnsPositiveIds() {
        MemoryIdGenerator idGenerator = new MemoryIdGenerator(0);
        for (int i = 0; i < 100; i++) {
            assertTrue(idGenerator.nextId() > 0);
        }
    }
}