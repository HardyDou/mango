package io.mango.infra.iplocation.starter;

import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.infra.iplocation.core.NoopIpLocationResolver;
import io.mango.infra.iplocation.core.cache.CachingIpLocationResolver;
import io.mango.infra.iplocation.core.ip2region.Ip2RegionXdbLocationResolver;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.io.File;

@AutoConfiguration
@EnableConfigurationProperties(IpLocationProperties.class)
public class IpLocationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IpLocationResolver ipLocationResolver(IpLocationProperties properties) {
        IpLocationResolver resolver = createResolver(properties);
        if (properties.getCache().isEnabled()) {
            return new CachingIpLocationResolver(
                    resolver,
                    properties.getCache().getMaximumSize(),
                    properties.getCache().getTtl());
        }
        return resolver;
    }

    private IpLocationResolver createResolver(IpLocationProperties properties) {
        if (!properties.isEnabled() || !"ip2region".equalsIgnoreCase(properties.getProvider())) {
            return new NoopIpLocationResolver();
        }
        try {
            Searcher searcher = createSearcher(properties.getIp2region());
            return new Ip2RegionXdbLocationResolver(searcher);
        } catch (Exception e) {
            if (properties.isFailFast()) {
                throw new IllegalStateException("Failed to initialize ip2region xdb resolver", e);
            }
            return new NoopIpLocationResolver();
        }
    }

    private Searcher createSearcher(IpLocationProperties.Ip2Region properties) throws Exception {
        Resource resource = properties.getXdbLocation();
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException("ip2region xdb file does not exist");
        }
        File file = resource.getFile();
        String path = file.getAbsolutePath();
        if (properties.isContentCacheEnabled()) {
            return Searcher.newWithBuffer(Searcher.loadContentFromFile(path));
        }
        if (properties.isVectorIndexEnabled()) {
            return Searcher.newWithVectorIndex(path, Searcher.loadVectorIndexFromFile(path));
        }
        return Searcher.newWithFileOnly(path);
    }
}
