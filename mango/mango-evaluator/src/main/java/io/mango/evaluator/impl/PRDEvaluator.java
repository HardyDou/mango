package io.mango.evaluator.impl;

import io.mango.evaluator.ArtifactEvaluator;
import io.mango.evaluator.EvaluationReport;
import io.mango.evaluator.EvaluationReport.Dimensions;
import io.mango.evaluator.EvaluationReport.Issue;
import io.mango.evaluator.EvaluationReport.IssueType;
import io.mango.evaluator.EvaluationReport.Severity;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PRD 评估器
 *
 * 评估维度:
 * - Design Quality (设计质量): 结构完整性、需求清晰度
 * - Originality (创新性): 解决方案的独特性
 * - Craft (工艺): 文档质量、格式规范
 * - Functionality (功能性): 功能完整性、边界覆盖
 *
 * @author Mango
 */
public class PRDEvaluator implements ArtifactEvaluator {

    // 必需文件
    private static final String[] REQUIRED_FILES = {
            "README.md",
            "PRD.md"
    };

    // 必需章节
    private static final String[] REQUIRED_SECTIONS = {
            "用户故事",
            "功能描述",
            "字段设计",
            "API 设计",
            "数据库设计"
    };

    @Override
    public EvaluationReport evaluate(String path) {
        List<Issue> issues = new ArrayList<>();
        int designScore = 10, origScore = 10, craftScore = 10, funcScore = 10;

        Path prdPath = Paths.get(path);
        if (!Files.exists(prdPath)) {
            issues.add(Issue.builder()
                    .type(IssueType.STRUCTURE)
                    .severity(Severity.BLOCKER)
                    .file(path)
                    .description("PRD directory not found: " + path)
                    .rule("dev-flow-rules.md")
                    .build());
            return EvaluationReport.fail(
                    Dimensions.builder()
                            .designQuality(0)
                            .originality(0)
                            .craft(0)
                            .functionality(0)
                            .build(),
                    issues,
                    "PRD not found"
            );
        }

        // 1. 评估设计质量 (PRD 结构完整性)
        designScore = evaluateDesign(prdPath, issues);

        // 2. 评估创新性 (解决方案独特性)
        origScore = evaluateOriginality(prdPath, issues);

        // 3. 评估工艺 (文档质量)
        craftScore = evaluateCraft(prdPath, issues);

        // 4. 评估功能性 (功能覆盖完整性)
        funcScore = evaluateFunctionality(prdPath, issues);

        Dimensions dimensions = Dimensions.builder()
                .designQuality(designScore)
                .originality(origScore)
                .craft(craftScore)
                .functionality(funcScore)
                .build();

        boolean passed = dimensions.meetsMinimum(7, 6, 7, 8);

        return EvaluationReport.builder()
                .passed(passed)
                .score(dimensions.calculateTotalScore())
                .dimensions(dimensions)
                .issues(issues)
                .metadata(EvaluationReport.Metadata.builder()
                        .artifactType("prd")
                        .artifactPath(path)
                        .timestamp(System.currentTimeMillis())
                        .ruleVersion("1.0.0")
                        .evaluator("Mango PRDEvaluator")
                        .build())
                .build();
    }

    private int evaluateDesign(Path prdPath, List<Issue> issues) {
        int score = 10;

        // 检查必需文件
        for (String file : REQUIRED_FILES) {
            Path filePath = prdPath.resolve(file);
            if (!Files.exists(filePath)) {
                issues.add(Issue.builder()
                        .type(IssueType.STRUCTURE)
                        .severity(Severity.CRITICAL)
                        .file(filePath.toString())
                        .description("Required file missing: " + file)
                        .rule("dev-flow-rules.md")
                        .suggestion("Create " + file + " with required content")
                        .build());
                score -= 3;
            }
        }

        return Math.max(0, score);
    }

    private int evaluateOriginality(Path prdPath, List<Issue> issues) {
        // PRD 的创新性评估比较主观，这里给一个默认分数
        return 7;
    }

    private int evaluateCraft(Path prdPath, List<Issue> issues) {
        int score = 10;

        // 检查 README.md 格式
        Path readmePath = prdPath.resolve("README.md");
        if (Files.exists(readmePath)) {
            try {
                String content = Files.readString(readmePath);
                if (content.length() < 100) {
                    issues.add(Issue.builder()
                            .type(IssueType.STRUCTURE)
                            .severity(Severity.MINOR)
                            .file(readmePath.toString())
                            .description("README.md content too short")
                            .rule("dev-flow-rules.md")
                            .build());
                    score -= 2;
                }
            } catch (IOException e) {
                // ignore
            }
        }

        return Math.max(0, score);
    }

    private int evaluateFunctionality(Path prdPath, List<Issue> issues) {
        int score = 10;

        // 检查 PRD.md 必需章节
        Path prdFilePath = prdPath.resolve("PRD.md");
        if (Files.exists(prdFilePath)) {
            try {
                String content = Files.readString(prdFilePath);
                for (String section : REQUIRED_SECTIONS) {
                    if (!content.contains(section)) {
                        issues.add(Issue.builder()
                                .type(IssueType.STRUCTURE)
                                .severity(Severity.MAJOR)
                                .file(prdFilePath.toString())
                                .description("Required section missing: " + section)
                                .rule("dev-flow-rules.md")
                                .suggestion("Add " + section + " section to PRD.md")
                                .build());
                        score -= 2;
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }

        return Math.max(0, score);
    }

    @Override
    public String getName() {
        return "PRDEvaluator";
    }

    @Override
    public String getSupportedArtifactType() {
        return "prd";
    }
}
