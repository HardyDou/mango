package io.mango.infra.kv.starter;

import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.IConverter;
import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.api.IIdGenerator;
import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.kv.api.ISerializer;
import io.mango.infra.kv.api.ITokenStore;
import io.mango.infra.kv.api.expression.KvContextContributor;
import io.mango.infra.kv.core.aspect.KvCapabilityAspect;
import io.mango.infra.kv.core.capability.KvStoreCache;
import io.mango.infra.kv.core.capability.KvStoreCounter;
import io.mango.infra.kv.core.capability.KvStoreIdempotent;
import io.mango.infra.kv.core.capability.KvStoreLocker;
import io.mango.infra.kv.core.capability.KvStoreRateLimiter;
import io.mango.infra.kv.core.capability.KvStoreTokenStore;
import io.mango.infra.kv.core.support.JsonConverter;
import io.mango.infra.kv.core.support.JsonSerializer;
import io.mango.infra.kv.core.support.KvKeyNormalizer;
import io.mango.infra.kv.core.support.KvStoreIdGenerator;
import io.mango.infra.kv.core.support.PrefixedCapabilities;
import io.mango.infra.kv.core.support.PrefixedKvStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * KV capability bean auto-configuration.
 *
 * Capability beans are opt-in. Store selection creates IKvStore; this class
 * only creates higher-level capability beans when explicitly enabled.
 */
@AutoConfiguration
@EnableConfigurationProperties(KvStoreProperties.class)
@ConditionalOnClass({ICache.class, ILocker.class})
@ConditionalOnExpression("${mango.kv.capability.enabled:false}")
public class KvCapabilityAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(KvCapabilityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(KvCapabilityAspect.class)
    public KvCapabilityAspect kvCapabilityAspect(ObjectProvider<ICache> cacheProvider,
                                                 ObjectProvider<ILocker> lockerProvider,
                                                 ObjectProvider<IRateLimiter> rateLimiterProvider,
                                                 ObjectProvider<IIdempotent> idempotentProvider,
                                                 ObjectProvider<ISerializer> serializerProvider,
                                                 BeanFactory beanFactory,
                                                 List<KvContextContributor> kvContextContributors) {
        return new KvCapabilityAspect(cacheProvider, lockerProvider, rateLimiterProvider, idempotentProvider,
                serializerProvider, beanFactory, kvContextContributors);
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.cache:false}")
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(ICache.class)
    public ICache cache(IKvStore kvStore, KvStoreProperties props) {
        ICache rawBean = new KvStoreCache(kvStore);
        ICache bean = new PrefixedCapabilities.Cache(rawBean, keyNormalizer(props));
        LOGGER.info("KV capability initialized: ICache ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.locker:false}")
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(ILocker.class)
    public ILocker locker(IKvStore kvStore, KvStoreProperties props) {
        ILocker rawBean = new KvStoreLocker(kvStore);
        ILocker bean = new PrefixedCapabilities.Locker(rawBean, keyNormalizer(props));
        LOGGER.info("KV capability initialized: ILocker ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.counter:false}")
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(ICounter.class)
    public ICounter counter(IKvStore kvStore, KvStoreProperties props) {
        ICounter rawBean = new KvStoreCounter(kvStore);
        ICounter bean = new PrefixedCapabilities.Counter(rawBean, keyNormalizer(props));
        LOGGER.info("KV capability initialized: ICounter ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.rate-limiter:false}")
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(IRateLimiter.class)
    public IRateLimiter rateLimiter(IKvStore kvStore, KvStoreProperties props) {
        IRateLimiter rawBean = new KvStoreRateLimiter(kvStore);
        IRateLimiter bean = new PrefixedCapabilities.RateLimiter(rawBean, keyNormalizer(props));
        LOGGER.info("KV capability initialized: IRateLimiter ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.idempotent:false}")
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(IIdempotent.class)
    public IIdempotent idempotent(IKvStore kvStore, KvStoreProperties props) {
        IIdempotent rawBean = new KvStoreIdempotent(kvStore);
        IIdempotent bean = new PrefixedCapabilities.Idempotent(rawBean, keyNormalizer(props));
        LOGGER.info("KV capability initialized: IIdempotent ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.token-store:false}")
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(ITokenStore.class)
    public ITokenStore tokenStore(IKvStore kvStore, KvStoreProperties props) {
        ITokenStore rawBean = new KvStoreTokenStore(kvStore);
        ITokenStore bean = new PrefixedCapabilities.TokenStore(rawBean, keyNormalizer(props));
        LOGGER.info("KV capability initialized: ITokenStore ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.id-generator:false}")
    @ConditionalOnBean(IKvStore.class)
    @ConditionalOnMissingBean(IIdGenerator.class)
    public IIdGenerator idGenerator(IKvStore kvStore, KvStoreProperties props) {
        IKvStore prefixedStore = prefixedStore(kvStore, props, KvKeyNormalizer.IDGEN);
        IIdGenerator bean = new KvStoreIdGenerator(prefixedStore);
        LOGGER.info("KV capability initialized: IIdGenerator ({})", bean.getClass().getSimpleName());
        return bean;
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.serializer:false}")
    @ConditionalOnMissingBean(ISerializer.class)
    public ISerializer jsonSerializer() {
        LOGGER.info("KV support initialized: ISerializer (JsonSerializer)");
        return new JsonSerializer();
    }

    @Bean
    @ConditionalOnExpression("${mango.kv.capability.converter:false}")
    @ConditionalOnMissingBean(IConverter.class)
    public IConverter jsonConverter() {
        LOGGER.info("KV support initialized: IConverter (JsonConverter)");
        return new JsonConverter();
    }

    private IKvStore prefixedStore(IKvStore kvStore, KvStoreProperties props, String capability) {
        return new PrefixedKvStore(kvStore, keyNormalizer(props), capability);
    }

    private KvKeyNormalizer keyNormalizer(KvStoreProperties props) {
        KvStoreProperties.Key key = props.getKey();
        return new KvKeyNormalizer(key.isEnabled(), key.getPrefix(), key.getEnv(), key.isAppEnabled(), key.getApp());
    }
}
