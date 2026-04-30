package io.mango.infra.log.starter;

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
        /** 单文件最大大小 */
        private String maxSize = "100MB";
        /** 保留天数 */
        private int maxHistory = 30;
        /** 总大小上限 */
        private String totalSizeCap = "3GB";
    }

    @Data
    public static class Operation {
        /** 是否开启审计日志 */
        private boolean enabled = true;
        /** 保留天数 */
        private int maxHistory = 90;
        /** 总大小上限 */
        private String totalSizeCap = "10GB";
    }

    @Data
    public static class Json {
        /** 是否启用 JSON 格式 */
        private boolean enabled = false;
    }
}
