package io.mango.infra.event.starter;

import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.infra.event.api.IDomainEventBus;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.event.core.memory.InMemoryDomainEventBus;
import io.mango.infra.event.core.outbox.OutboxDomainEventDispatcher;
import io.mango.infra.event.core.outbox.OutboxDomainEventPublisher;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.starter.OutboxAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
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

    @Bean
    @ConditionalOnProperty(prefix = "mango.event.outbox", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(IOutboxDispatcher.class)
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
                outbox.getRetryDelaySeconds());
    }
}
