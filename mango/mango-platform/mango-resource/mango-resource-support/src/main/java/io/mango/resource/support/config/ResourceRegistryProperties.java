package io.mango.resource.support.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源注册中心配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.resource.registry")
public class ResourceRegistryProperties {

    private boolean enabled = true;
    private boolean failOnConflict = true;
    private String instanceId = "";
    private int lockTtlSeconds = 300;
    private Remote remote = new Remote();
    private List<String> locations = new ArrayList<>(List.of(
            "classpath*:META-INF/mango/resources/*.json",
            "classpath*:META-INF/mango/resources/*.yml",
            "classpath*:META-INF/mango/resources/*.yaml"
    ));

    /**
     * 远程部署资源上报配置。
     */
    @Data
    public static class Remote {

        private boolean enabled = true;
        private String appCode = "";
        private String serviceCode = "";
    }
}
