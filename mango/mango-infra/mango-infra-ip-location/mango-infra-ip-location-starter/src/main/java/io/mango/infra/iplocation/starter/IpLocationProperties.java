package io.mango.infra.iplocation.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "mango.ip-location")
public class IpLocationProperties {

    /**
     * 是否启用 IP 归属地解析。
     */
    private boolean enabled = true;

    /**
     * 解析提供方，支持 noop、ip2region。
     */
    private String provider = "ip2region";

    /**
     * xdb 缺失或初始化失败时是否阻止应用启动。
     */
    private boolean failFast = false;

    private final Cache cache = new Cache();

    private final Ip2Region ip2region = new Ip2Region();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public Cache getCache() {
        return cache;
    }

    public Ip2Region getIp2region() {
        return ip2region;
    }

    public static class Cache {
        private boolean enabled = true;
        private int maximumSize = 10000;
        private Duration ttl = Duration.ofHours(24);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(int maximumSize) {
            this.maximumSize = maximumSize;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static class Ip2Region {
        private Resource xdbLocation;
        private boolean vectorIndexEnabled = true;
        private boolean contentCacheEnabled = false;

        public Resource getXdbLocation() {
            return xdbLocation;
        }

        public void setXdbLocation(Resource xdbLocation) {
            this.xdbLocation = xdbLocation;
        }

        public boolean isVectorIndexEnabled() {
            return vectorIndexEnabled;
        }

        public void setVectorIndexEnabled(boolean vectorIndexEnabled) {
            this.vectorIndexEnabled = vectorIndexEnabled;
        }

        public boolean isContentCacheEnabled() {
            return contentCacheEnabled;
        }

        public void setContentCacheEnabled(boolean contentCacheEnabled) {
            this.contentCacheEnabled = contentCacheEnabled;
        }
    }
}
