package io.mango.evaluator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估报告
 *
 * 四维评估标准:
 * - Design Quality (30%): 代码设计质量
 * - Originality (20%): 创新性
 * - Craft (25%): 代码工艺
 * - Functionality (25%): 功能性
 *
 * @author Mango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationReport {

    /**
     * 评估状态
     */
    private boolean passed;

    /**
     * 总分 (0-100)
     */
    private int score;

    /**
     * 问题列表
     */
    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

    /**
     * 维度评分 (0-10)
     */
    private Dimensions dimensions;

    /**
     * 评估元数据
     */
    private Metadata metadata;

    /**
     * 问题严重程度
     */
    public enum Severity {
        BLOCKER,  // 阻断性问题
        CRITICAL, // 严重问题
        MAJOR,    // 主要问题
        MINOR,    // 次要问题
        INFO      // 信息级
    }

    /**
     * 问题类型
     */
    public enum IssueType {
        STRUCTURE,   // 结构问题
        NAMING,      // 命名问题
        DUPLICATE,   // 重复代码
        LENGTH,      // 长度问题
        COMPLEXITY,  // 复杂度问题
        COVERAGE,    // 覆盖率问题
        STYLE,       // 风格问题
        SECURITY     // 安全问题
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        /**
         * 问题类型
         */
        private IssueType type;

        /**
         * 严重程度
         */
        private Severity severity;

        /**
         * 文件路径
         */
        private String file;

        /**
         * 行号
         */
        private int line;

        /**
         * 问题描述
         */
        private String description;

        /**
         * 规则依据
         */
        private String rule;

        /**
         * 建议修复方式
         */
        private String suggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimensions {
        /**
         * 设计质量 (0-10) - 30%权重
         * 评估: 模块结构、类设计、方法职责、依赖关系
         */
        private int designQuality;

        /**
         * 创新性 (0-10) - 20%权重
         * 评估: 代码复用、设计模式应用、解决方案的独特性
         */
        private int originality;

        /**
         * 代码工艺 (0-10) - 25%权重
         * 评估: 代码整洁度、命名规范、重复度、注释质量
         */
        private int craft;

        /**
         * 功能性 (0-10) - 25%权重
         * 评估: 功能完整性、边界处理、错误处理、测试覆盖
         */
        private int functionality;

        /**
         * 计算总分 (加权)
         */
        public int calculateTotalScore() {
            return (designQuality * 30 + originality * 20 + craft * 25 + functionality * 25) / 10;
        }

        /**
         * 检查是否满足最低阈值
         */
        public boolean meetsMinimum(int designMin, int origMin, int craftMin, int funcMin) {
            return designQuality >= designMin
                    && originality >= origMin
                    && craft >= craftMin
                    && functionality >= funcMin;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        /**
         * 评估对象类型
         */
        private String artifactType;

        /**
         * 评估对象路径
         */
        private String artifactPath;

        /**
         * 评估时间戳
         */
        private long timestamp;

        /**
         * 评估规则版本
         */
        private String ruleVersion;

        /**
         * 评估人
         */
        private String evaluator;
    }

    /**
     * 创建通过结果
     */
    public static EvaluationReport pass(Dimensions dimensions, String message) {
        return EvaluationReport.builder()
                .passed(true)
                .score(dimensions.calculateTotalScore())
                .dimensions(dimensions)
                .metadata(Metadata.builder()
                        .timestamp(System.currentTimeMillis())
                        .build())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static EvaluationReport fail(Dimensions dimensions, List<Issue> issues, String message) {
        return EvaluationReport.builder()
                .passed(false)
                .score(dimensions.calculateTotalScore())
                .dimensions(dimensions)
                .issues(issues)
                .metadata(Metadata.builder()
                        .timestamp(System.currentTimeMillis())
                        .build())
                .build();
    }

    /**
     * 添加问题
     */
    public void addIssue(Issue issue) {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);
        this.passed = false;
    }
}
