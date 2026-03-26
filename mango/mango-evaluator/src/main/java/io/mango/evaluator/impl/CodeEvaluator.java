package io.mango.evaluator.impl;

import io.mango.evaluator.ArtifactEvaluator;
import io.mango.evaluator.EvaluationReport;
import io.mango.evaluator.EvaluationReport.Dimensions;
import io.mango.evaluator.EvaluationReport.Issue;
import io.mango.evaluator.EvaluationReport.IssueType;
import io.mango.evaluator.EvaluationReport.Severity;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 代码评估器
 *
 * 评估维度:
 * - Design Quality (设计质量): 模块结构、类设计
 * - Originality (创新性): 代码复用、设计模式
 * - Craft (工艺): 命名规范、重复度
 * - Functionality (功能性): 测试覆盖、边界处理
 *
 * @author Mango
 */
public class CodeEvaluator implements ArtifactEvaluator {

    // 最低阈值
    private static final int MIN_DESIGN = 7;
    private static final int MIN_ORIG = 6;
    private static final int MIN_CRAFT = 7;
    private static final int MIN_FUNC = 8;

    @Override
    public EvaluationReport evaluate(String path) {
        List<Issue> issues = new ArrayList<>();
        int designScore = 10, origScore = 10, craftScore = 10, funcScore = 10;

        Path srcPath = Paths.get(path);
        if (!Files.exists(srcPath)) {
            issues.add(Issue.builder()
                    .type(IssueType.STRUCTURE)
                    .severity(Severity.BLOCKER)
                    .file(path)
                    .description("Source directory not found: " + path)
                    .rule("module-rules.md")
                    .build());
            return EvaluationReport.fail(
                    Dimensions.builder()
                            .designQuality(0)
                            .originality(0)
                            .craft(0)
                            .functionality(0)
                            .build(),
                    issues,
                    "Source not found"
            );
        }

        // 1. 评估设计质量
        designScore = evaluateDesign(srcPath, issues);

        // 2. 评估创新性
        origScore = evaluateOriginality(srcPath, issues);

        // 3. 评估工艺
        craftScore = evaluateCraft(srcPath, issues);

        // 4. 评估功能性
        funcScore = evaluateFunctionality(srcPath, issues);

        Dimensions dimensions = Dimensions.builder()
                .designQuality(designScore)
                .originality(origScore)
                .craft(craftScore)
                .functionality(funcScore)
                .build();

        boolean passed = dimensions.meetsMinimum(MIN_DESIGN, MIN_ORIG, MIN_CRAFT, MIN_FUNC);

        return EvaluationReport.builder()
                .passed(passed)
                .score(dimensions.calculateTotalScore())
                .dimensions(dimensions)
                .issues(issues)
                .metadata(EvaluationReport.Metadata.builder()
                        .artifactType("code")
                        .artifactPath(path)
                        .timestamp(System.currentTimeMillis())
                        .ruleVersion("1.0.0")
                        .evaluator("Mango CodeEvaluator")
                        .build())
                .build();
    }

    private int evaluateDesign(Path srcPath, List<Issue> issues) {
        int score = 10;

        // 检查模块结构
        boolean hasModules = false;
        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.toString().contains("-api")
                            || dir.toString().contains("-core")
                            || dir.toString().contains("-starter")) {
                        hasModules = true;
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // ignore
        }

        if (!hasModules) {
            score -= 2;
            issues.add(Issue.builder()
                    .type(IssueType.STRUCTURE)
                    .severity(Severity.MAJOR)
                    .description("Module structure not found (expected: -api, -core, -starter)")
                    .rule("module-rules.md")
                    .build());
        }

        // 检查 core 层依赖
        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith("-core/pom.xml")) {
                        try {
                            String content = Files.readString(file);
                            if (content.contains("-starter")) {
                                issues.add(Issue.builder()
                                        .type(IssueType.STRUCTURE)
                                        .severity(Severity.CRITICAL)
                                        .file(file.toString())
                                        .description("Core module should not depend on starter")
                                        .rule("module-rules.md")
                                        .suggestion("Move starter dependencies to the app module")
                                        .build());
                                score -= 3;
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // ignore
        }

        return Math.max(0, score);
    }

    private int evaluateOriginality(Path srcPath, List<Issue> issues) {
        int score = 7; // 默认分数

        // 统计代码行数和文件数
        final int[] lines = {0};
        final int[] files = {0};

        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        files[0]++;
                        try {
                            lines[0] += Files.readString(file).split("\n").length;
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // ignore
        }

        // 简单评估：代码量适中给基础分，太少或太多都扣分
        if (files[0] < 5) {
            score -= 2;
        } else if (files[0] > 100) {
            score -= 1;
        }

        return Math.max(0, Math.min(10, score));
    }

    private int evaluateCraft(Path srcPath, List<Issue> issues) {
        int score = 10;

        // 检查重复代码
        Map<String, Set<String>> methodSignatures = new HashMap<>();
        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        try {
                            String content = Files.readString(file);
                            Pattern pattern = Pattern.compile("(public|private|protected)\\s+\\w+\\s+\\w+\\s*\\([^)]*\\)");
                            var matcher = pattern.matcher(content);
                            while (matcher.find()) {
                                String sig = matcher.group();
                                methodSignatures.computeIfAbsent(sig, k -> new HashSet<>()).add(file.toString());
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // ignore
        }

        // 找出重复方法
        for (var entry : methodSignatures.entrySet()) {
            if (entry.getValue().size() > 1) {
                issues.add(Issue.builder()
                        .type(IssueType.DUPLICATE)
                        .severity(Severity.MAJOR)
                        .description("Duplicate method signature found in " + entry.getValue().size() + " files: " + entry.getKey())
                        .rule("code-rules.md")
                        .suggestion("Extract to shared utility class")
                        .build());
                score -= 1;
            }
        }

        // 检查命名规范
        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        try {
                            String content = Files.readString(file);
                            String fileName = file.getFileName().toString().replace(".java", "");

                            // 检查类名是否与文件名匹配
                            if (!content.contains("class " + fileName) && !content.contains("interface " + fileName)) {
                                issues.add(Issue.builder()
                                        .type(IssueType.NAMING)
                                        .severity(Severity.MINOR)
                                        .file(file.toString())
                                        .description("Class name does not match file name")
                                        .rule("naming-rules.md")
                                        .build());
                                score -= 1;
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // ignore
        }

        return Math.max(0, score);
    }

    private int evaluateFunctionality(Path srcPath, List<Issue> issues) {
        int score = 10;

        // 统计测试覆盖率
        final int[] javaFiles = {0};
        final int[] testFiles = {0};

        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String path = file.toString();
                    if (path.endsWith(".java")) {
                        if (path.contains("Test.java") || path.contains("/test/")) {
                            testFiles[0]++;
                        } else {
                            javaFiles[0]++;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // ignore
        }

        if (javaFiles[0] > 0) {
            double coverage = (double) testFiles[0] / javaFiles[0];
            if (coverage < 0.3) {
                issues.add(Issue.builder()
                        .type(IssueType.COVERAGE)
                        .severity(Severity.MAJOR)
                        .description("Test coverage too low: " + String.format("%.1f%%", coverage * 100) + " (expected: >30%)")
                        .rule("test-rules.md")
                        .suggestion("Add unit tests for core functionality")
                        .build());
                score -= 3;
            }
        }

        return Math.max(0, score);
    }

    @Override
    public String getName() {
        return "CodeEvaluator";
    }

    @Override
    public String getSupportedArtifactType() {
        return "code";
    }
}
