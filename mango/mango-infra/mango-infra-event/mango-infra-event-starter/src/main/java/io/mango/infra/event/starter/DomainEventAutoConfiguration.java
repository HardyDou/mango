package io.mango.infra.event.starter;

import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.infra.event.api.IDomainEventBus;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.event.core.memory.InMemoryDomainEventBus;
import io.mango.infra.event.core.outbox.OutboxDomainEventDispatcher;
import io.mango.infra.event.core.outbox.OutboxDomainEventPublisher;
import io.mango.infra.event.core.redis.RedisStreamDomainEventTransport;
import io.mango.infra.event.core.system.SystemEventService;
import io.mango.infra.event.core.transport.DomainEventTransport;
import io.mango.infra.event.core.transport.TransportDomainEventDispatcher;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.starter.OutboxAutoConfiguration;
import io.mango.infra.event.starter.controller.SystemEventController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * 领域事件总线自动配置。
 */
@AutoConfiguration(after = OutboxAutoConfiguration.class)
@EnableConfigurationProperties(DomainEventProperties.class)
@ConditionalOnClass(IDomainEventBus.class)
public class DomainEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IDomainEventBus.class)
    public IDomainEventBus domainEventBus(List<DomainEventSubscriber> subscribers) {
        InMemoryDomainEventBus eventBus = new InMemoryDomainEventBus();
        for (DomainEventSubscriber subscriber : subscribers) {
            eventBus.subscribe(subscriber.eventType(), subscriber::onEvent);
        }
        return eventBus;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.event.outbox", name = "enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(IDomainEventPublisher.class)
    public IDomainEventPublisher domainEventPublisher(IDomainEventBus eventBus) {
        return eventBus;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.event.outbox", name = "enabled", havingValue = "true")
    @Primary
    public IDomainEventPublisher outboxDomainEventPublisher(IOutboxPublisher outboxPublisher) {
        return new OutboxDomainEventPublisher(outboxPublisher);
    }

    @Bean("domainEventOutboxDispatcher")
    @ConditionalOnProperty(prefix = "mango.event.outbox", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "domainEventOutboxDispatcher")
    @ConditionalOnProperty(prefix = "mango.event", name = "transport", havingValue = "none", matchIfMissing = true)
    public IOutboxDispatcher domainEventOutboxDispatcher(
            IOutboxStore outboxStore,
            IDomainEventBus eventBus,
            DomainEventProperties properties) {
        DomainEventProperties.Outbox outbox = properties.getOutbox();
        return new OutboxDomainEventDispatcher(
                outboxStore,
                eventBus,
                Clock.systemUTC(),
                outbox.getWorkerId(),
                outbox.getBatchSize(),
                outbox.getRetryDelaySeconds(),
                outbox.getMaxAttempts());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.event", name = "transport", havingValue = "redis-stream")
    @ConditionalOnMissingBean
    public DomainEventTransport redisStreamDomainEventTransport(
            RedissonClient redissonClient,
            IDomainEventBus eventBus,
            ObjectMapper objectMapper,
            DomainEventProperties properties) {
        DomainEventProperties.RedisStream redisStream = properties.getRedisStream();
        return new RedisStreamDomainEventTransport(
                redissonClient,
                eventBus,
                objectMapper,
                redisStream.getStreamName(),
                redisStream.getGroup(),
                redisStream.getConsumer(),
                redisStream.getBatchSize(),
                Duration.ofMillis(redisStream.getReadTimeoutMillis()),
                Duration.ofMillis(redisStream.getPendingIdleTimeoutMillis()));
    }

    @Bean("domainEventOutboxDispatcher")
    @ConditionalOnExpression("${mango.event.outbox.enabled:false} && '${mango.event.transport:none}' == 'redis-stream'")
    @ConditionalOnMissingBean(name = "domainEventOutboxDispatcher")
    public IOutboxDispatcher transportDomainEventOutboxDispatcher(
            IOutboxStore outboxStore,
            DomainEventTransport transport,
            DomainEventProperties properties) {
        DomainEventProperties.Outbox outbox = properties.getOutbox();
        return new TransportDomainEventDispatcher(
                outboxStore,
                transport,
                Clock.systemUTC(),
                outbox.getWorkerId(),
                outbox.getBatchSize(),
                outbox.getRetryDelaySeconds(),
                outbox.getMaxAttempts());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.event.outbox", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public SystemEventService systemEventService(IOutboxStore outboxStore) {
        return new SystemEventService(outboxStore, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.event.outbox", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public SystemEventController systemEventController(SystemEventService systemEventService) {
        return new SystemEventController(systemEventService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.event.outbox", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "domainEventOutboxTaskScheduler")
    public TaskScheduler domainEventOutboxTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("mango-event-outbox-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnExpression("${mango.event.outbox.enabled:false} && ${mango.event.outbox.dispatch-enabled:true}")
    @ConditionalOnMissingBean(OutboxDispatchScheduler.class)
    public OutboxDispatchScheduler domainEventOutboxDispatchScheduler(
            @Qualifier("domainEventOutboxDispatcher") IOutboxDispatcher dispatcher,
            TaskScheduler domainEventOutboxTaskScheduler,
            DomainEventProperties properties) {
        DomainEventProperties.Outbox outbox = properties.getOutbox();
        return new OutboxDispatchScheduler(
                dispatcher,
                domainEventOutboxTaskScheduler,
                outbox.getDispatchIntervalMillis(),
                outbox.getDispatchInitialDelayMillis());
    }

    @Bean
    @ConditionalOnMissingBean(name = "domainEventTransportTaskScheduler")
    @ConditionalOnExpression("'${mango.event.transport:none}' == 'redis-stream'")
    public TaskScheduler domainEventTransportTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("mango-event-transport-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnExpression("'${mango.event.transport:none}' == 'redis-stream' && ${mango.event.redis-stream.consume-enabled:true}")
    @ConditionalOnMissingBean
    public DomainEventTransportScheduler domainEventTransportScheduler(
            DomainEventTransport transport,
            TaskScheduler domainEventTransportTaskScheduler,
            DomainEventProperties properties) {
        DomainEventProperties.RedisStream redisStream = properties.getRedisStream();
        return new DomainEventTransportScheduler(
                transport,
                domainEventTransportTaskScheduler,
                redisStream.getConsumeIntervalMillis(),
                redisStream.getConsumeInitialDelayMillis());
    }
}
