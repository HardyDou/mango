package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mango.bootstrap")
public class BootstrapProperties {

    private BootstrapMode mode;
    private BootstrapAction action = BootstrapAction.APPLY;
    private BootstrapStrategy strategy = BootstrapStrategy.ROLLING;
    private BootstrapPhase phase = BootstrapPhase.EXPAND;
    private String environmentKey = "default";
    private int lockTimeoutSeconds = 30;
    private String instanceId;
    private Duration runtimeLeaseTtl = Duration.ofSeconds(30);
    private Duration runtimeHeartbeatInterval = Duration.ofSeconds(10);

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
