package io.mango.infra.event.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 领域事件配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.event")
public class DomainEventProperties {

    /**
     * 事件总线类型。当前实现 memory，后续扩展 redis/db。
     */
    private String type = "memory";
}
