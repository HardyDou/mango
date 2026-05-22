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
    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Queue<PollingWaiter>> waiters = new ConcurrentHashMap<>();
    private final int defaultMaxSize;

    public InMemoryRealtimePollingService() {
        this(FALLBACK_DEFAULT_MAX_SIZE);
    }

    public InMemoryRealtimePollingService(int defaultMaxSize) {
        this.defaultMaxSize = defaultMaxSize <= 0 ? FALLBACK_DEFAULT_MAX_SIZE : defaultMaxSize;
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
        subscribers.add(subscriberId);
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        tenantSubscribers.computeIfAbsent(resolvedTenantId, key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
        if (userId != null) {
            userSubscribers.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
            drainAliasQueue(userSubscriberId(userId), subscriberId);
        }
        if (clientId != null && !clientId.isBlank()) {
            clientSubscribers.computeIfAbsent(clientKey(resolvedTenantId, clientId), key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
            drainAliasQueue(clientSubscriberId(clientId), subscriberId);
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
        int limit = maxSize <= 0 ? defaultMaxSize : maxSize;
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
        register(subscriberId, tenantId);
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

    public static String userSubscriberId(Long userId) {
        return "user:" + userId;
    }

    public static String clientSubscriberId(String clientId) {
        return "client:" + clientId;
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
        return clientSubscriberId(envelope.source().clientId()).equals(subscriberId);
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

    private String clientKey(String tenantId, String clientId) {
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        return resolvedTenantId + ":" + clientId.trim();
    }

    private String groupKey(String tenantId, String groupId) {
        String resolvedTenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        return resolvedTenantId + ":" + groupId.trim();
    }
}
