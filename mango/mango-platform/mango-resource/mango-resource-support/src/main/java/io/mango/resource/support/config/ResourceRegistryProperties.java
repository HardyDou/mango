package io.mango.resource.support.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源注册中心配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.resource.registry")
public class ResourceRegistryProperties {

    private static final int DEFAULT_LOCK_TTL_SECONDS = 300;
    // Keep below Spring Boot's default 30s timeout-per-shutdown-phase.
    private static final int DEFAULT_SHUTDOWN_WAIT_SECONDS = 25;

    private boolean enabled = true;
    private boolean failOnConflict = true;
    private String instanceId = "";
    private int lockTtlSeconds = DEFAULT_LOCK_TTL_SECONDS;
    private int shutdownWaitSeconds = DEFAULT_SHUTDOWN_WAIT_SECONDS;
    private Remote remote = new Remote();
    private List<String> locations = new ArrayList<>(List.of(
            "classpath*:META-INF/mango/resources/*.json",
            "classpath*:META-INF/mango/resources/*.yml",
            "classpath*:META-INF/mango/resources/*.yaml"
    ));
    private boolean demoEnabled = false;
    private List<String> demoLocations = new ArrayList<>(List.of(
            "classpath*:META-INF/mango/demo/*.json",
            "classpath*:META-INF/mango/demo/*.yml",
            "classpath*:META-INF/mango/demo/*.yaml"
    ));

    public List<String> getLocations() {
        return List.copyOf(locations);
    }

    public void setLocations(List<String> locations) {
        if (locations == null) {
            this.locations = new ArrayList<>();
            return;
        }
        this.locations = new ArrayList<>(locations);
    }

    public List<String> getDemoLocations() {
        return List.copyOf(demoLocations);
    }

    public void setDemoLocations(List<String> demoLocations) {
        if (demoLocations == null) {
            this.demoLocations = new ArrayList<>();
            return;
        }
        this.demoLocations = new ArrayList<>(demoLocations);
    }

    /**
     * 远程部署资源上报配置。
     */
    @Data
    public static class Remote {

        private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofSeconds(10);
        private static final Duration DEFAULT_RETRY_MAX_INTERVAL = Duration.ofMinutes(1);

        private boolean enabled = true;
        private String appCode = "";
        private String serviceCode = "";
        private Duration retryInterval = DEFAULT_RETRY_INTERVAL;
        private Duration retryMaxInterval = DEFAULT_RETRY_MAX_INTERVAL;
    }
}
