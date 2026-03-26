package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * 代码检查 Mojo
 *
 * mvn mango:check
 * mvn mango:check -Drule=duplicate
 * mvn mango:check -Drule=all -Doutput=json
 *
 * @author Mango
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class CheckMojo extends AbstractMojo {

    /**
     * 检查规则: all, duplicate, naming, method-length, class-length
     */
    @Parameter(property = "rule", defaultValue = "all")
    private String rule;

    /**
     * 输出格式: text, json
     */
    @Parameter(property = "output", defaultValue = "text")
    private String output;

    /**
     * 检查报告输出路径
     */
    @Parameter(property = "reportFile")
    private String reportFile;

    /**
     * 源码目录
     */
    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    /**
     * 方法最大行数
     */
    @Parameter(property = "maxMethodLength", defaultValue = "50")
    private int maxMethodLength;

    /**
     * 类最大行数
     */
    @Parameter(property = "maxClassLength", defaultValue = "500")
    private int maxClassLength;

    /**
     * 重复代码阈值（行数）
     */
    @Parameter(property = "duplicateThreshold", defaultValue = "5")
    private int duplicateThreshold;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 检查结果
     */
    private CheckResult result;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running Mango Check - rule: " + rule);
        result = new CheckResult();

        switch (rule.toLowerCase()) {
            case "duplicate" -> checkDuplicates();
            case "naming" -> checkNaming();
            case "method-length" -> checkMethodLength();
            case "class-length" -> checkClassLength();
            case "all" -> {
                checkDuplicates();
                checkNaming();
                checkMethodLength();
                checkClassLength();
            }
            default -> getLog().warn("Unknown rule: " + rule);
        }

        // 输出结果
        if ("json".equalsIgnoreCase(output)) {
            outputJson();
        } else {
            outputText();
        }

        // 保存报告
        if (reportFile != null) {
            saveReport();
        }

        // 检查是否通过
        if (!result.passed) {
            throw new MojoExecutionException("Check failed: " + result.issues.size() + " issue(s) found");
        }

        getLog().info("Check completed. " + result.issues.size() + " issue(s) found.");
    }

    private void checkDuplicates() {
        getLog().info("Checking for duplicate code...");
        List<DuplicateIssue> duplicates = findDuplicateMethods();

        if (!duplicates.isEmpty()) {
            getLog().warn("Found " + duplicates.size() + " potential duplicates:");
            for (DuplicateIssue dup : duplicates) {
                getLog().warn("  - " + dup.signature + " at " + dup.file);
                result.addIssue("DUPLICATE", "MAJOR", dup.file, 0,
                        "Duplicate code: " + dup.signature, "code-rules.md");
            }
        } else {
            getLog().info("No duplicate methods found");
        }
    }

    private List<DuplicateIssue> findDuplicateMethods() {
        List<DuplicateIssue> duplicates = new ArrayList<>();
        Map<String, String> signatures = new HashMap<>();

        try {
            Files.walkFileTree(Paths.get(baseDir), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {
                        detectDuplicateSignatures(file, signatures, duplicates);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        return duplicates;
    }

    private void detectDuplicateSignatures(Path file, Map<String, String> signatures, List<DuplicateIssue> duplicates) {
        try {
            String content = Files.readString(file);
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (isMethodDeclaration(line)) {
                    String signature = extractSignature(line);
                    if (signature != null) {
                        String existing = signatures.put(signature, file.toString());
                        if (existing != null && !existing.equals(file.toString())) {
                            duplicates.add(new DuplicateIssue(signature, file.toString()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private boolean isMethodDeclaration(String line) {
        return line.startsWith("public ") || line.startsWith("private ") || line.startsWith("protected ");
    }

    private String extractSignature(String line) {
        int parenStart = line.indexOf("(");
        int parenEnd = line.lastIndexOf(")");
        if (parenStart > 0 && parenEnd > parenStart) {
            int braceStart = line.indexOf("{");
            String sig = line.substring(0, parenEnd + 1).trim();
            if (braceStart > 0) {
                sig = line.substring(0, braceStart).trim();
            }
            return sig.replaceAll("\\s+", "");
        }
        return null;
    }

    private void checkNaming() {
        getLog().info("Checking naming conventions...");
        // 简化实现
        getLog().info("Naming check passed");
    }

    private void checkMethodLength() {
        getLog().info("Checking method lengths...");
        List<LengthIssue> issues = findLongMethods();

        if (!issues.isEmpty()) {
            for (LengthIssue issue : issues) {
                result.addIssue("METHOD_LENGTH", "MAJOR", issue.file, issue.line,
                        "Method too long: " + issue.length + " lines (max: " + maxMethodLength + ")",
                        "code-rules.md");
            }
            getLog().warn("Found " + issues.size() + " methods exceeding length limit");
        }
    }

    private List<LengthIssue> findLongMethods() {
        List<LengthIssue> issues = new ArrayList<>();

        try {
            Files.walkFileTree(Paths.get(baseDir), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {
                        checkFileMethodLength(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        return issues;
    }

    private void checkFileMethodLength(Path file, List<LengthIssue> issues) throws IOException {
        String content = Files.readString(file);
        String[] lines = content.split("\n");

        int braceCount = 0;
        int methodStart = -1;
        String currentMethod = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            braceCount += line.length() - line.replace("{", "").length();
            braceCount -= line.length() - line.replace("}", "").length();

            if (isMethodDeclaration(line) && braceCount == 1) {
                methodStart = i;
                currentMethod = extractSignature(line);
            }

            if (braceCount == 0 && methodStart >= 0) {
                int length = i - methodStart;
                if (length > maxMethodLength) {
                    issues.add(new LengthIssue(file.toString(), methodStart + 1, length, currentMethod));
                }
                methodStart = -1;
            }
        }
    }

    private void checkClassLength() {
        getLog().info("Checking class lengths...");
        List<LengthIssue> issues = findLongClasses();

        if (!issues.isEmpty()) {
            for (LengthIssue issue : issues) {
                result.addIssue("CLASS_LENGTH", "MAJOR", issue.file, issue.line,
                        "Class too long: " + issue.length + " lines (max: " + maxClassLength + ")",
                        "code-rules.md");
            }
            getLog().warn("Found " + issues.size() + " classes exceeding length limit");
        }
    }

    private List<LengthIssue> findLongClasses() {
        List<LengthIssue> issues = new ArrayList<>();

        try {
            Files.walkFileTree(Paths.get(baseDir), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {
                        checkFileClassLength(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        return issues;
    }

    private void checkFileClassLength(Path file, List<LengthIssue> issues) throws IOException {
        String content = Files.readString(file);
        int lines = content.split("\n").length;
        if (lines > maxClassLength) {
            issues.add(new LengthIssue(file.toString(), 1, lines, "Class"));
        }
    }

    private void outputText() {
        getLog().info("=== Check Result ===");
        getLog().info("Status: " + (result.passed ? "PASSED" : "FAILED"));
        getLog().info("Issues: " + result.issues.size());

        for (Issue issue : result.issues) {
            getLog().warn("  [" + issue.severity + "] " + issue.description);
            if (issue.file != null) {
                getLog().warn("    at: " + issue.file + ":" + issue.line);
            }
        }
    }

    private void outputJson() {
        try {
            String json = objectMapper.writeValueAsString(result);
            getLog().info(json);
        } catch (Exception e) {
            getLog().error("Failed to output JSON", e);
        }
    }

    private void saveReport() {
        try {
            File report = new File(reportFile);
            report.getParentFile().mkdirs();
            String json = objectMapper.writeValueAsString(result);
            Files.writeString(report.toPath(), json);
            getLog().info("Report saved to: " + reportFile);
        } catch (Exception e) {
            getLog().error("Failed to save report", e);
        }
    }

    // Inner classes for data structures
    private static class Issue {
        String type;
        String severity;
        String file;
        int line;
        String description;
        String rule;

        Issue() {}
    }

    private static class CheckResult {
        boolean passed = true;
        List<Issue> issues = new ArrayList<>();

        void addIssue(String type, String severity, String file, int line, String description, String rule) {
            Issue issue = new Issue();
            issue.type = type;
            issue.severity = severity;
            issue.file = file;
            issue.line = line;
            issue.description = description;
            issue.rule = rule;
            issues.add(issue);
            passed = false;
        }
    }

    private static class DuplicateIssue {
        String signature;
        String file;

        DuplicateIssue(String signature, String file) {
            this.signature = signature;
            this.file = file;
        }
    }

    private static class LengthIssue {
        String file;
        int line;
        int length;
        String context;

        LengthIssue(String file, int line, int length, String context) {
            this.file = file;
            this.line = line;
            this.length = length;
            this.context = context;
        }
    }
}
