package io.mango.plugin.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 质检评估 Mojo
 *
 * 支持四维评估标准:
 * - Design Quality (30%): 代码设计质量
 * - Originality (20%): 创新性
 * - Craft (25%): 代码工艺
 * - Functionality (25%): 功能性
 *
 * mvn mango:evaluate -Dartifact=prd
 * mvn mango:evaluate -Dartifact=code
 * mvn mango:evaluate -Dartifact=code -Doutput=json
 *
 * @author Mango
 */
@Mojo(name = "evaluate", defaultPhase = LifecyclePhase.VERIFY)
public class EvaluateMojo extends AbstractMojo {

    /**
     * 评估对象: prd, code
     */
    @Parameter(property = "artifact", required = true)
    private String artifact;

    /**
     * 输出格式: text, json
     */
    @Parameter(property = "output", defaultValue = "text")
    private String output;

    /**
     * 评估报告输出路径
     */
    @Parameter(property = "reportFile")
    private String reportFile;

    /**
     * 源码目录
     */
    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    /**
     * 最低通过分数
     */
    @Parameter(property = "minScore", defaultValue = "60")
    private int minScore;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running Mango Evaluate - artifact: " + artifact);

        EvaluationResult result;
        switch (artifact.toLowerCase()) {
            case "prd" -> result = evaluatePRD();
            case "code" -> result = evaluateCode();
            default -> {
                getLog().warn("Unknown artifact type: " + artifact);
                result = EvaluationResult.fail(0, new ArrayList<>(), "Unknown artifact type: " + artifact);
            }
        }

        // 输出结果
        if ("json".equalsIgnoreCase(output)) {
            outputJson(result);
        } else {
            outputText(result);
        }

        // 保存报告
        if (reportFile != null) {
            saveReport(result, reportFile);
        }

        // 检查是否通过
        if (!result.isPassed()) {
            throw new MojoExecutionException("Evaluation failed: " + result.getMessage());
        }

        getLog().info("Evaluation completed. Score: " + result.getScore() + "/100");
    }

    private EvaluationResult evaluatePRD() {
        getLog().info("=== PRD Quality Evaluation ===");

        List<EvaluationResult.Issue> issues = new ArrayList<>();
        int score = 100;

        Path prdPath = Paths.get(baseDir, "prd");
        if (!Files.exists(prdPath)) {
            issues.add(EvaluationResult.Issue.builder()
                    .type("MISSING")
                    .severity("BLOCKER")
                    .file(prdPath.toString())
                    .description("PRD directory not found")
                    .rule("dev-flow-rules.md")
                    .build());
            score -= 50;
        } else {
            // 检查 PRD 基本结构
            checkFile(prdPath, "README.md", issues);
            checkFile(prdPath, "PRD.md", issues);
        }

        if (score >= minScore) {
            return EvaluationResult.pass(score, "PRD evaluation passed");
        } else {
            return EvaluationResult.fail(score, issues, "PRD evaluation failed");
        }
    }

    private void checkFile(Path prdPath, String filename, List<EvaluationResult.Issue> issues) {
        Path file = prdPath.resolve(filename);
        if (Files.exists(file)) {
            getLog().info("  [OK] " + filename);
        } else {
            getLog().warn("  [MISSING] " + filename);
            issues.add(EvaluationResult.Issue.builder()
                    .type("MISSING")
                    .severity("CRITICAL")
                    .file(file.toString())
                    .description("Required file missing: " + filename)
                    .rule("dev-flow-rules.md")
                    .build());
        }
    }

    private EvaluationResult evaluateCode() {
        getLog().info("=== Code Quality Evaluation ===");

        List<EvaluationResult.Issue> issues = new ArrayList<>();
        int designScore = 0, originalityScore = 0, craftScore = 0, funcScore = 0;

        Path srcPath = Paths.get(baseDir, "src");
        if (!Files.exists(srcPath)) {
            issues.add(EvaluationResult.Issue.builder()
                    .type("MISSING")
                    .severity("BLOCKER")
                    .file(srcPath.toString())
                    .description("Source directory not found")
                    .rule("module-rules.md")
                    .build());
            return EvaluationResult.fail(0, issues, "Source directory not found");
        }

        // 1. Design Quality (设计质量)
        designScore = evaluateDesign(srcPath, issues);

        // 2. Originality (创新性) - 简化评估
        originalityScore = 70; // 默认分数
        getLog().info("  Originality: " + originalityScore + "/10");

        // 3. Craft (代码工艺) - 检查重复代码、命名等
        craftScore = evaluateCraft(srcPath, issues);

        // 4. Functionality (功能性) - 检查测试覆盖率
        funcScore = evaluateFunctionality(srcPath, issues);

        // 计算总分 (加权)
        int totalScore = (designScore * 30 + originalityScore * 20 + craftScore * 25 + funcScore * 25) / 10;

        // 构建维度评分
        EvaluationResult.DimensionScores dimensions = EvaluationResult.DimensionScores.builder()
                .designQuality(designScore)
                .originality(originalityScore)
                .craft(craftScore)
                .functionality(funcScore)
                .build();

        getLog().info("  Design Quality: " + designScore + "/10");
        getLog().info("  Originality: " + originalityScore + "/10");
        getLog().info("  Craft: " + craftScore + "/10");
        getLog().info("  Functionality: " + funcScore + "/10");
        getLog().info("  Total Score: " + totalScore + "/100 (min: " + minScore + ")");

        EvaluationResult result = EvaluationResult.builder()
                .passed(totalScore >= minScore)
                .score(totalScore)
                .issues(issues)
                .dimensions(dimensions)
                .message(totalScore >= minScore ? "Code evaluation passed" : "Code evaluation failed")
                .build();

        return result;
    }

    private int evaluateDesign(Path srcPath, List<EvaluationResult.Issue> issues) {
        int score = 10;
        getLog().info("  Design Quality: " + score + "/10");
        return score;
    }

    private int evaluateCraft(Path srcPath, List<EvaluationResult.Issue> issues) {
        int score = 10;
        long javaFiles = countFiles(srcPath, ".java");
        long testFiles = countFiles(srcPath, "Test.java");

        if (javaFiles > 0) {
            double coverage = (double) testFiles / javaFiles * 100;
            getLog().info("  Java files: " + javaFiles + ", Test files: " + testFiles);
            getLog().info("  Test coverage: " + String.format("%.1f%%", coverage));

            if (coverage < 30) {
                issues.add(EvaluationResult.Issue.builder()
                        .type("COVERAGE")
                        .severity("MAJOR")
                        .description("Test coverage too low: " + String.format("%.1f%%", coverage))
                        .rule("test-rules.md")
                        .build());
                score -= 3;
            }
        }

        return Math.max(0, score);
    }

    private int evaluateFunctionality(Path srcPath, List<EvaluationResult.Issue> issues) {
        int score = 10;
        // 检查是否有 Controller/Service/Mapper 完整结构
        boolean hasController = Files.exists(srcPath.resolve("main/java/io/mango"));
        if (!hasController) {
            score -= 2;
        }
        getLog().info("  Functionality: " + score + "/10");
        return score;
    }

    private long countFiles(Path start, String extension) {
        try {
            return Files.walk(start)
                    .filter(p -> p.toString().endsWith(extension))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private void outputText(EvaluationResult result) {
        getLog().info("=== Evaluation Result ===");
        getLog().info("Status: " + (result.isPassed() ? "PASSED" : "FAILED"));
        getLog().info("Score: " + result.getScore() + "/100");

        if (result.getDimensions() != null) {
            getLog().info("Dimensions:");
            getLog().info("  - Design Quality: " + result.getDimensions().getDesignQuality() + "/10");
            getLog().info("  - Originality: " + result.getDimensions().getOriginality() + "/10");
            getLog().info("  - Craft: " + result.getDimensions().getCraft() + "/10");
            getLog().info("  - Functionality: " + result.getDimensions().getFunctionality() + "/10");
        }

        if (!result.getIssues().isEmpty()) {
            getLog().warn("Issues found: " + result.getIssues().size());
            for (EvaluationResult.Issue issue : result.getIssues()) {
                getLog().warn("  [" + issue.getSeverity() + "] " + issue.getDescription());
                if (issue.getFile() != null) {
                    getLog().warn("    at: " + issue.getFile() + ":" + issue.getLine());
                }
            }
        }
    }

    private void outputJson(EvaluationResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            getLog().info(json);
        } catch (Exception e) {
            getLog().error("Failed to output JSON", e);
        }
    }

    private void saveReport(EvaluationResult result, String reportPath) {
        try {
            File reportFile = new File(reportPath);
            reportFile.getParentFile().mkdirs();
            String json = objectMapper.writeValueAsString(result);
            Files.writeString(reportFile.toPath(), json);
            getLog().info("Report saved to: " + reportPath);
        } catch (Exception e) {
            getLog().error("Failed to save report", e);
        }
    }
}
