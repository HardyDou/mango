package io.mango.infra.web.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Mango Web extension properties.
 */
@ConfigurationProperties(prefix = "mango.web")
public class MangoWebProperties {

    private final Cors cors = new Cors();
    private final Inner inner = new Inner();
    private final Mdc mdc = new Mdc();
    private final RequestContext requestContext = new RequestContext();

    public Cors getCors() {
        return cors;
    }

    public Inner getInner() {
        return inner;
    }

    public Mdc getMdc() {
        return mdc;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public static class Cors {
        private boolean enabled = true;
        private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));
        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
        private boolean allowCredentials = true;
        private long maxAge = 3600;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class Inner {
        private boolean enabled = true;
        private String secret = "";
        private long timestampToleranceSeconds = 300;
        private long nonceTtlSeconds = 300;
        private long pathRefreshIntervalSeconds = 300;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getTimestampToleranceSeconds() {
            return timestampToleranceSeconds;
        }

        public void setTimestampToleranceSeconds(long timestampToleranceSeconds) {
            this.timestampToleranceSeconds = timestampToleranceSeconds;
        }

        public long getNonceTtlSeconds() {
            return nonceTtlSeconds;
        }

        public void setNonceTtlSeconds(long nonceTtlSeconds) {
            this.nonceTtlSeconds = nonceTtlSeconds;
        }

        public long getPathRefreshIntervalSeconds() {
            return pathRefreshIntervalSeconds;
        }

        public void setPathRefreshIntervalSeconds(long pathRefreshIntervalSeconds) {
            this.pathRefreshIntervalSeconds = pathRefreshIntervalSeconds;
        }
    }

    public static class Mdc {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class RequestContext {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
