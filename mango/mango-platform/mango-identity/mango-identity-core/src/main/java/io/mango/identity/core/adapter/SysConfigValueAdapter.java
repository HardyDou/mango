package io.mango.identity.core.adapter;

import io.mango.common.result.R;
import io.mango.system.api.SysConfigApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** 隔离系统参数远程协议，为身份域提供带默认值的配置读取。 */
@Component
@RequiredArgsConstructor
public class SysConfigValueAdapter {

    private final ObjectProvider<SysConfigApi> sysConfigApiProvider;

    public boolean booleanValue(String key, boolean defaultValue) {
        SysConfigApi api = sysConfigApiProvider.getIfAvailable();
        if (api == null) {
            return defaultValue;
        }
        try {
            R<Boolean> result = api.getBooleanValue(key, defaultValue);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            return defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    public int integerValue(String key, int defaultValue) {
        SysConfigApi api = sysConfigApiProvider.getIfAvailable();
        if (api == null) {
            return defaultValue;
        }
        try {
            R<Integer> result = api.getIntegerValue(key, defaultValue);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            return defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    public String stringValue(String key, String defaultValue) {
        SysConfigApi api = sysConfigApiProvider.getIfAvailable();
        if (api == null) {
            return defaultValue;
        }
        try {
            R<String> result = api.getValue(key);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            return defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }
}
