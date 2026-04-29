package io.mango.authorization.access.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 边界入口配置属性。
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.authorization.access")
public class AccessProperties {

    /**
     * 是否启用网关认证
     */
    private boolean authEnabled = true;

}
