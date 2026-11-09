package io.mango.authorization.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 前端运行单元可达性探测的网络边界配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.authorization.integration")
public class OutboundNetworkProperties {

    /**
     * 是否允许探测回环、私网和链路本地地址；默认关闭。
     */
    private boolean allowPrivateNetwork = false;
}
