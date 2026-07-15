package io.mango.infra.web.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Mango Web 扩展配置项。
 */
@ConfigurationProperties(prefix = "mango.web")
public class MangoWebProperties {

    private static final long DEFAULT_CORS_MAX_AGE_SECONDS = 3600L;
    private static final long DEFAULT_INNER_TIMEOUT_SECONDS = 300L;

    private final Cors cors = new Cors();
    private final Inner inner = new Inner();
    private final Mdc mdc = new Mdc();
    private final RequestContext requestContext = new RequestContext();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring configuration exposes nested bindable beans")
    public Cors getCors() {
        return cors;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring configuration exposes nested bindable beans")
    public Inner getInner() {
        return inner;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring configuration exposes nested bindable beans")
    public Mdc getMdc() {
        return mdc;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring configuration exposes nested bindable beans")
    public RequestContext getRequestContext() {
        return requestContext;
    }

    public static class Cors {
        private boolean enabled = true;
        private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));
        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
        private boolean allowCredentials = true;
        private long maxAge = DEFAULT_CORS_MAX_AGE_SECONDS;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Mutable list is required for property binding")
        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Mutable list is required for property binding")
        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Mutable list is required for property binding")
        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Mutable list is required for property binding")
        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Mutable list is required for property binding")
        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Mutable list is required for property binding")
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
        private long timestampToleranceSeconds = DEFAULT_INNER_TIMEOUT_SECONDS;
        private long nonceTtlSeconds = DEFAULT_INNER_TIMEOUT_SECONDS;
        private long pathRefreshIntervalSeconds = DEFAULT_INNER_TIMEOUT_SECONDS;

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
