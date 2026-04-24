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
        if (subscriberId == null || subscriberId.isBlank()) {
            return;
        }
        subscribers.add(subscriberId);
        if (tenantId != null && !tenantId.isBlank()) {
            tenantSubscribers.computeIfAbsent(tenantId, key -> ConcurrentHashMap.newKeySet()).add(subscriberId);
        }
    }

    @Override
    public void append(String subscriberId, RealtimeOutboundMessage envelope) {
        if (subscriberId == null || subscriberId.isBlank() || envelope == null) {
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
        append(userSubscriberId(userId), envelope);
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

    public static String userSubscriberId(Long userId) {
        return "user:" + userId;
    }

    private void publishToSubscribers(Collection<String> subscriberIds, RealtimeOutboundMessage envelope) {
        if (envelope == null) {
            return;
        }
        subscriberIds.forEach(subscriberId -> append(subscriberId, envelope));
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
}
