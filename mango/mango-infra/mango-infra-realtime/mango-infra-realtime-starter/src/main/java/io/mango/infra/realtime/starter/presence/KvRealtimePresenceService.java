package io.mango.infra.realtime.starter.presence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.realtime.core.presence.IRealtimePresenceService;
import io.mango.infra.realtime.core.presence.RealtimePresence;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * KV-backed presence route store for multi-instance realtime nodes.
 */
@Slf4j
public class KvRealtimePresenceService implements IRealtimePresenceService, AutoCloseable {

    private static final String DEFAULT_PREFIX = "mango:infra:realtime:presence";
    private static final long DEFAULT_TTL_SECONDS = 120L;

    private final IKvStore kvStore;
    private final IKvSortedSet sortedSet;
    private final ObjectMapper objectMapper;
    private final String prefix;
    private final long ttlSeconds;
    private final ConcurrentHashMap<String, RealtimePresence> localPresences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> localGroups = new ConcurrentHashMap<>();
    private final ScheduledExecutorService refresher;

    public KvRealtimePresenceService(IKvStore kvStore,
                                     IKvSortedSet sortedSet,
                                     ObjectMapper objectMapper,
                                     String prefix,
                                     long ttlSeconds) {
        this.kvStore = kvStore;
        this.sortedSet = sortedSet;
        this.objectMapper = objectMapper;
        this.prefix = normalizePrefix(prefix);
        this.ttlSeconds = ttlSeconds <= 0 ? DEFAULT_TTL_SECONDS : ttlSeconds;
        this.refresher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "realtime-kv-presence-refresh");
            thread.setDaemon(true);
            return thread;
        });
        long refreshInterval = Math.max(5, Math.min(60, this.ttlSeconds / 3));
        this.refresher.scheduleAtFixedRate(this::refreshLocalPresences, refreshInterval, refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void online(RealtimePresence presence) {
        if (presence == null || presence.sessionId() == null || presence.sessionId().isBlank()) {
            return;
        }
        localPresences.put(presence.sessionId(), presence);
        writePresence(presence);
    }

    @Override
    public void offline(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        RealtimePresence presence = localPresences.remove(sessionId);
        if (presence == null) {
            presence = read(sessionKey(sessionId));
        }
        Map<String, String> groups = localGroups.remove(sessionId);
        deletePresence(sessionId, presence);
        deleteGroups(sessionId, groups);
    }

    @Override
    public Collection<RealtimePresence> findByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return findByIndex(userIndexKey(userId));
    }

    @Override
    public Collection<RealtimePresence> findByTenant(String tenantId) {
        return findByIndex(tenantIndexKey(normalizeTenantId(tenantId)));
    }

    @Override
    public Collection<RealtimePresence> findByClient(String tenantId, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return List.of();
        }
        return findByIndex(clientIndexKey(tenantId, clientId));
    }

    @Override
    public Collection<RealtimePresence> findByConnection(String connectionId) {
        RealtimePresence presence = connectionId == null ? null : read(sessionKey(connectionId));
        return presence == null ? List.of() : List.of(presence);
    }

    @Override
    public Collection<RealtimePresence> findByGroup(String tenantId, String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return List.of();
        }
        return findByIndex(groupIndexKey(tenantId, groupId));
    }

    @Override
    public Collection<RealtimePresence> findAll() {
        return findByIndex(allIndexKey());
    }

    @Override
    public void joinGroup(String sessionId, String tenantId, String groupId) {
        if (sessionId == null || sessionId.isBlank() || groupId == null || groupId.isBlank()) {
            return;
        }
        String resolvedTenantId = normalizeTenantId(tenantId);
        String resolvedGroupId = groupId.trim();
        localGroups.computeIfAbsent(sessionId, key -> new ConcurrentHashMap<>()).put(resolvedGroupId, resolvedTenantId);
        writeGroup(sessionId, resolvedTenantId, resolvedGroupId);
    }

    @Override
    public void leaveGroup(String sessionId, String tenantId, String groupId) {
        if (sessionId == null || groupId == null || groupId.isBlank()) {
            return;
        }
        String resolvedTenantId = normalizeTenantId(tenantId);
        String resolvedGroupId = groupId.trim();
        Map<String, String> groups = localGroups.get(sessionId);
        if (groups != null) {
            groups.remove(resolvedGroupId);
            if (groups.isEmpty()) {
                localGroups.remove(sessionId, groups);
            }
        }
        sortedSet.remove(groupIndexKey(resolvedTenantId, resolvedGroupId), sessionId);
    }

    @Override
    public void close() {
        refresher.shutdownNow();
    }

    private void refreshLocalPresences() {
        for (RealtimePresence presence : localPresences.values()) {
            writePresence(presence);
        }
        for (Map.Entry<String, Map<String, String>> entry : localGroups.entrySet()) {
            String sessionId = entry.getKey();
            for (Map.Entry<String, String> group : entry.getValue().entrySet()) {
                writeGroup(sessionId, group.getValue(), group.getKey());
            }
        }
    }

    private Collection<RealtimePresence> findByIndex(String indexKey) {
        long nowMillis = System.currentTimeMillis();
        sortedSet.removeByScore(indexKey, Double.NEGATIVE_INFINITY, nowMillis);
        Map<String, RealtimePresence> presences = new LinkedHashMap<>();
        for (String sessionId : sortedSet.rangeByScore(indexKey, nowMillis, Double.POSITIVE_INFINITY, 0)) {
            RealtimePresence presence = read(sessionKey(sessionId));
            if (presence != null) {
                presences.put(presence.sessionId(), presence);
            }
        }
        return presences.values();
    }

    private void writePresence(RealtimePresence presence) {
        try {
            String value = objectMapper.writeValueAsString(presence);
            long expireAtMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds);
            kvStore.set(sessionKey(presence.sessionId()), value, ttlSeconds);
            sortedSet.add(allIndexKey(), presence.sessionId(), expireAtMillis, ttlSeconds);
            sortedSet.add(tenantIndexKey(presence.tenantId()), presence.sessionId(), expireAtMillis, ttlSeconds);
            if (presence.userId() != null) {
                sortedSet.add(userIndexKey(presence.userId()), presence.sessionId(), expireAtMillis, ttlSeconds);
            }
            if (presence.clientId() != null && !presence.clientId().isBlank()) {
                sortedSet.add(clientIndexKey(presence.tenantId(), presence.clientId()), presence.sessionId(), expireAtMillis, ttlSeconds);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize realtime presence", e);
        }
    }

    private RealtimePresence read(String key) {
        String value = kvStore.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, RealtimePresence.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize realtime presence from key {}", key, e);
            return null;
        }
    }

    private void deletePresence(String sessionId, RealtimePresence presence) {
        kvStore.delete(sessionKey(sessionId));
        sortedSet.remove(allIndexKey(), sessionId);
        if (presence == null) {
            return;
        }
        sortedSet.remove(tenantIndexKey(presence.tenantId()), sessionId);
        if (presence.userId() != null) {
            sortedSet.remove(userIndexKey(presence.userId()), sessionId);
        }
        if (presence.clientId() != null && !presence.clientId().isBlank()) {
            sortedSet.remove(clientIndexKey(presence.tenantId(), presence.clientId()), sessionId);
        }
    }

    private void writeGroup(String sessionId, String tenantId, String groupId) {
        long expireAtMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds);
        sortedSet.add(groupIndexKey(tenantId, groupId), sessionId, expireAtMillis, ttlSeconds);
    }

    private void deleteGroups(String sessionId, Map<String, String> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        groups.forEach((groupId, tenantId) -> sortedSet.remove(groupIndexKey(tenantId, groupId), sessionId));
    }

    private String sessionKey(String sessionId) {
        return prefix + ":session:" + sessionId;
    }

    private String allIndexKey() {
        return prefix + ":index:all";
    }

    private String userIndexKey(Long userId) {
        return prefix + ":index:user:" + userId;
    }

    private String tenantIndexKey(String tenantId) {
        return prefix + ":index:tenant:" + normalizeTenantId(tenantId);
    }

    private String clientIndexKey(String tenantId, String clientId) {
        return prefix + ":index:client:" + normalizeTenantId(tenantId) + ":" + clientId.trim();
    }

    private String groupIndexKey(String tenantId, String groupId) {
        return prefix + ":index:group:" + normalizeTenantId(tenantId) + ":" + groupId.trim();
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim();
    }

    private String normalizePrefix(String value) {
        String normalized = value == null || value.isBlank() ? DEFAULT_PREFIX : value.trim();
        while (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
