package io.mango.infra.module.starter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mango 模块信息配置。
 */
@ConfigurationProperties(prefix = "mango.module.module-service")
public class ModuleProperties {

    private boolean enabled = true;

    private Map<String, ModuleServiceProperties> modules = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, ModuleServiceProperties> getModules() {
        return modules;
    }

    public void setModules(Map<String, ModuleServiceProperties> modules) {
        this.modules = modules == null ? new LinkedHashMap<>() : modules;
    }

    public static class ModuleServiceProperties {

        private String serviceName;

        private String contextPath;

        private String modulePath;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public String getModulePath() {
            return modulePath;
        }

        public void setModulePath(String modulePath) {
            this.modulePath = modulePath;
        }
    }
}
