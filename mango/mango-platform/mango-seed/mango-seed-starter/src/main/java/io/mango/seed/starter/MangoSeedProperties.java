package io.mango.seed.starter;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mango.seed")
public class MangoSeedProperties {

    private boolean enabled = false;
    private String profile = "official";
    private final Admin admin = new Admin();
    private final Tenant tenant = new Tenant();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public static class Admin {

        private String username = "admin";
        private String initialPassword = "";
        private String nickname = "Administrator";
        private String email = "admin@mango.local";
        private String phone = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getInitialPassword() {
            return initialPassword;
        }

        public void setInitialPassword(String initialPassword) {
            this.initialPassword = initialPassword;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class Tenant {

        private String code = "default";
        private String name = "芒果集团";
        private String institutionType = "PLATFORM";
        private Set<String> capabilityCodes = new LinkedHashSet<>(Set.of(
                "PLATFORM_ADMIN", "SYSTEM_ADMIN", "AUTH_ADMIN", "ORG_ADMIN", "WORKFLOW"
        ));
        private String packageCode = "platform_admin";
        private String appCode = "internal-admin";

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getInstitutionType() {
            return institutionType;
        }

        public void setInstitutionType(String institutionType) {
            this.institutionType = institutionType;
        }

        public Set<String> getCapabilityCodes() {
            return capabilityCodes;
        }

        public void setCapabilityCodes(Set<String> capabilityCodes) {
            this.capabilityCodes = capabilityCodes;
        }

        public String getPackageCode() {
            return packageCode;
        }

        public void setPackageCode(String packageCode) {
            this.packageCode = packageCode;
        }

        public String getAppCode() {
            return appCode;
        }

        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }
    }
}
