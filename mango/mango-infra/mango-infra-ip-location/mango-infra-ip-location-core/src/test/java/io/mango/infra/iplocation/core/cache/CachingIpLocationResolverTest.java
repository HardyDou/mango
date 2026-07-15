package io.mango.infra.iplocation.core.cache;

import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class CachingIpLocationResolverTest {

    @Test
    void shouldNormalizeKeysAndIsolateCachedValueFromCallerMutation() {
        AtomicInteger invocations = new AtomicInteger();
        IpLocationResolver delegate = ip -> {
            invocations.incrementAndGet();
            IpLocation location = IpLocation.empty(ip);
            location.setCountry("China");
            location.setResolved(true);
            return location;
        };
        CachingIpLocationResolver resolver = new CachingIpLocationResolver(delegate, 10, Duration.ofMinutes(1));

        IpLocation first = resolver.resolve(" 8.8.8.8 ");
        first.setCountry("poisoned");
        IpLocation second = resolver.resolve("8.8.8.8");

        assertThat(invocations).hasValue(1);
        assertThat(second.getIp()).isEqualTo("8.8.8.8");
        assertThat(second.getCountry()).isEqualTo("China");
        assertThat(second).isNotSameAs(first);
    }

    @Test
    void shouldConvertNullDelegateResultToUnresolvedContractResult() {
        CachingIpLocationResolver resolver = new CachingIpLocationResolver(ip -> null, 10, Duration.ofMinutes(1));

        IpLocation result = resolver.resolve(" 1.1.1.1 ");

        assertThat(result).isNotNull();
        assertThat(result.getIp()).isEqualTo("1.1.1.1");
        assertThat(result.isResolved()).isFalse();
    }

    @Test
    void shouldContainDelegateFailureAsRequiredByResolverContract() {
        CachingIpLocationResolver resolver = new CachingIpLocationResolver(ip -> {
            throw new IllegalStateException("provider unavailable");
        }, 10, Duration.ofMinutes(1));

        IpLocation result = resolver.resolve("1.1.1.1");

        assertThat(result.getIp()).isEqualTo("1.1.1.1");
        assertThat(result.isResolved()).isFalse();
    }

    @Test
    void shouldExpireUsingMonotonicTicker() {
        AtomicInteger invocations = new AtomicInteger();
        AtomicLong ticker = new AtomicLong();
        CachingIpLocationResolver resolver = new CachingIpLocationResolver(ip -> {
            invocations.incrementAndGet();
            return IpLocation.empty(ip);
        }, 10, Duration.ofNanos(10), ticker::get);

        resolver.resolve("1.1.1.1");
        ticker.set(10);
        resolver.resolve("1.1.1.1");
        ticker.set(11);
        resolver.resolve("1.1.1.1");

        assertThat(invocations).hasValue(2);
    }
}
