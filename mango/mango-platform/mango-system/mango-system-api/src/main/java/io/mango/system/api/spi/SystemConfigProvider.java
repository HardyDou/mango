package io.mango.system.api.spi;

public interface SystemConfigProvider {
    String getValue(String configKey);
    Boolean getBooleanValue(String configKey, Boolean defaultValue);
    Integer getIntegerValue(String configKey, Integer defaultValue);
}
