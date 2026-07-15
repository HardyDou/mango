package io.mango.infra.doc.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

/**
 * API 文档配置。
 *
 * @author Mango
 */
@ConfigurationProperties(prefix = "mango.doc")
public class DocProperties {

    /**
     * 是否启用 API 文档。
     */
    private boolean enabled = true;

    /**
     * API 标题。
     */
    private String title = "Mango API";

    /**
     * API 描述。
     */
    private String description = "Mango Scaffold API Documentation";

    /**
     * API 版本。
     */
    private String version = "1.0.0";

    /**
     * 默认 OpenAPI 分组。
     */
    private String group = "public-api";

    /**
     * 默认分组包含的路径。
     */
    private String[] pathsToMatch = {"/api/**"};

    /**
     * 模块分组配置。
     */
    private ModuleGrouping moduleGrouping = new ModuleGrouping();

    /**
     * 联系人信息。
     */
    private Contact contact = new Contact();

    /**
     * License 名称。
     */
    private String license = "Apache 2.0";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String[] getPathsToMatch() {
        return Arrays.copyOf(pathsToMatch, pathsToMatch.length);
    }

    public void setPathsToMatch(String[] pathsToMatch) {
        if (pathsToMatch == null) {
            this.pathsToMatch = new String[0];
        } else {
            this.pathsToMatch = Arrays.copyOf(pathsToMatch, pathsToMatch.length);
        }
    }

    public ModuleGrouping getModuleGrouping() {
        return new ModuleGrouping(moduleGrouping);
    }

    public void setModuleGrouping(ModuleGrouping moduleGrouping) {
        if (moduleGrouping == null) {
            this.moduleGrouping = new ModuleGrouping();
        } else {
            this.moduleGrouping = new ModuleGrouping(moduleGrouping);
        }
    }

    public Contact getContact() {
        return new Contact(contact);
    }

    public void setContact(Contact contact) {
        if (contact == null) {
            this.contact = new Contact();
        } else {
            this.contact = new Contact(contact);
        }
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public static class Contact {
        private String name = "Mango Team";
        private String email = "mango@example.com";

        public Contact() {
        }

        private Contact(Contact source) {
            this.name = source.name;
            this.email = source.email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ModuleGrouping {

        /**
         * 是否按 Mango 模块元数据生成 Swagger 分组。
         */
        private boolean enabled = true;

        /**
         * 是否保留默认全局分组。
         */
        private boolean includeDefaultGroup = true;

        /**
         * 是否为接口添加对内/对外 tag 和 OpenAPI extension。
         */
        private boolean includeScopeTags = true;

        public ModuleGrouping() {
        }

        private ModuleGrouping(ModuleGrouping source) {
            this.enabled = source.enabled;
            this.includeDefaultGroup = source.includeDefaultGroup;
            this.includeScopeTags = source.includeScopeTags;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIncludeDefaultGroup() {
            return includeDefaultGroup;
        }

        public void setIncludeDefaultGroup(boolean includeDefaultGroup) {
            this.includeDefaultGroup = includeDefaultGroup;
        }

        public boolean isIncludeScopeTags() {
            return includeScopeTags;
        }

        public void setIncludeScopeTags(boolean includeScopeTags) {
            this.includeScopeTags = includeScopeTags;
        }
    }
}
