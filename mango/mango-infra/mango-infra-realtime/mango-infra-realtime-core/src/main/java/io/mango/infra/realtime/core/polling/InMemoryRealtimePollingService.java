package io.mango.infra.realtime.core.polling;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Lightweight in-memory polling queue for local deployments.
 */
public class InMemoryRealtimePollingService implements RealtimePollingService {

    private static final int FALLBACK_DEFAULT_MAX_SIZE = 20;

    private final ConcurrentHashMap<String, Queue<RealtimeOutboundMessage>> queues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> tenantSubscribers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<String>> userSubscribers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> clientSubscribers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> groupSubscribers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PollingRegistration> registrations = new ConcurrentHashMap<>();
    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Queue<PollingWaiter>> waiters = new ConcurrentHashMap<>();
    private final int defaultMaxSize;

    public InMemoryRealtimePollingService() {
        this(FALLBACK_DEFAULT_MAX_SIZE);
    }

    public InMemoryRealtimePollingService(int defaultMaxSize) {
        this.defaultMaxSize = defaultMaxSize(defaultMaxSize);
    }

    public void register(String subscriberId, String tenantId) {
        register(subscriberId, tenantId, null, null);
    }

    public void register(String subscriberId, String tenantId, String clientId) {
        register(subscriberId, tenantId, null, clientId);
    }

    public void register(String subscriberId, String tenantId, Long userId, String clientId) {
        if (subscriberId == null || subscriberId.isBlank()) {
            return;
        }
        String resolvedTenantId = normalizeTenantId(tenantId);
        String resolvedClientId = normalizeText(clientId);
        PollingRegistration registration = new PollingRegistration(resolvedTenantId, userId, resolvedClientId);
        PollingRegistration previous = registrations.put(subscriberId, registration);
        if (previous != null && !previous.equals(registration)) {
            removeRegistrationIndexes(subscriberId, previous);
        }
        subscribers.add(subscriberId);
        tenantSubscribers.computeIfAbsent(resolvedTenantId, key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
        if (userId != null) {
            userSubscribers.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
            drainAliasQueue(userSubscriberId(userId), subscriberId);
        }
        if (resolvedClientId != null) {
            clientSubscribers.computeIfAbsent(clientKey(resolvedTenantId, resolvedClientId),
                    key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
            drainAliasQueue(clientSubscriberId(resolvedClientId), subscriberId);
        }
    }

    @Override
    public void append(String subscriberId, RealtimeOutboundMessage envelope) {
        if (subscriberId == null || subscriberId.isBlank() || envelope == null) {
            return;
        }
        if (isSourceSubscriber(subscriberId, envelope)) {
            return;
        }
        queues.computeIfAbsent(subscriberId, key -> new ConcurrentLinkedQueue<>()).offer(envelope);
        completeWaitingPoll(subscriberId);
    }

    @Override
    public List<RealtimeOutboundMessage> poll(String subscriberId, int maxSize) {
        Queue<RealtimeOutboundMessage> queue = queues.get(subscriberId);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        int limit = resolveMaxSize(maxSize);
        List<RealtimeOutboundMessage> messages = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            RealtimeOutboundMessage envelope = queue.poll();
            if (envelope == null) {
                break;
            }
            messages.add(envelope);
        }
        if (queue.isEmpty()) {
            queues.remove(subscriberId, queue);
        }
        return messages;
    }

    public DeferredResult<List<RealtimeOutboundMessage>> pollAsync(String subscriberId,
                                                                   String tenantId,
                                                                   int maxSize,
                                                                   long timeoutMillis) {
        if (!registrations.containsKey(subscriberId)) {
            register(subscriberId, tenantId);
        }
        List<RealtimeOutboundMessage> messages = poll(subscriberId, maxSize);
        DeferredResult<List<RealtimeOutboundMessage>> result = new DeferredResult<>(Math.max(timeoutMillis, 0L), List.of());
        if (!messages.isEmpty() || timeoutMillis <= 0) {
            result.setResult(messages);
            return result;
        }

        Queue<PollingWaiter> subscriberWaiters =
                waiters.computeIfAbsent(subscriberId, key -> new ConcurrentLinkedQueue<>());
        PollingWaiter waiter = new PollingWaiter(result, maxSize);
        subscriberWaiters.offer(waiter);
        result.onCompletion(() -> removeWaiter(subscriberId, result));
        result.onTimeout(() -> removeWaiter(subscriberId, result));
        return result;
    }

    public void publishToUser(Long userId, RealtimeOutboundMessage envelope) {
        if (userId == null) {
            return;
        }
        Set<String> subscriberIds = userSubscribers.get(userId);
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            append(userSubscriberId(userId), envelope);
            return;
        }
        publishToSubscribers(subscriberIds, envelope);
    }

    public void publishToClient(String tenantId, String clientId, RealtimeOutboundMessage envelope) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }
        publishToSubscribers(clientSubscribers.getOrDefault(clientKey(tenantId, clientId), Set.of()), envelope);
    }

    public void publishToConnection(String connectionId, RealtimeOutboundMessage envelope) {
        append(connectionId, envelope);
    }

    public void publishToGroup(String tenantId, String groupId, RealtimeOutboundMessage envelope) {
        if (groupId == null || groupId.isBlank()) {
            return;
        }
        publishToSubscribers(groupSubscribers.getOrDefault(groupKey(tenantId, groupId), Set.of()), envelope);
    }

    public void publishToTenant(String tenantId, RealtimeOutboundMessage envelope) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }
        publishToSubscribers(tenantSubscribers.getOrDefault(tenantId, Set.of()), envelope);
    }

    public void broadcast(RealtimeOutboundMessage envelope) {
        publishToSubscribers(subscribers, envelope);
    }

    public void subscribeGroup(String subscriberId, String tenantId, String groupId) {
        if (subscriberId == null || subscriberId.isBlank() || groupId == null || groupId.isBlank()) {
            return;
        }
        groupSubscribers.computeIfAbsent(groupKey(tenantId, groupId), key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
    }

    public void unsubscribeGroup(String subscriberId, String tenantId, String groupId) {
        if (subscriberId == null || groupId == null || groupId.isBlank()) {
            return;
        }
        String key = groupKey(tenantId, groupId);
        Set<String> subscriberIds = groupSubscribers.get(key);
        if (subscriberIds == null) {
            return;
        }
        subscriberIds.remove(subscriberId);
        if (subscriberIds.isEmpty()) {
            groupSubscribers.remove(key, subscriberIds);
        }
    }

    public boolean isSubscribedToGroup(RealtimeGroupSubscriptionKey key) {
        String subscriberId = key.subscriberId();
        String groupId = key.groupId();
        if (subscriberId == null || subscriberId.isBlank() || groupId == null || groupId.isBlank()) {
            return false;
        }
        return groupSubscribers.getOrDefault(groupKey(key.tenantId(), groupId), Set.of()).contains(subscriberId);
    }

    public static String userSubscriberId(Long userId) {
        return "user:" + userId;
    }

    public static String userSubscriberId(String tenantId, Long userId) {
        return "tenant:" + normalizeTenantId(tenantId) + ":user:" + userId;
    }

    public static String clientSubscriberId(String clientId) {
        return "client:" + clientId;
    }

    public static String clientSubscriberId(String tenantId, String clientId) {
        return "tenant:" + normalizeTenantId(tenantId) + ":client:" + normalizeText(clientId);
    }

    private void removeRegistrationIndexes(String subscriberId, PollingRegistration registration) {
        removeSubscriber(tenantSubscribers, registration.tenantId(), subscriberId);
        if (registration.userId() != null) {
            removeSubscriber(userSubscribers, registration.userId(), subscriberId);
        }
        if (registration.clientId() != null) {
            removeSubscriber(clientSubscribers,
                    clientKey(registration.tenantId(), registration.clientId()), subscriberId);
        }
        groupSubscribers.forEach((key, subscriberIds) -> removeSubscriber(groupSubscribers, key, subscriberId));
    }

    private <K> void removeSubscriber(ConcurrentHashMap<K, Set<String>> index,
                                      K key,
                                      String subscriberId) {
        Set<String> subscriberIds = index.get(key);
        if (subscriberIds == null) {
            return;
        }
        subscriberIds.remove(subscriberId);
        if (subscriberIds.isEmpty()) {
            index.remove(key, subscriberIds);
        }
    }

    private void publishToSubscribers(Collection<String> subscriberIds, RealtimeOutboundMessage envelope) {
        if (envelope == null) {
            return;
        }
        subscriberIds.forEach(subscriberId -> append(subscriberId, envelope));
    }

    private void drainAliasQueue(String aliasSubscriberId, String subscriberId) {
        if (aliasSubscriberId == null || aliasSubscriberId.equals(subscriberId)) {
            return;
        }
        Queue<RealtimeOutboundMessage> aliasQueue = queues.remove(aliasSubscriberId);
        if (aliasQueue == null || aliasQueue.isEmpty()) {
            return;
        }
        RealtimeOutboundMessage message;
        while ((message = aliasQueue.poll()) != null) {
            append(subscriberId, message);
        }
    }

    private boolean isSourceSubscriber(String subscriberId, RealtimeOutboundMessage envelope) {
        if (envelope.source() == null || envelope.source().clientId() == null || envelope.source().clientId().isBlank()) {
            return false;
        }
        String sourceClientId = envelope.source().clientId();
        return clientSubscriberId(sourceClientId).equals(subscriberId)
                || clientSubscriberId(envelope.tenantId(), sourceClientId).equals(subscriberId);
    }

    private void completeWaitingPoll(String subscriberId) {
        Queue<PollingWaiter> subscriberWaiters = waiters.get(subscriberId);
        if (subscriberWaiters == null) {
            return;
        }
        PollingWaiter waiter;
        while ((waiter = subscriberWaiters.poll()) != null) {
            List<RealtimeOutboundMessage> messages = poll(subscriberId, waiter.maxSize());
            if (messages.isEmpty()) {
                return;
            }
            waiter.result().setResult(messages);
        }
        if (subscriberWaiters.isEmpty()) {
            waiters.remove(subscriberId, subscriberWaiters);
        }
    }

    private void removeWaiter(String subscriberId, DeferredResult<List<RealtimeOutboundMessage>> waiter) {
        Queue<PollingWaiter> subscriberWaiters = waiters.get(subscriberId);
        if (subscriberWaiters == null) {
            return;
        }
        subscriberWaiters.removeIf(candidate -> candidate.result() == waiter);
        if (subscriberWaiters.isEmpty()) {
            waiters.remove(subscriberId, subscriberWaiters);
        }
    }

    private record PollingWaiter(DeferredResult<List<RealtimeOutboundMessage>> result, int maxSize) {
    }

    private record PollingRegistration(String tenantId, Long userId, String clientId) {
    }

    private String clientKey(String tenantId, String clientId) {
        return normalizeTenantId(tenantId) + ":" + clientId.trim();
    }

    private String groupKey(String tenantId, String groupId) {
        return normalizeTenantId(tenantId) + ":" + groupId.trim();
    }

    private static String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "default";
        }
        return tenantId.trim();
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int defaultMaxSize(int candidate) {
        if (candidate <= 0) {
            return FALLBACK_DEFAULT_MAX_SIZE;
        }
        return candidate;
    }

    private int resolveMaxSize(int candidate) {
        if (candidate <= 0) {
            return defaultMaxSize;
        }
        return candidate;
    }
}
