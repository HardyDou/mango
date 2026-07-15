package io.mango.infra.log.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志配置属性
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.log")
public class LogProperties {

    /** 日志级别配置 */
    private Level level = new Level();

    /** 文件滚动配置 */
    private File file = new File();

    /** 审计日志配置 */
    private Operation operation = new Operation();

    /** JSON 输出配置 */
    private Json json = new Json();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public Level getLevel() {
        return level;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally replaces this nested property bean")
    public void setLevel(Level level) {
        this.level = level;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public File getFile() {
        return file;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally replaces this nested property bean")
    public void setFile(File file) {
        this.file = file;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public Operation getOperation() {
        return operation;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally replaces this nested property bean")
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public Json getJson() {
        return json;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally replaces this nested property bean")
    public void setJson(Json json) {
        this.json = json;
    }

    @Data
    public static class Level {
        /** 全局日志级别 */
        private String root = "INFO";
        /** 业务代码日志级别 */
        private String mango = "DEBUG";
        /** Spring 框架日志级别 */
        private String spring = "WARN";
        /** MyBatis 日志级别 */
        private String mybatis = "WARN";
        /** HTTP 客户端日志级别 */
        private String http = "INFO";
    }

    @Data
    public static class File {
        private static final int DEFAULT_MAX_HISTORY = 30;

        /** 单文件最大大小 */
        private String maxSize = "100MB";
        /** 保留天数 */
        private int maxHistory = DEFAULT_MAX_HISTORY;
        /** 总大小上限 */
        private String totalSizeCap = "3GB";
    }

    @Data
    public static class Operation {
        private static final int DEFAULT_MAX_HISTORY = 90;

        /** 是否开启审计日志 */
        private boolean enabled = true;
        /** 保留天数 */
        private int maxHistory = DEFAULT_MAX_HISTORY;
        /** 总大小上限 */
        private String totalSizeCap = "10GB";
    }

    @Data
    public static class Json {
        /** 是否启用 JSON 格式 */
        private boolean enabled = false;
    }
}
