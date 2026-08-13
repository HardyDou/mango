package cn.keking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Controls the optional controls shown by the XLSX Web preview template.
 */
@Component
public class OfficeXlsxWebButtonsConfig {

    public static final String DEFAULT_ENABLED = "true";

    private static volatile boolean enabled = Boolean.parseBoolean(DEFAULT_ENABLED);

    @Value("${office.xlsx.web.buttons.enabled:true}")
    public void setEnabled(String value) {
        setEnabledValue(Boolean.parseBoolean(value));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabledValue(boolean value) {
        enabled = value;
    }
}
