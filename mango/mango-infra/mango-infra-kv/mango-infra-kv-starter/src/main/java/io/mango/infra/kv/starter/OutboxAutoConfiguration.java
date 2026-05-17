package io.mango.infra.kv.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.core.outbox.KvOutboxPublisher;
import io.mango.infra.kv.core.outbox.KvOutboxStore;
import io.mango.infra.kv.core.support.KvKeyNormalizer;
import io.mango.infra.kv.core.support.PrefixedKvStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Outbox auto configuration.
 */
@AutoConfiguration(after = KvStoreAutoConfiguration.class)
@EnableConfigurationProperties(KvStoreProperties.class)
@ConditionalOnClass(IOutboxStore.class)
@ConditionalOnExpression("${mango.kv.capability.enabled:false} && ${mango.kv.capability.outbox:false}")
public class OutboxAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxAutoConfiguration.class);

    @Bean
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(IOutboxStore.class)
    public IOutboxStore outboxStore(IKvStore kvStore, KvStoreProperties props, ObjectMapper objectMapper) {
        IKvStore prefixedStore = new PrefixedKvStore(kvStore, keyNormalizer(props), KvKeyNormalizer.OUTBOX);
        IOutboxStore bean = new KvOutboxStore(prefixedStore, objectMapper);
        LOGGER.info("KV capability initialized: IOutboxStore ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnBean(IOutboxStore.class)
    @ConditionalOnMissingBean(IOutboxPublisher.class)
    public IOutboxPublisher outboxPublisher(IOutboxStore outboxStore) {
        IOutboxPublisher bean = new KvOutboxPublisher(outboxStore);
        LOGGER.info("KV capability initialized: IOutboxPublisher ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper outboxObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private KvKeyNormalizer keyNormalizer(KvStoreProperties props) {
        KvStoreProperties.Key key = props.getKey();
        return new KvKeyNormalizer(key.isEnabled(), key.getPrefix(), key.getEnv(), key.isAppEnabled(), key.getApp());
    }
}
