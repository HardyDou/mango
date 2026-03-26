package io.mango.plugin.eval;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 质检评估 Mojo
 *
 * mvn mango:evaluate -Dartifact=prd
 * mvn mango:evaluate -Dartifact=code
 *
 * @author Mango
 */
@Mojo(name = "evaluate", defaultPhase = LifecyclePhase.VERIFY)
public class EvaluateMojo extends AbstractMojo {

    @Parameter(property = "artifact", required = true)
    private String artifact;

    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Evaluating artifact: " + artifact);

        switch (artifact.toLowerCase()) {
            case "prd" -> evaluatePRD();
            case "code" -> evaluateCode();
            default -> getLog().warn("Unknown artifact type: " + artifact);
        }

        getLog().info("Evaluation completed");
    }

    private void evaluatePRD() {
        getLog().info("=== PRD Quality Evaluation ===");

        Path prdPath = Paths.get(baseDir, "prd");
        if (!Files.exists(prdPath)) {
            getLog().warn("PRD directory not found at: " + prdPath);
            return;
        }

        // 检查 PRD 基本结构
        checkFile("README.md", prdPath);
        checkFile("PRD.md", prdPath);

        getLog().info("PRD evaluation completed");
    }

    private void evaluateCode() {
        getLog().info("=== Code Quality Evaluation ===");

        Path srcPath = Paths.get(baseDir, "src");
        if (!Files.exists(srcPath)) {
            getLog().warn("Source directory not found at: " + srcPath);
            return;
        }

        // 统计代码量
        long javaFiles = countFiles(srcPath, ".java");
        long testFiles = countFiles(srcPath, "Test.java");

        getLog().info("Java files: " + javaFiles);
        getLog().info("Test files: " + testFiles);

        if (javaFiles > 0) {
            double coverage = (double) testFiles / javaFiles * 100;
            getLog().info("Estimated test coverage: " + String.format("%.1f%%", coverage));
        }

        getLog().info("Code evaluation completed");
    }

    private void checkFile(String filename, Path dir) {
        Path file = dir.resolve(filename);
        if (Files.exists(file)) {
            getLog().info("  [OK] " + filename);
        } else {
            getLog().warn("  [MISSING] " + filename);
        }
    }

    private long countFiles(Path start, String extension) {
        try {
            return java.nio.file.Files.walk(start)
                    .filter(p -> p.toString().endsWith(extension))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }
}
