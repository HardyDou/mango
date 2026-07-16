package io.mango.identity.core.adapter;

import io.mango.system.api.spi.SystemConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** 隔离系统参数远程协议，为身份域提供带默认值的配置读取。 */
@Component
@RequiredArgsConstructor
public class SysConfigValueAdapter {

    private final ObjectProvider<SystemConfigProvider> configProvider;

    public boolean booleanValue(String key, boolean defaultValue) {
        SystemConfigProvider config = configProvider.getIfAvailable();
        if (config == null) {
            return defaultValue;
        }
        try {
            Boolean result = config.getBooleanValue(key, defaultValue);
            return result == null ? defaultValue : result;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    public int integerValue(String key, int defaultValue) {
        SystemConfigProvider config = configProvider.getIfAvailable();
        if (config == null) {
            return defaultValue;
        }
        try {
            Integer result = config.getIntegerValue(key, defaultValue);
            return result == null ? defaultValue : result;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    public String stringValue(String key, String defaultValue) {
        SystemConfigProvider config = configProvider.getIfAvailable();
        if (config == null) {
            return defaultValue;
        }
        try {
            String result = config.getValue(key);
            return result == null ? defaultValue : result;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }
}
