package io.mango.plugin.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估结果
 * 用于 Generator-Evaluator 循环的结构化输出
 *
 * @author Mango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

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
     * 维度评分
     */
    private DimensionScores dimensions;

    /**
     * 评估详情
     */
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        /**
         * 问题类型
         */
        private String type;

        /**
         * 严重程度: BLOCKER, CRITICAL, MAJOR, MINOR, INFO
         */
        private String severity;

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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScores {
        /**
         * 设计质量 (30%)
         */
        private int designQuality;

        /**
         * 创新性 (20%)
         */
        private int originality;

        /**
         * 代码工艺 (25%)
         */
        private int craft;

        /**
         * 功能性 (25%)
         */
        private int functionality;
    }

    /**
     * 创建成功结果
     */
    public static EvaluationResult pass(int score, String message) {
        return EvaluationResult.builder()
                .passed(true)
                .score(score)
                .message(message)
                .dimensions(DimensionScores.builder()
                        .designQuality(10)
                        .originality(10)
                        .craft(10)
                        .functionality(10)
                        .build())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static EvaluationResult fail(int score, List<Issue> issues, String message) {
        return EvaluationResult.builder()
                .passed(false)
                .score(score)
                .issues(issues)
                .message(message)
                .build();
    }
}
