package com.mango.plugin.eval;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 质检评估 - PRD/代码/测试
 * mvn mango:evaluate -Dartifact=prd
 */
@Mojo(name = "evaluate", defaultPhase = LifecyclePhase.VERIFY)
public class EvaluateMojo extends AbstractMojo {

    @Parameter(property = "artifact", required = true)
    private String artifact;

    @Parameter(property = "rulesDir", defaultValue = "${project.basedir}/../rules")
    private String rulesDir;

    @Parameter(property = "artifactDir", defaultValue = "${project.basedir}")
    private String artifactDir;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Evaluating artifact: " + artifact);

        try {
            boolean passed = switch (artifact.toLowerCase()) {
                case "prd" -> evaluatePrd();
                case "code" -> evaluateCode();
                case "test" -> evaluateTest();
                default -> {
                    getLog().warn("Unknown artifact type: " + artifact);
                    yield false;
                }
            };

            if (passed) {
                getLog().info("Evaluation passed: " + artifact);
            } else {
                getLog().error("Evaluation failed: " + artifact);
                throw new MojoExecutionException("Evaluation failed: " + artifact);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Evaluation failed", e);
        }
    }

    private boolean evaluatePrd() throws IOException {
        getLog().info("Evaluating PRD...");

        Path prdPath = Paths.get(artifactDir, "PRD.md");
        if (!Files.exists(prdPath)) {
            getLog().warn("PRD.md not found at " + prdPath);
            return false;
        }

        String content = Files.readString(prdPath);

        // 检查必需章节
        String[] requiredSections = {
                "用户故事", "功能描述", "字段设计", "API设计",
                "数据库设计", "UI/UX", "业务流程", "边界情况"
        };

        int found = 0;
        for (String section : requiredSections) {
            if (content.contains(section)) {
                found++;
            } else {
                getLog().warn("Missing PRD section: " + section);
            }
        }

        double completeness = (found * 100.0) / requiredSections.length;
        getLog().info("PRD completeness: " + String.format("%.0f", completeness) + "%");

        return completeness >= 80;
    }

    private boolean evaluateCode() throws IOException {
        getLog().info("Evaluating code...");

        Path srcPath = Paths.get(artifactDir, "src");
        if (!Files.exists(srcPath)) {
            getLog().warn("src directory not found");
            return false;
        }

        List<Path> javaFiles = Files.walk(srcPath)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        if (javaFiles.isEmpty()) {
            getLog().warn("No Java files found");
            return false;
        }

        getLog().info("Found " + javaFiles.size() + " Java files");

        // 检查代码规范
        int passed = 0;
        for (Path file : javaFiles) {
            String content = Files.readString(file);

            // 简单检查
            if (!content.contains("硬编码")) passed++;
            if (content.contains("Serializable") || file.getFileName().toString().contains("DTO")) passed++;
        }

        return passed > 0;
    }

    private boolean evaluateTest() throws IOException {
        getLog().info("Evaluating test coverage...");

        Path testPath = Paths.get(artifactDir, "src/test");
        if (!Files.exists(testPath)) {
            getLog().warn("src/test directory not found");
            return false;
        }

        List<Path> testFiles = Files.walk(testPath)
                .filter(p -> p.toString().endsWith("Test.java"))
                .collect(Collectors.toList());

        List<Path> srcFiles = Files.walk(Paths.get(artifactDir, "src/main/java"))
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        if (srcFiles.isEmpty()) {
            return false;
        }

        double coverage = (testFiles.size() * 100.0) / srcFiles.size();
        getLog().info("Test coverage estimate: " + String.format("%.0f", coverage) + "%");

        return coverage >= 50;  // 简单检查
    }
}
