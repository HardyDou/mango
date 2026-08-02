package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mango.bootstrap")
public class BootstrapProperties {

    private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 30;
    private static final Duration DEFAULT_RUNTIME_LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_RUNTIME_HEARTBEAT_INTERVAL = Duration.ofSeconds(10);

    private BootstrapMode mode;
    private BootstrapAction action = BootstrapAction.APPLY;
    private BootstrapStrategy strategy = BootstrapStrategy.ROLLING;
    private BootstrapPhase phase = BootstrapPhase.EXPAND;
    private String environmentKey = "default";
    private String receiptDirectory = ".mango/bootstrap";
    private int lockTimeoutSeconds = DEFAULT_LOCK_TIMEOUT_SECONDS;
    private String instanceId;
    private Duration runtimeLeaseTtl = DEFAULT_RUNTIME_LEASE_TTL;
    private Duration runtimeHeartbeatInterval = DEFAULT_RUNTIME_HEARTBEAT_INTERVAL;

    public BootstrapMode getMode() {
        return mode;
    }

    public void setMode(BootstrapMode mode) {
        this.mode = mode;
    }

    public BootstrapAction getAction() {
        return action;
    }

    public void setAction(BootstrapAction action) {
        this.action = action;
    }

    public BootstrapStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(BootstrapStrategy strategy) {
        this.strategy = strategy;
    }

    public BootstrapPhase getPhase() {
        return phase;
    }

    public void setPhase(BootstrapPhase phase) {
        this.phase = phase;
    }

    public String getEnvironmentKey() {
        return environmentKey;
    }

    public void setEnvironmentKey(String environmentKey) {
        this.environmentKey = environmentKey;
    }

    public String getReceiptDirectory() {
        return receiptDirectory;
    }

    public void setReceiptDirectory(String receiptDirectory) {
        this.receiptDirectory = receiptDirectory;
    }

    public int getLockTimeoutSeconds() {
        return lockTimeoutSeconds;
    }

    public void setLockTimeoutSeconds(int lockTimeoutSeconds) {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Duration getRuntimeLeaseTtl() {
        return runtimeLeaseTtl;
    }

    public void setRuntimeLeaseTtl(Duration runtimeLeaseTtl) {
        this.runtimeLeaseTtl = runtimeLeaseTtl;
    }

    public Duration getRuntimeHeartbeatInterval() {
        return runtimeHeartbeatInterval;
    }

    public void setRuntimeHeartbeatInterval(Duration runtimeHeartbeatInterval) {
        this.runtimeHeartbeatInterval = runtimeHeartbeatInterval;
    }
}
