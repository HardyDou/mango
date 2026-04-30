package io.mango.infra.persistence.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化基础设施配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.persistence")
public class PersistenceProperties {

    /**
     * Flyway 迁移配置。
     */
    private final Flyway flyway = new Flyway();

    /**
     * MyBatis-Plus 配置。
     */
    private final MybatisPlus mybatisPlus = new MybatisPlus();

    /**
     * 审计字段配置。
     */
    private final Audit audit = new Audit();

    /**
     * 数据库结构校验配置。
     */
    private final SchemaValidation schemaValidation = new SchemaValidation();

    @Data
    public static class Flyway {
        /**
         * 是否启用 Mango 管理的数据库迁移。
         */
        private boolean enabled = true;
    }

    @Data
    public static class MybatisPlus {
        /**
         * 分页插件配置。
         */
        private final Pagination pagination = new Pagination();
    }

    @Data
    public static class Pagination {
        /**
         * 是否启用默认分页插件。
         */
        private boolean enabled = true;

        /**
         * 溢出页是否回到首页。
         */
        private boolean overflow = false;

        /**
         * 单页最大条数。
         */
        private Long maxLimit = 500L;

        /**
         * 数据库类型；为空时由 MyBatis-Plus 自动判断。
         */
        private String dbType;
    }

    @Data
    public static class Audit {
        /**
         * 是否启用审计字段自动填充。
         */
        private boolean enabled = true;
    }

    @Data
    public static class SchemaValidation {
        /**
         * 是否启用启动期数据库元数据校验。
         */
        private boolean enabled = true;

        /**
         * 发现不符合规范的表时是否立即启动失败。
         */
        private boolean failFast = false;

        /**
         * 必须存在的字段。
         */
        private List<String> requiredColumns = new ArrayList<>(
                List.of("created_by", "created_at", "updated_by", "updated_at", "tenant_id"));

        /**
         * 不参与校验的表名。
         */
        private List<String> excludedTables = new ArrayList<>(
                List.of("flyway_schema_history", "databasechangelog", "databasechangeloglock",
                        "kv_record", "infra_kv_entry", "sys_login_log", "sys_operation_log"));
    }
}
