package io.mango.infra.doc.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API 文档配置。
 *
 * @author Mango
 */
@Data
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

    @Data
    public static class Contact {
        private String name = "Mango Team";
        private String email = "mango@example.com";
    }

    @Data
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
    }
}
