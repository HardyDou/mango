package io.mango.infra.kv.api;

import java.util.Collection;

/**
 * Sorted-set abstraction in the Mango KV capability family.
 * Members are unique in one key and ordered by score.
 */
public interface IKvSortedSet {

    /**
     * Add or update one member score and refresh the sorted-set key TTL.
     * Implementations must make score update and TTL refresh atomic for one key where the backend supports it.
     * @param key sorted-set key, must not be blank
     * @param member unique member, must not be blank
     * @param score ordering score
     * @param ttlSeconds sorted-set key TTL in seconds; non-positive TTL removes the member
     */
    void add(String key, String member, double score, long ttlSeconds);

    /**
     * Remove one member from a sorted set.
     * @param key sorted-set key, must not be blank
     * @param member member to remove, must not be blank
     */
    void remove(String key, String member);

    /**
     * Return members whose score is inside the inclusive range, ordered by score ascending.
     * @param key sorted-set key, must not be blank
     * @param minScore inclusive lower score bound
     * @param maxScore inclusive upper score bound
     * @param limit max returned members; non-positive means no explicit limit
     * @return ordered members, or an empty collection if none exists
     */
    Collection<String> rangeByScore(String key, double minScore, double maxScore, int limit);

    /**
     * Remove members whose score is inside the inclusive range.
     * @param key sorted-set key, must not be blank
     * @param minScore inclusive lower score bound
     * @param maxScore inclusive upper score bound
     * @return removed member count
     */
    long removeByScore(String key, double minScore, double maxScore);

    /**
     * Return the current sorted-set size.
     * @param key sorted-set key, must not be blank
     * @return live member count
     */
    long size(String key);
}
