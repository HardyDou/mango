package io.mango.authorization.resource.sync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用模块资源清单同步配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.authorization.resource-sync.manifest")
public class AppModuleResourceManifestSyncProperties {

    /**
     * 是否启用资源清单同步。
     */
    private boolean enabled = true;

    /**
     * 同步模式：write 写入授权服务，read 只解析并输出日志。
     */
    private String mode = "write";

    /**
     * classpath 资源位置。
     */
    private List<String> locations = new ArrayList<>(List.of(
            "classpath*:META-INF/mango/resource-manifest.json",
            "classpath*:META-INF/mango/resource-manifests/*.json"));
}
