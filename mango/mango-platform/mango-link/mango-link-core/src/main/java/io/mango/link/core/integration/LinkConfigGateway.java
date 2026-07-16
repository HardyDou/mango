package io.mango.link.core.integration;

import io.mango.system.api.spi.SystemConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Reads link configuration through the system module API and isolates its transport envelope.
 */
@Component
@RequiredArgsConstructor
public class LinkConfigGateway {

    private final ObjectProvider<SystemConfigProvider> configProvider;

    public boolean booleanValue(String configKey, String legacyConfigKey) {
        SystemConfigProvider config = configProvider.getIfAvailable();
        if (config == null) {
            return false;
        }
        String current = config.getValue(configKey);
        if (current != null) {
            return Boolean.parseBoolean(current);
        }
        return Boolean.TRUE.equals(config.getBooleanValue(legacyConfigKey, false));
    }
}
