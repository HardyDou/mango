package io.mango.access.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 边界入口配置属性。
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.access")
public class AccessProperties {

    /**
     * 是否启用边界入口认证
     */
    private boolean authEnabled = true;

    /**
     * 开启后，非 PUBLIC/INTERNAL 接口必须携带 permissionCode 参数。
     */
    private boolean requirePermissionCode = false;

}
