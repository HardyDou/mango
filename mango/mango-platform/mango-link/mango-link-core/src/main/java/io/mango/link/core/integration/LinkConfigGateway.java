package io.mango.link.core.integration;

import io.mango.common.result.R;
import io.mango.system.api.SysConfigApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Reads link configuration through the system module API and isolates its transport envelope.
 */
@Component
@RequiredArgsConstructor
public class LinkConfigGateway {

    private final ObjectProvider<SysConfigApi> sysConfigApi;

    public boolean booleanValue(String configKey, String legacyConfigKey) {
        SysConfigApi api = sysConfigApi.getIfAvailable();
        if (api == null) {
            return false;
        }
        R<String> current = api.getValue(configKey);
        if (current != null && current.isSuccess()) {
            return Boolean.parseBoolean(current.getData());
        }
        R<Boolean> legacy = api.getBooleanValue(legacyConfigKey, false);
        return legacy != null && Boolean.TRUE.equals(legacy.getData());
    }
}
