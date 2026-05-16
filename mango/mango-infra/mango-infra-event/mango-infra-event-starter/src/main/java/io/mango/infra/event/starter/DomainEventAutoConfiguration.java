package io.mango.infra.event.starter;

import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.infra.event.api.IDomainEventBus;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.event.core.memory.InMemoryDomainEventBus;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 领域事件总线自动配置。
 */
@AutoConfiguration
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
    @ConditionalOnMissingBean(IDomainEventPublisher.class)
    public IDomainEventPublisher domainEventPublisher(IDomainEventBus eventBus) {
        return eventBus;
    }
}
