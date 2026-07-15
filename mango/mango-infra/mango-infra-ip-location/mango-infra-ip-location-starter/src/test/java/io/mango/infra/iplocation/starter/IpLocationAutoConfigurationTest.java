package io.mango.infra.iplocation.starter;

import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.infra.iplocation.core.NoopIpLocationResolver;
import io.mango.infra.iplocation.core.cache.CachingIpLocationResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ByteArrayResource;

import static io.mango.infra.iplocation.starter.XdbFixture.xdb;
import static org.assertj.core.api.Assertions.assertThat;

class IpLocationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IpLocationAutoConfiguration.class));

    @Test
    void shouldCreateCachedNoopResolverWhenDisabled() {
        contextRunner.withPropertyValues("mango.ip-location.enabled=false")
                .run(context -> assertThat(context).getBean(IpLocationResolver.class)
                        .isInstanceOf(CachingIpLocationResolver.class));
    }

    @Test
    void shouldHonorCustomResolver() {
        IpLocationResolver custom = ip -> null;
        contextRunner.withBean(IpLocationResolver.class, () -> custom)
                .run(context -> assertThat(context.getBean(IpLocationResolver.class)).isSameAs(custom));
    }

    @Test
    void shouldFallBackToNoopWhenXdbIsMissing() {
        contextRunner.withPropertyValues("mango.ip-location.cache.enabled=false")
                .run(context -> assertThat(context).getBean(IpLocationResolver.class)
                        .isInstanceOf(NoopIpLocationResolver.class));
    }

    @Test
    void shouldFailFastWhenConfiguredXdbIsMissing() {
        contextRunner.withPropertyValues(
                        "mango.ip-location.cache.enabled=false",
                        "mango.ip-location.fail-fast=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldLoadXdbFromNonFileResourceLikeClasspathInsideJar() {
        IpLocationProperties properties = new IpLocationProperties();
        properties.getCache().setEnabled(false);
        properties.getIp2region().setXdbLocation(new ByteArrayResource(xdb("中国|0|浙江省|杭州市|电信")));

        IpLocationResolver resolver = new IpLocationAutoConfiguration().ipLocationResolver(properties);

        assertThat(resolver.resolve("8.8.8.8").getCity()).isEqualTo("杭州市");
    }
}
