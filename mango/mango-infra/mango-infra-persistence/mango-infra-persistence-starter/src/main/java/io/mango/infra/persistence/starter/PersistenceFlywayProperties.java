package io.mango.infra.persistence.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flyway 迁移配置。
 * <p>
 * 配置前缀：{@code mango.persistence.flyway}
 * <p>
 * 示例：
 * <pre>
 * mango:
 *   persistence:
 *     flyway:
 *       enabled: true
 *       modules:
 *         user:
 *           enabled: true
 *           baseline-on-migrate: false
 *         area:
 *           enabled: false
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mango.persistence.flyway")
public class PersistenceFlywayProperties {

    /**
     * 全局迁移开关，默认开启。
     */
    private boolean enabled = true;

    /**
     * 模块级迁移配置。
     * Key 为模块名称，例如 user、area、org。
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this mutable module map"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally stores this mutable module map"))
    private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    /**
     * 是否启用默认外部升级目录。
     * 启用后，未显式配置 locations 的模块会自动追加 {upgradeRoot}/{module}。
     */
    private boolean upgradeLocationsEnabled = true;

    /**
     * 默认外部升级根目录。
     * 为空时按 mango.upgrade.root、MANGO_UPGRADE_DIR、mango.home/MANGO_HOME、/opt/mango/upgrade 解析。
     */
    private String upgradeRoot;

    /** Optional one-shot schema snapshot for a genuinely empty database. */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally stores this nested property bean"))
    private ColdBaseline coldBaseline = new ColdBaseline();

    @Data
    public static class ColdBaseline {
        private boolean enabled = false;
    }

    @Data
    public static class ModuleConfig {
        /**
         * 是否启用当前模块迁移，默认开启。
         */
        private boolean enabled = true;

        /**
         * 当前模块有 classpath migration 但本应用有意不执行时的原因。
         * enabled=false 时必须填写，避免误跳过已进入 classpath 的模块 migration。
         */
        private String skipReason;

        /**
         * 是否对当前模块启用基线迁移。
         * 适用于数据库已有表结构、Flyway 需要从指定基线开始接管的场景。
         */
        private boolean baselineOnMigrate = true;

        /**
         * 是否允许当前模块按非顺序版本补跑迁移。
         * 仅用于明确的历史库兼容或升级补偿场景。
         * 未配置时由 Mango 模块兼容策略决定，业务模块默认关闭。
         */
        private Boolean outOfOrder;

        /**
         * 当前模块 Flyway history table。
         * 未配置时使用 flyway_schema_history_{module}。
         */
        private String historyTable;

        /**
         * 当前模块迁移脚本位置。为空时使用 classpath:db/migration/{module}。
         * 支持 classpath:、filesystem:，以及 http(s) 单个 SQL 文件。
         */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration binding intentionally exposes this mutable location list"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration binding intentionally stores this mutable location list"))
        private List<String> locations = new ArrayList<>();

        /**
         * 当前模块破坏性迁移脚本位置。为空时仅在
         * classpath:db/migration-contract/{module} 存在时启用。
         * EXPAND 与 CONTRACT 使用同一个 Flyway history table，版本号必须全局唯一。
         */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration binding intentionally exposes this mutable location list"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration binding intentionally stores this mutable location list"))
        private List<String> contractLocations = new ArrayList<>();

        /**
         * 当前模块独立迁移数据源。
         * 未配置时使用应用主数据源。
         */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration binding intentionally exposes this nested property bean"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration binding intentionally stores this nested property bean"))
        private DataSourceConfig datasource = new DataSourceConfig();

        /** 当前模块拥有的空库快速基线。 */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration binding intentionally exposes this nested property bean"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration binding intentionally stores this nested property bean"))
        private BaselineConfig baseline = new BaselineConfig();

        /**
         * 是否在迁移前校验历史记录。
         */
        private boolean validateOnMigrate = true;

        /**
         * 是否忽略数据库中存在但当前代码已移除的历史迁移。
         * 仅用于模块重构后接管存量历史，默认关闭。
         */
        private boolean ignoreMissingMigrations = false;

        public boolean isOutOfOrder() {
            return Boolean.TRUE.equals(outOfOrder);
        }
    }

    @Data
    public static class DataSourceConfig {
        /**
         * 逻辑数据源名称。多个模块指向同一显式 JDBC URL 时必须配置相同名称。
         */
        private String logicalName;

        /**
         * JDBC URL。配置后当前模块迁移使用独立数据库。
         */
        private String url;

        /**
         * JDBC 驱动类名。
         */
        private String driverClassName;

        /**
         * 数据库用户名。
         */
        private String username;

        /**
         * 数据库密码。
         */
        private String password;
    }

    @Data
    public static class BaselineConfig {
        /**
         * 当前模块唯一的基线 SQL。未配置时按
         * classpath*:db/baseline/{module}/B*__baseline.sql 自动发现。
         */
        private String location;

        /**
         * 基线包含的最高 migration 版本。未配置时从 B{version}__*.sql 文件名解析。
         */
        private String version;
    }
}
