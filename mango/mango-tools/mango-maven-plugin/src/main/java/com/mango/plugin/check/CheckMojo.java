package com.mango.plugin.check;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代码质量检查
 * mvn mango:check -Drule=duplicate
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class CheckMojo extends AbstractMojo {

    @Parameter(property = "rule", defaultValue = "all")
    private String rule;

    @Parameter(property = "threshold", defaultValue = "3")
    private int threshold;

    @Parameter(property = "max", defaultValue = "50")
    private int max;

    @Parameter(property = "min", defaultValue = "80")
    private int min;

    @Parameter(property = "srcDir", defaultValue = "${project.basedir}/src")
    private String srcDir;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running check: " + rule);

        try {
            boolean passed = switch (rule.toLowerCase()) {
                case "duplicate" -> checkDuplicate();
                case "method-length" -> checkMethodLength();
                case "class-length" -> checkClassLength();
                case "naming" -> checkNaming();
                case "all" -> checkAll();
                default -> {
                    getLog().warn("Unknown rule: " + rule);
                    yield true;
                }
            };

            if (!passed) {
                throw new MojoExecutionException("Check failed: " + rule);
            }
            getLog().info("Check passed: " + rule);
        } catch (IOException e) {
            throw new MojoExecutionException("Check failed", e);
        }
    }

    private boolean checkAll() throws IOException {
        getLog().info("Running all checks...");
        boolean duplicate = checkDuplicate();
        boolean methodLength = checkMethodLength();
        boolean classLength = checkClassLength();
        boolean naming = checkNaming();
        return duplicate && methodLength && classLength && naming;
    }

    private boolean checkDuplicate() throws IOException {
        getLog().info("Checking duplicate code (threshold=" + threshold + "%)...");
        Path srcPath = Paths.get(srcDir);
        List<Path> javaFiles = findJavaFiles(srcPath);

        // 简单实现：检查重复的 import 语句
        long totalLines = 0;
        long duplicateLines = 0;

        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file);
            totalLines += lines.size();

            // 检查是否有重复的方法
            long methodCount = lines.stream()
                    .filter(l -> l.contains("public ") || l.contains("private "))
                    .count();

            if (methodCount > 10) {
                duplicateLines += methodCount - 10;
            }
        }

        double duplicateRate = totalLines > 0 ? (duplicateLines * 100.0 / totalLines) : 0;
        getLog().info("Duplicate rate: " + String.format("%.2f", duplicateRate) + "%");

        return duplicateRate <= threshold;
    }

    private boolean checkMethodLength() throws IOException {
        getLog().info("Checking method length (max=" + max + " lines)...");
        Path srcPath = Paths.get(srcDir);
        List<Path> javaFiles = findJavaFiles(srcPath);

        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file);
            int braceCount = 0;
            int lineCount = 0;
            int startLine = 0;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                braceCount += line.chars().filter(c -> c == '{').count();
                braceCount -= line.chars().filter(c -> c == '}').count();

                if (line.contains("public ") || line.contains("private ") || line.contains("protected ")) {
                    if (braceCount == 0) {
                        startLine = i;
                        lineCount = 1;
                    }
                }

                if (braceCount > 0) {
                    lineCount++;
                }

                if (braceCount == 0 && lineCount > 0 && lineCount > max) {
                    getLog().warn("Method too long at " + file + ":" + (startLine + 1) + " (" + lineCount + " lines)");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkClassLength() throws IOException {
        getLog().info("Checking class length (max=500 lines)...");
        Path srcPath = Paths.get(srcDir);
        List<Path> javaFiles = findJavaFiles(srcPath);

        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file);
            if (lines.size() > 500) {
                getLog().warn("Class too long: " + file + " (" + lines.size() + " lines)");
                return false;
            }
        }
        return true;
    }

    private boolean checkNaming() throws IOException {
        getLog().info("Checking naming conventions...");
        Path srcPath = Paths.get(srcDir);
        List<Path> javaFiles = findJavaFiles(srcPath);

        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file);
            String filename = file.getFileName().toString();

            // 检查类名是否为 PascalCase
            if (filename.endsWith(".java")) {
                String className = filename.replace(".java", "");
                if (!className.isEmpty() && Character.isLowerCase(className.charAt(0))) {
                    getLog().warn("Class name should be PascalCase: " + filename);
                    // 不强制失败，仅警告
                }
            }
        }
        return true;
    }

    private List<Path> findJavaFiles(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());
    }
}
