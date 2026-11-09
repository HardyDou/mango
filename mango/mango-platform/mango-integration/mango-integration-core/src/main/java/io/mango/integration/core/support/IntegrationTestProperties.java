package io.mango.integration.core.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 集成连接测试配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.integration.test")
public class IntegrationTestProperties {

    /**
     * 是否允许连接测试访问回环、私网和链路本地地址；默认关闭，避免被用作内网探测。
     */
    private boolean allowPrivateNetwork = false;
}
