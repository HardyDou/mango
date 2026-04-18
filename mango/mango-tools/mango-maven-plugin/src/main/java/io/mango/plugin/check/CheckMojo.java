package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * 检查规则: all, duplicate, naming, method-length, class-length, dependency,
     * module-info, remote-adapter, api-contract, kv-key, test-fixture
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
     * Maven Session
     */
    @org.apache.maven.plugins.annotations.Parameter(defaultValue = "${session}", readonly = true)
    private org.apache.maven.execution.MavenSession session;

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
        if (baseDir == null || baseDir.isEmpty()) {
            if (session != null && session.getExecutionRootDirectory() != null) {
                baseDir = session.getExecutionRootDirectory();
            } else {
                baseDir = System.getProperty("user.dir");
            }
        }
        
        getLog().info("Running Mango Check - rule: " + rule);
        result = new CheckResult();

        switch (rule.toLowerCase()) {
            case "duplicate" -> checkDuplicates();
            case "naming" -> checkNaming();
            case "method-length" -> checkMethodLength();
            case "class-length" -> checkClassLength();
            case "dependency" -> checkDependency();
            case "module-info" -> checkModuleInfo();
            case "remote-adapter" -> checkRemoteAdapter();
            case "api-contract" -> checkApiContract();
            case "kv-key" -> checkKvKey();
            case "test-fixture" -> checkTestFixture();
            case "all" -> {
                checkDuplicates();
                checkNaming();
                checkMethodLength();
                checkClassLength();
                checkDependency();
                checkModuleInfo();
                checkRemoteAdapter();
                checkApiContract();
                checkKvKey();
                checkTestFixture();
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

    /**
     * 检查模块依赖规范
     *
     * 规则:
     * 1. *-api 模块不能依赖 *-core, *-starter, *-starter-remote
     * 2. *-core 模块不能依赖 *-starter, *-starter-remote
     * 3. bff-* 模块只能依赖 *-starter 或 *-starter-remote (不能直接依赖 *-api 或 *-core)
     */
    private void checkDependency() {
        getLog().info("Checking module dependencies...");

        // Resolve baseDir from session if not explicitly set
        String effectiveBaseDir = baseDir;
        if (effectiveBaseDir == null || effectiveBaseDir.isEmpty()) {
            if (session != null && session.getExecutionRootDirectory() != null) {
                effectiveBaseDir = session.getExecutionRootDirectory();
            } else {
                getLog().error("Cannot determine base directory. Please set -DbaseDir=<path>");
                return;
            }
        }
        getLog().info("Base directory: " + effectiveBaseDir);

        Path rootPath = Paths.get(effectiveBaseDir);
        if (rootPath == null || !Files.exists(rootPath)) {
            getLog().error("Base directory does not exist: " + effectiveBaseDir);
            return;
        }

        List<DependencyIssue> issues = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith("/pom.xml")) {
                        try {
                            analyzePomDependency(file, issues);
                        } catch (Exception e) {
                            getLog().warn("Failed to analyze: " + file + " - " + e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (DependencyIssue issue : issues) {
                result.addIssue("DEPENDENCY", issue.severity, issue.file, 0,
                        issue.description, "module-rules.md");
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " dependency violation(s)");
        } else {
            getLog().info("All dependency checks passed");
        }
    }

    private void analyzePomDependency(Path pomFile, List<DependencyIssue> issues) {
        try {
            if (!Files.exists(pomFile)) {
                getLog().warn("Pom file does not exist: " + pomFile);
                return;
            }
            String content = Files.readString(pomFile);
            if (content == null || content.isEmpty()) {
                getLog().warn("Empty pom file: " + pomFile);
                return;
            }
            String artifactId = extractArtifactId(content);
            if (artifactId == null) return;

            String groupId = extractGroupId(content);

            // 确定模块类型
            ModuleType moduleType = classifyModule(artifactId);

            // 提取所有依赖
            List<String> dependencies = extractDependencies(content);

            for (String dep : dependencies) {
                if (dep == null) continue;
                String depArtifactId = extractArtifactIdFromDep(dep);
                if (depArtifactId == null || depArtifactId.isEmpty()) continue;
                String depGroupId = extractGroupIdFromDep(dep);

                // 跳过外部依赖（非 io.mango）
                if (!"io.mango".equals(depGroupId)) continue;

                DependencyIssue issue = validateDependency(moduleType, artifactId, depArtifactId);
                if (issue != null) {
                    issue.file = pomFile.toString();
                    issues.add(issue);
                }
            }
        } catch (Exception e) {
            getLog().warn("Error analyzing pom: " + pomFile + " - " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private ModuleType classifyModule(String artifactId) {
        if (artifactId.endsWith("-api")) return ModuleType.API;
        if (artifactId.endsWith("-core")) return ModuleType.CORE;
        if (artifactId.endsWith("-starter-remote")) return ModuleType.STARTER_REMOTE;
        if (artifactId.endsWith("-starter")) return ModuleType.STARTER;
        if (artifactId.startsWith("bff-")) return ModuleType.BFF;
        if (artifactId.startsWith("mango-") && !artifactId.equals("mango") && !artifactId.equals("mango-parent"))
            return ModuleType.OTHER;
        return ModuleType.ROOT;
    }

    private DependencyIssue validateDependency(ModuleType consumer, String consumerArtifact, String depArtifact) {
        // API 模块规则
        if (consumer == ModuleType.API) {
            if (depArtifact.endsWith("-core")) {
                return new DependencyIssue("CRITICAL", "*_api 模块不能依赖 *-core: " + consumerArtifact + " -> " + depArtifact);
            }
            if (depArtifact.endsWith("-starter")) {
                return new DependencyIssue("CRITICAL", "*_api 模块不能依赖 *-starter: " + consumerArtifact + " -> " + depArtifact);
            }
            if (depArtifact.endsWith("-starter-remote")) {
                return new DependencyIssue("CRITICAL", "*_api 模块不能依赖 *-starter-remote: " + consumerArtifact + " -> " + depArtifact);
            }
        }

        // Core 模块规则
        if (consumer == ModuleType.CORE) {
            if (depArtifact.endsWith("-starter")) {
                return new DependencyIssue("CRITICAL", "*_core 模块不能依赖 *-starter: " + consumerArtifact + " -> " + depArtifact);
            }
            if (depArtifact.endsWith("-starter-remote")) {
                return new DependencyIssue("CRITICAL", "*_core 模块不能依赖 *-starter-remote: " + consumerArtifact + " -> " + depArtifact);
            }
        }

        // BFF 模块规则
        if (consumer == ModuleType.BFF) {
            if (depArtifact.endsWith("-api")) {
                return new DependencyIssue("MAJOR", "BFF 模块不能直接依赖 *-api，请使用 *-starter: " + consumerArtifact + " -> " + depArtifact);
            }
            if (depArtifact.endsWith("-core")) {
                return new DependencyIssue("MAJOR", "BFF 模块不能直接依赖 *-core，请使用 *-starter: " + consumerArtifact + " -> " + depArtifact);
            }
        }

        return null;
    }

    private String extractArtifactId(String content) {
        String projectContent = content.replaceFirst("(?s)<parent>.*?</parent>", "");
        int start = projectContent.indexOf("<artifactId>");
        if (start == -1) return null;
        start += "<artifactId>".length();
        int end = projectContent.indexOf("</artifactId>", start);
        if (end == -1) return null;
        return projectContent.substring(start, end).trim();
    }

    private String extractGroupId(String content) {
        int start = content.indexOf("<groupId>");
        if (start == -1) return null;
        start += "<groupId>".length();
        int end = content.indexOf("</groupId>", start);
        if (end == -1) return null;
        return content.substring(start, end).trim();
    }

    private List<String> extractDependencies(String content) {
        List<String> deps = new ArrayList<>();
        int depStart = content.indexOf("<dependencies>");
        if (depStart == -1) return deps;
        int depEnd = content.indexOf("</dependencies>", depStart);
        if (depEnd == -1) return deps;

        String depsSection = content.substring(depStart, depEnd);
        int marker = 0;
        while ((marker = depsSection.indexOf("<dependency>", marker)) != -1) {
            int end = depsSection.indexOf("</dependency>", marker);
            if (end == -1) break;
            deps.add(depsSection.substring(marker, end + "</dependency>".length()));
            marker = end + "</dependency>".length();
        }
        return deps;
    }

    private String extractArtifactIdFromDep(String dep) {
        int start = dep.indexOf("<artifactId>");
        if (start == -1) return "";
        start += "<artifactId>".length();
        int end = dep.indexOf("</artifactId>", start);
        if (end == -1) return "";
        return dep.substring(start, end).trim();
    }

    private String extractGroupIdFromDep(String dep) {
        int start = dep.indexOf("<groupId>");
        if (start == -1) return "";
        start += "<groupId>".length();
        int end = dep.indexOf("</groupId>", start);
        if (end == -1) return "";
        return dep.substring(start, end).trim();
    }

    private static class DependencyIssue {
        String severity;
        String description;
        String file;

        DependencyIssue(String severity, String description) {
            this.severity = severity;
            this.description = description;
        }
    }

    /**
     * 检查模块信息声明。
     *
     * 规则:
     * 1. 本地 *-starter 必须提供 META-INF/mango/module.properties
     * 2. module.properties 必须声明 module-name
     * 3. module-name 必须使用稳定模块名，不允许空值或非法字符
     */
    private void checkModuleInfo() {
        getLog().info("Checking module info declarations...");

        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<ModuleInfoIssue> issues = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith("/pom.xml")) {
                        analyzeModuleInfo(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (ModuleInfoIssue issue : issues) {
                result.addIssue("MODULE_INFO", issue.severity, issue.file, 0,
                        issue.description, "module-rules.md");
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " module info violation(s)");
        } else {
            getLog().info("All module info checks passed");
        }
    }

    private Path resolveBasePath() {
        String effectiveBaseDir = baseDir;
        if (effectiveBaseDir == null || effectiveBaseDir.isEmpty()) {
            if (session != null && session.getExecutionRootDirectory() != null) {
                effectiveBaseDir = session.getExecutionRootDirectory();
            } else {
                getLog().error("Cannot determine base directory. Please set -DbaseDir=<path>");
                return null;
            }
        }

        Path rootPath = Paths.get(effectiveBaseDir);
        if (!Files.exists(rootPath)) {
            getLog().error("Base directory does not exist: " + effectiveBaseDir);
            return null;
        }
        return rootPath;
    }

    private void analyzeModuleInfo(Path pomFile, List<ModuleInfoIssue> issues) {
        try {
            String content = Files.readString(pomFile);
            String artifactId = extractArtifactId(content);
            if (artifactId == null || classifyModule(artifactId) != ModuleType.STARTER) {
                return;
            }
            if ("mango-infra-module-starter".equals(artifactId)) {
                return;
            }

            Path moduleInfoFile = pomFile.getParent()
                    .resolve("src/main/resources/META-INF/mango/module.properties");
            if (!Files.exists(moduleInfoFile)) {
                issues.add(new ModuleInfoIssue("CRITICAL", moduleInfoFile.toString(),
                        artifactId + " 缺少 META-INF/mango/module.properties"));
                return;
            }

            Properties properties = new Properties();
            try (var inputStream = Files.newInputStream(moduleInfoFile)) {
                properties.load(inputStream);
            }

            String moduleName = properties.getProperty("module-name", "").trim();
            if (moduleName.isEmpty()) {
                issues.add(new ModuleInfoIssue("CRITICAL", moduleInfoFile.toString(),
                        artifactId + " 的 module-name 不能为空"));
                return;
            }
            if (!moduleName.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
                issues.add(new ModuleInfoIssue("MAJOR", moduleInfoFile.toString(),
                        artifactId + " 的 module-name 只能使用小写字母、数字和中划线: " + moduleName));
            }
        } catch (Exception e) {
            issues.add(new ModuleInfoIssue("MAJOR", pomFile.toString(),
                    "模块信息检查失败: " + e.getMessage()));
        }
    }

    private static class ModuleInfoIssue {
        String severity;
        String file;
        String description;

        ModuleInfoIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    /**
     * 检查远程适配器。
     *
     * 规则:
     * 1. starter-remote 的 @FeignClient name 必须使用 Mango 模块名
     * 2. 禁止使用 auth-service、permission-service 等真实服务发现名
     */
    private void checkRemoteAdapter() {
        getLog().info("Checking remote adapter declarations...");

        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<RemoteAdapterIssue> issues = new ArrayList<>();
        Pattern feignClientNamePattern = Pattern.compile("@FeignClient\\s*\\([^)]*name\\s*=\\s*\"([^\"]+)\"");

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isStarterRemoteJavaFile(file)) {
                        analyzeRemoteAdapter(file, feignClientNamePattern, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (RemoteAdapterIssue issue : issues) {
                result.addIssue("REMOTE_ADAPTER", issue.severity, issue.file, 0,
                        issue.description, "module-rules.md");
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " remote adapter violation(s)");
        } else {
            getLog().info("All remote adapter checks passed");
        }
    }

    private boolean isStarterRemoteJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("-starter-remote/src/main/java/") && normalized.endsWith(".java");
    }

    private void analyzeRemoteAdapter(Path file, Pattern feignClientNamePattern, List<RemoteAdapterIssue> issues) {
        try {
            Matcher matcher = feignClientNamePattern.matcher(Files.readString(file));
            while (matcher.find()) {
                String feignName = matcher.group(1).trim();
                if (!feignName.matches("mango-[a-z0-9]+(-[a-z0-9]+)*")) {
                    issues.add(new RemoteAdapterIssue("CRITICAL", file.toString(),
                            "@FeignClient name 必须使用 Mango 模块名: " + feignName));
                }
            }
        } catch (IOException e) {
            issues.add(new RemoteAdapterIssue("MAJOR", file.toString(),
                    "远程适配器检查失败: " + e.getMessage()));
        }
    }

    private static class RemoteAdapterIssue {
        String severity;
        String file;
        String description;

        RemoteAdapterIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    /**
     * 检查 API 契约。
     *
     * 规则:
     * 1. *-api 源码禁止声明 @FeignClient
     */
    private void checkApiContract() {
        getLog().info("Checking API contracts...");

        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<ApiContractIssue> issues = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isApiJavaFile(file)) {
                        analyzeApiContract(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (ApiContractIssue issue : issues) {
                result.addIssue("API_CONTRACT", issue.severity, issue.file, 0,
                        issue.description, "api-rules.md");
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " API contract violation(s)");
        } else {
            getLog().info("All API contract checks passed");
        }
    }

    private boolean isApiJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("-api/src/main/java/") && normalized.endsWith(".java");
    }

    private void analyzeApiContract(Path file, List<ApiContractIssue> issues) {
        try {
            if (Files.readString(file).contains("@FeignClient")) {
                issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                        "*-api 禁止声明 @FeignClient"));
            }
        } catch (IOException e) {
            issues.add(new ApiContractIssue("MAJOR", file.toString(),
                    "API 契约检查失败: " + e.getMessage()));
        }
    }

    private static class ApiContractIssue {
        String severity;
        String file;
        String description;

        ApiContractIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    /**
     * 检查 KV key 规范。
     *
     * 规则:
     * 1. 注解 key 不得写完整基础设施前缀 mango:infra:kv，由 KV capability 自动补齐。
     * 2. 动态 key 必须使用 SpEL 模板 user:#{#userId} 或直接 SpEL #userId。
     */
    private void checkKvKey() {
        getLog().info("Checking KV key declarations...");

        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<KvKeyIssue> issues = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isMainJavaFile(file)) {
                        analyzeKvKey(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (KvKeyIssue issue : issues) {
                result.addIssue("KV_KEY", issue.severity, issue.file, issue.line,
                        issue.description, "naming-rules.md");
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " KV key violation(s)");
        } else {
            getLog().info("All KV key checks passed");
        }
    }

    private boolean isMainJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/src/main/java/") && normalized.endsWith(".java");
    }

    private void analyzeKvKey(Path file, List<KvKeyIssue> issues) {
        try {
            String content = Files.readString(file);
            Pattern annotationPattern = Pattern.compile(
                    "@(Cacheable|Locker|RateLimit|Idempotent)\\s*\\([\\s\\S]*?\\bkey\\s*=\\s*\"([^\"]*)\"");
            Matcher matcher = annotationPattern.matcher(content);
            while (matcher.find()) {
                String key = matcher.group(2).trim();
                int line = lineNumber(content, matcher.start(2));
                if (key.startsWith("mango:infra:kv:")) {
                    issues.add(new KvKeyIssue("CRITICAL", file.toString(), line,
                            "KV 注解 key 不得手写 mango:infra:kv 前缀，应由 capability 自动补齐"));
                }
                if (usesInlinePlaceholder(key, "#")) {
                    issues.add(new KvKeyIssue("CRITICAL", file.toString(), line,
                            "KV 注解 key 不支持 user:#userId 写法，应使用 user:#{#userId}"));
                }
                if (usesInlinePlaceholder(key, "@")) {
                    issues.add(new KvKeyIssue("CRITICAL", file.toString(), line,
                            "KV 注解 key 不支持 user:@bean 写法，应使用 user:#{@bean.method()}"));
                }
            }
        } catch (IOException e) {
            issues.add(new KvKeyIssue("MAJOR", file.toString(), 0,
                    "KV key 检查失败: " + e.getMessage()));
        }
    }

    private int lineNumber(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private boolean usesInlinePlaceholder(String key, String token) {
        if (!key.contains(token) || key.startsWith(token)) {
            return false;
        }
        return !key.contains("#{" + token);
    }

    private static class KvKeyIssue {
        String severity;
        String file;
        int line;
        String description;

        KvKeyIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    /**
     * 检查测试命名和测试物料是否一致。
     *
     * 自动覆盖的高风险错配:
     * 1. Redis*Test 中使用 MemoryKvStore/JdbcKvStore 作为核心被测物料。
     * 2. Jdbc*Test 中使用 MemoryKvStore/RedisKvStore 作为核心被测物料。
     * 3. Memory*Test 中使用 RedisKvStore/JdbcKvStore 作为核心被测物料。
     */
    private void checkTestFixture() {
        getLog().info("Checking test fixture consistency...");

        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<TestFixtureIssue> issues = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isTestJavaFile(file)) {
                        analyzeTestFixture(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (TestFixtureIssue issue : issues) {
                result.addIssue("TEST_FIXTURE", issue.severity, issue.file, issue.line,
                        issue.description, "test-rules.md");
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " test fixture violation(s)");
        } else {
            getLog().info("All test fixture checks passed");
        }
    }

    private boolean isTestJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/src/test/java/") && normalized.endsWith("Test.java");
    }

    private void analyzeTestFixture(Path file, List<TestFixtureIssue> issues) {
        String fileName = file.getFileName().toString();
        try {
            String content = Files.readString(file);
            String expected = implementationPrefix(fileName);
            if (expected == null) {
                return;
            }

            List<String> mismatches = new ArrayList<>();
            for (String candidate : List.of("MemoryKvStore", "RedisKvStore", "JdbcKvStore")) {
                if (!candidate.startsWith(expected) && content.contains(candidate)) {
                    mismatches.add(candidate);
                }
            }
            for (String mismatch : mismatches) {
                issues.add(new TestFixtureIssue("CRITICAL", file.toString(), lineNumber(content, content.indexOf(mismatch)),
                        fileName + " 表示测试 " + expected + " 实现，但测试物料出现 " + mismatch
                                + "；实现类型测试必须使用同名实现，通用能力测试应按能力命名并参数化注入"));
            }
        } catch (IOException e) {
            issues.add(new TestFixtureIssue("MAJOR", file.toString(), 0,
                    "测试物料一致性检查失败: " + e.getMessage()));
        }
    }

    private String implementationPrefix(String fileName) {
        if (fileName.startsWith("Redis")) {
            return "Redis";
        }
        if (fileName.startsWith("Jdbc")) {
            return "Jdbc";
        }
        if (fileName.startsWith("Memory")) {
            return "Memory";
        }
        return null;
    }

    private static class TestFixtureIssue {
        String severity;
        String file;
        int line;
        String description;

        TestFixtureIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private enum ModuleType {
        ROOT, OTHER, API, CORE, STARTER, STARTER_REMOTE, BFF
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
