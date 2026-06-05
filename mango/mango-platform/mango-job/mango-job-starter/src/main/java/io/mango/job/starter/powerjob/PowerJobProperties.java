package io.mango.job.starter.powerjob;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * PowerJob Adapter 配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mango.job.powerjob")
public class PowerJobProperties {

    /**
     * 是否启用 PowerJob 真实同步。
     */
    private boolean enabled;

    /**
     * PowerJob Server 地址列表。
     */
    private List<String> serverAddresses = new ArrayList<>();

    /**
     * PowerJob 应用名称。
     */
    private String appName;

    /**
     * PowerJob 应用密码。
     */
    private String password;

    /**
     * PowerJob 应用 ID。
     */
    private Long appId;

    /**
     * HTTP 连接超时毫秒。
     */
    private int connectionTimeoutMillis = 5000;

    /**
     * HTTP 读取超时毫秒。
     */
    private int readTimeoutMillis = 10000;

    /**
     * HTTP 写入超时毫秒。
     */
    private int writeTimeoutMillis = 10000;

    /**
     * 默认最大实例数。
     */
    private int maxInstanceNum = 1;

    /**
     * 默认并发数。
     */
    private int concurrency = 1;
}
