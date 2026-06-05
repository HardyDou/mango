package io.mango.job.starter.powerjob;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * PowerJob Adapter 配置。
 */
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

    /**
     * Worker 配置。
     */
    private Worker worker = new Worker();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getServerAddresses() {
        return serverAddresses;
    }

    public void setServerAddresses(List<String> serverAddresses) {
        this.serverAddresses = serverAddresses;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public int getMaxInstanceNum() {
        return maxInstanceNum;
    }

    public void setMaxInstanceNum(int maxInstanceNum) {
        this.maxInstanceNum = maxInstanceNum;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    /**
     * PowerJob Worker 配置。
     */
    public static class Worker {

        /**
         * 是否在当前 Mango 进程内启动 PowerJob Worker。
         */
        private boolean enabled;

        /**
         * Worker 应用名，缺省使用 adapter appName。
         */
        private String appName;

        /**
         * Worker 连接的 PowerJob Server 地址，缺省使用 adapter serverAddresses。
         */
        private List<String> serverAddresses = new ArrayList<>();

        /**
         * Worker 通信协议。
         */
        private String protocol = "HTTP";

        /**
         * Worker 监听端口；未配置时由 PowerJob 随机选择。
         */
        private Integer port;

        /**
         * 兼容旧 akka port 配置；未配置时忽略。
         */
        private Integer akkaPort;

        /**
         * 结果存储策略。
         */
        private String storeStrategy = "DISK";

        /**
         * 是否允许 JobCenter 暂不可达时延迟连接。
         */
        private boolean allowLazyConnectServer = true;

        /**
         * Worker 标签。
         */
        private String tag;

        /**
         * 最大轻量任务数。
         */
        private Integer maxLightweightTaskNum = 1024;

        /**
         * 最大重量任务数。
         */
        private Integer maxHeavyweightTaskNum = 64;

        /**
         * 健康上报间隔秒。
         */
        private Integer healthReportInterval = 10;

        /**
         * 最大结果长度。
         */
        private int maxResultLength = 8192;

        /**
         * 最大追加工作流上下文长度。
         */
        private int maxAppendedWfContextLength = 8192;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public List<String> getServerAddresses() {
            return serverAddresses;
        }

        public void setServerAddresses(List<String> serverAddresses) {
            this.serverAddresses = serverAddresses;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Integer getAkkaPort() {
            return akkaPort;
        }

        public void setAkkaPort(Integer akkaPort) {
            this.akkaPort = akkaPort;
        }

        public String getStoreStrategy() {
            return storeStrategy;
        }

        public void setStoreStrategy(String storeStrategy) {
            this.storeStrategy = storeStrategy;
        }

        public boolean isAllowLazyConnectServer() {
            return allowLazyConnectServer;
        }

        public void setAllowLazyConnectServer(boolean allowLazyConnectServer) {
            this.allowLazyConnectServer = allowLazyConnectServer;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public Integer getMaxLightweightTaskNum() {
            return maxLightweightTaskNum;
        }

        public void setMaxLightweightTaskNum(Integer maxLightweightTaskNum) {
            this.maxLightweightTaskNum = maxLightweightTaskNum;
        }

        public Integer getMaxHeavyweightTaskNum() {
            return maxHeavyweightTaskNum;
        }

        public void setMaxHeavyweightTaskNum(Integer maxHeavyweightTaskNum) {
            this.maxHeavyweightTaskNum = maxHeavyweightTaskNum;
        }

        public Integer getHealthReportInterval() {
            return healthReportInterval;
        }

        public void setHealthReportInterval(Integer healthReportInterval) {
            this.healthReportInterval = healthReportInterval;
        }

        public int getMaxResultLength() {
            return maxResultLength;
        }

        public void setMaxResultLength(int maxResultLength) {
            this.maxResultLength = maxResultLength;
        }

        public int getMaxAppendedWfContextLength() {
            return maxAppendedWfContextLength;
        }

        public void setMaxAppendedWfContextLength(int maxAppendedWfContextLength) {
            this.maxAppendedWfContextLength = maxAppendedWfContextLength;
        }
    }
}
