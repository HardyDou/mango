package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Mango project-specific check Mojo.
 *
 * <p>`mango:check` only owns Mango-specific rules. Generic Java static analysis
 * such as method length, class length, duplicate code, complexity, and common
 * language best practices are delegated to PMD/P3C, Checkstyle, and SpotBugs.</p>
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, aggregator = true)
public class CheckMojo extends AbstractMojo {

    private static final String SOURCE_MANGO_CHECK = "mango-check";
    private static final String SOURCE_PMD = "pmd";
    private static final String SOURCE_CHECKSTYLE = "checkstyle";
    private static final String SOURCE_SPOTBUGS = "spotbugs";
    private static final String DOC_AUTO_CHECK_MAPPING = "auto-check-mapping.md";
    private static final String DOC_NAMING_RULES = "naming-rules.md";
    private static final String DOC_MODULE_RULES = "module-rules.md";
    private static final String DOC_API_RULES = "api-rules.md";
    private static final String DOC_TEST_RULES = "test-rules.md";
    private static final String DOC_PERSISTENCE_RULES = "persistence-rules.md";
    private static final String DOC_STATIC_ANALYSIS = "auto-check-mapping.md";
    private static final String PATH_VARIABLE_ANNOTATION = "@Path" + "Variable";
    private static final String CHECKSTYLE_REPORT = "checkstyle-result.xml";
    private static final String PMD_REPORT = "pmd.xml";
    private static final String SPOTBUGS_REPORT = "spotbugsXml.xml";
    private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern FEIGN_CLIENT_PATTERN = Pattern.compile("@FeignClient\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern REQUEST_MAPPING_PATTERN = Pattern.compile(
            "@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*\\((.*?)\\)",
            Pattern.DOTALL);
    private static final Pattern API_METHOD_PATTERN = Pattern.compile(
            "\\bR\\s*<[^;]+?>\\s+([A-Za-z0-9_]+)\\s*\\(([^;{}]*)\\)\\s*;",
            Pattern.DOTALL);
    private static final Pattern TYPE_DECLARATION_PATTERN = Pattern.compile(
            "\\b(?:public\\s+)?(?:sealed\\s+)?(?:abstract\\s+)?(?:class|interface|record|enum)\\s+([A-Za-z0-9_]+)");
    private static final Pattern API_FIELD_PATTERN = Pattern.compile(
            "\\bprivate\\s+(?:final\\s+)?(?:[A-Za-z0-9_$.]+\\.)?([A-Za-z0-9_]+Api)\\s+[A-Za-z0-9_]+\\s*;");
    private static final Pattern SERVICE_IMPLEMENTS_API_PATTERN = Pattern.compile(
            "\\bclass\\s+([A-Za-z0-9_]+Service)\\b[^\\{;]*\\bimplements\\b[^\\{;]*\\b[A-Za-z0-9_]+Api\\b",
            Pattern.DOTALL);
    private static final Pattern FEIGN_EXTENDS_API_PATTERN = Pattern.compile(
            "\\binterface\\s+([A-Za-z0-9_]+FeignClient)\\b[^\\{;]*\\bextends\\b[^\\{;]*\\b[A-Za-z0-9_]+Api\\b",
            Pattern.DOTALL);
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "(?is)create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?(?:`?([a-zA-Z0-9_]+)`?\\.)?`?([a-zA-Z0-9_]+)`?\\s*\\(");
    private static final List<String> REQUIRED_PERSISTENCE_COLUMNS = List.of(
            "created_by", "created_at", "updated_by", "updated_at", "tenant_id");
    private static final Set<String> DEFAULT_PERSISTENCE_EXCLUDED_TABLES = Set.of(
            "flyway_schema_history", "databasechangelog", "databasechangeloglock",
            "kv_record", "infra_kv_entry", "sys_login_log", "sys_operation_log");

    /**
     * Check rule: all, static, naming, dependency, module-boundary, module-info, remote-adapter,
     * api-contract, path-param, kv-key, test-fixture, persistence-schema.
     */
    @Parameter(property = "rule", defaultValue = "all")
    private String rule;

    /**
     * Output format: text, json.
     */
    @Parameter(property = "output", defaultValue = "text")
    private String output;

    /**
     * Report output path.
     */
    @Parameter(property = "reportFile")
    private String reportFile;

    /**
     * Source root.
     */
    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    /**
     * Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private org.apache.maven.execution.MavenSession session;

    /**
     * Retained for backward-compatible tests and configuration migration.
     * Generic method-length checks are delegated to PMD/P3C/Checkstyle.
     */
    @Parameter(property = "maxMethodLength", defaultValue = "50")
    private int maxMethodLength;

    /**
     * Retained for backward-compatible tests and configuration migration.
     * Generic class-length checks are delegated to PMD/P3C/Checkstyle.
     */
    @Parameter(property = "maxClassLength", defaultValue = "500")
    private int maxClassLength;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

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

        switch (rule.toLowerCase(Locale.ROOT)) {
            case "duplicate", "method-length", "class-length", "complexity" -> unsupportedGenericRule(rule);
            case "static" -> runStaticAnalysis();
            case "naming" -> checkNaming();
            case "dependency", "module-boundary" -> checkDependency();
            case "module-info" -> checkModuleInfo();
            case "remote-adapter" -> checkRemoteAdapter();
            case "api-contract" -> checkApiContract();
            case "path-param" -> checkPathParam();
            case "kv-key" -> checkKvKey();
            case "persistence-schema" -> checkPersistenceSchema();
            case "test-fixture" -> checkTestFixture();
            case "web-boundary" -> checkWebBoundary();
            case "all" -> {
                runStaticAnalysis();
                checkNaming();
                checkDependency();
                checkModuleInfo();
                checkRemoteAdapter();
                checkApiContract();
                checkPathParam();
                checkKvKey();
                checkPersistenceSchema();
                checkTestFixture();
                checkWebBoundary();
            }
            default -> getLog().warn("Unknown rule: " + rule);
        }

        if ("json".equalsIgnoreCase(output)) {
            outputJson();
        } else {
            outputText();
        }

        if (reportFile != null) {
            saveReport();
        }

        if (!result.passed) {
            throw new MojoExecutionException("Check failed: " + result.issues.size() + " issue(s) found");
        }

        getLog().info("Check completed. " + result.issues.size() + " issue(s) found.");
    }

    private void unsupportedGenericRule(String selectedRule) {
        String description = "mango:check only runs Mango-specific rules. Generic rule '" + selectedRule
                + "' is handled by aggregated static analysis inside mango:check; use rule=static or the mapping in "
                + DOC_AUTO_CHECK_MAPPING + ".";
        getLog().warn(description);
        result.addIssue("UNSUPPORTED_RULE", "MAJOR", null, 0, description,
                "UNSUPPORTED_RULE", DOC_AUTO_CHECK_MAPPING, SOURCE_MANGO_CHECK);
    }

    private void runStaticAnalysis() throws MojoExecutionException {
        getLog().info("Running aggregated static analysis...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }
        if (!Files.exists(rootPath.resolve("pom.xml"))) {
            getLog().warn("Skip aggregated static analysis because baseDir has no pom.xml: " + rootPath);
            return;
        }

        cleanStaticReports(rootPath);
        invokeMavenGoals(rootPath, List.of("pmd:check", "checkstyle:check", "spotbugs:spotbugs"));
        collectPmdIssues(rootPath);
        collectCheckstyleIssues(rootPath);
        collectSpotbugsIssues(rootPath);
    }

    private void cleanStaticReports(Path rootPath) throws MojoExecutionException {
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    if (CHECKSTYLE_REPORT.equals(fileName) || PMD_REPORT.equals(fileName) || SPOTBUGS_REPORT.equals(fileName)) {
                        Files.deleteIfExists(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to clean static-analysis reports", e);
        }
    }

    private void invokeMavenGoals(Path rootPath, List<String> goals) throws MojoExecutionException {
        File mavenExecutable = findMavenExecutable();
        if (mavenExecutable == null) {
            getLog().warn("Skip aggregated static analysis because Maven executable was not found");
            return;
        }

        List<String> reactorProjects = discoverReactorProjects(rootPath);
        for (String goal : goals) {
            invokeSingleGoal(mavenExecutable, rootPath, goal, reactorProjects);
        }
    }

    private void invokeSingleGoal(File mavenExecutable, Path rootPath, String goal,
                                  List<String> reactorProjects) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add(mavenExecutable.getAbsolutePath());
        command.add("-q");
        command.add("-f");
        command.add(rootPath.resolve("pom.xml").toString());
        command.add("-DskipTests");
        if (!reactorProjects.isEmpty()) {
            command.add("-pl");
            command.add(String.join(",", reactorProjects));
            command.add("-am");
        }
        command.add(goal);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(rootPath.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output = readProcessOutput(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException("Static-analysis delegation failed with exit code "
                        + exitCode + System.lineSeparator() + output);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to start delegated static-analysis goals", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Delegated static-analysis goals were interrupted", e);
        }
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes());
    }

    private List<String> discoverReactorProjects(Path rootPath) throws MojoExecutionException {
        List<String> modules = new ArrayList<>();
        collectReactorProjects(rootPath, rootPath, modules);
        return modules;
    }

    private void collectReactorProjects(Path rootPath, Path currentDir, List<String> modules)
            throws MojoExecutionException {
        Path pomFile = currentDir.resolve("pom.xml");
        if (!Files.exists(pomFile)) {
            return;
        }
        for (String childModule : readPomModules(pomFile)) {
            Path moduleDir = currentDir.resolve(childModule).normalize();
            if (!Files.exists(moduleDir.resolve("pom.xml"))) {
                continue;
            }
            String relativePath = rootPath.relativize(moduleDir).toString();
            if (!relativePath.isBlank()) {
                modules.add(relativePath);
            }
            // Recursively collect nested modules
            collectReactorProjects(rootPath, moduleDir, modules);
        }
    }

    private List<String> readPomModules(Path pomFile) throws MojoExecutionException {
        List<String> modules = new ArrayList<>();
        Document document = loadXml(pomFile);
        NodeList moduleNodes = document.getElementsByTagName("module");
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            String module = moduleNodes.item(i).getTextContent();
            if (module != null && !module.isBlank()) {
                modules.add(module.trim());
            }
        }
        return modules;
    }

    private File findMavenExecutable() {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return null;
        }
        // PATH separator is always ":" on both Unix and Windows
        // (File.pathSeparator is ";", which is the classpath separator on Windows)
        for (String dir : path.split(":")) {
            if (dir == null || dir.isBlank()) {
                continue;
            }
            File dirFile = new File(dir);
            if (!dirFile.isDirectory()) {
                continue;
            }
            // On Windows, check mvn.cmd first, then fall back to mvn (Git Bash/MSYS2)
            for (String candidate : new String[]{"mvn.cmd", "mvn"}) {
                File executable = new File(dirFile, candidate);
                if (executable.isFile() && executable.canExecute()) {
                    return executable;
                }
            }
        }
        return null;
    }

    private void collectPmdIssues(Path rootPath) throws MojoExecutionException {
        for (Path report : findReports(rootPath, PMD_REPORT)) {
            parsePmdReport(report);
        }
    }

    private void collectCheckstyleIssues(Path rootPath) throws MojoExecutionException {
        for (Path report : findReports(rootPath, CHECKSTYLE_REPORT)) {
            parseCheckstyleReport(report);
        }
    }

    private void collectSpotbugsIssues(Path rootPath) throws MojoExecutionException {
        for (Path report : findReports(rootPath, SPOTBUGS_REPORT)) {
            parseSpotbugsReport(report);
        }
    }

    private List<Path> findReports(Path rootPath, String fileName) throws MojoExecutionException {
        List<Path> reports = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (fileName.equals(file.getFileName().toString())) {
                        reports.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan report files: " + fileName, e);
        }
        return reports;
    }

    private void parsePmdReport(Path report) throws MojoExecutionException {
        Document document = loadXml(report);
        NodeList violations = document.getElementsByTagName("violation");
        for (int i = 0; i < violations.getLength(); i++) {
            Element violation = (Element) violations.item(i);
            String filename = extractPmdFilename(violation);
            int line = parseInteger(violation.getAttribute("beginline"));
            String ruleName = violation.getAttribute("rule");
            String description = normalizeWhitespace(violation.getTextContent());
            result.addIssue(ruleName, mapPmdSeverity(violation.getAttribute("priority")), filename, line,
                    description, ruleName, DOC_STATIC_ANALYSIS, SOURCE_PMD);
        }
    }

    private String extractPmdFilename(Element violation) {
        Element file = (Element) violation.getParentNode();
        if (file != null && "file".equals(file.getTagName())) {
            String name = file.getAttribute("name");
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        // Fallback: try to get filename from violation element's attributes
        String filename = violation.getAttribute("filename");
        if (filename != null && !filename.isBlank()) {
            return filename;
        }
        return null;
    }

    private void parseCheckstyleReport(Path report) throws MojoExecutionException {
        Document document = loadXml(report);
        NodeList files = document.getElementsByTagName("file");
        for (int i = 0; i < files.getLength(); i++) {
            Element file = (Element) files.item(i);
            String filename = file.getAttribute("name");
            NodeList errors = file.getElementsByTagName("error");
            for (int j = 0; j < errors.getLength(); j++) {
                Element error = (Element) errors.item(j);
                String ruleName = extractCheckstyleRule(error.getAttribute("source"));
                result.addIssue(ruleName,
                        mapCheckstyleSeverity(error.getAttribute("severity")),
                        filename,
                        parseInteger(error.getAttribute("line")),
                        error.getAttribute("message"),
                        ruleName,
                        DOC_STATIC_ANALYSIS,
                        SOURCE_CHECKSTYLE);
            }
        }
    }

    private void parseSpotbugsReport(Path report) throws MojoExecutionException {
        Document document = loadXml(report);
        NodeList bugInstances = document.getElementsByTagName("BugInstance");
        for (int i = 0; i < bugInstances.getLength(); i++) {
            Element bugInstance = (Element) bugInstances.item(i);
            Element sourceLine = firstChildElement(bugInstance, "SourceLine");
            String filename = sourceLine == null ? null : sourceLine.getAttribute("sourcepath");
            int line = sourceLine == null ? 0 : parseInteger(sourceLine.getAttribute("start"));
            String type = bugInstance.getAttribute("type");
            String description = bugInstance.getAttribute("message");
            if (description == null || description.isBlank()) {
                description = normalizeWhitespace(textOfFirstChild(bugInstance, "LongMessage"));
            }
            result.addIssue(type,
                    mapSpotbugsSeverity(bugInstance.getAttribute("priority"), bugInstance.getAttribute("rank")),
                    filename,
                    line,
                    description,
                    type,
                    DOC_STATIC_ANALYSIS,
                    SOURCE_SPOTBUGS);
        }
    }

    private Document loadXml(Path report) throws MojoExecutionException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(report.toFile());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to parse XML report: " + report, e);
        }
    }

    private Element firstChildElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return (Element) nodes.item(0);
    }

    private String textOfFirstChild(Element parent, String tagName) {
        Element child = firstChildElement(parent, tagName);
        return child == null ? "" : child.getTextContent();
    }

    private int parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String mapPmdSeverity(String priority) {
        int level = parseInteger(priority);
        if (level <= 1) {
            return "CRITICAL";
        }
        if (level == 2) {
            return "MAJOR";
        }
        return "MINOR";
    }

    private String mapCheckstyleSeverity(String severity) {
        if ("error".equalsIgnoreCase(severity)) {
            return "MAJOR";
        }
        return "MINOR";
    }

    private String mapSpotbugsSeverity(String priority, String rank) {
        int priorityValue = parseInteger(priority);
        int rankValue = parseInteger(rank);
        if (priorityValue == 1 || (rankValue > 0 && rankValue <= 4)) {
            return "CRITICAL";
        }
        if (priorityValue == 2 || (rankValue > 0 && rankValue <= 9)) {
            return "MAJOR";
        }
        return "MINOR";
    }

    private String extractCheckstyleRule(String source) {
        if (source == null || source.isBlank()) {
            return "CHECKSTYLE";
        }
        int index = source.lastIndexOf('.');
        if (index >= 0 && index < source.length() - 1) {
            return source.substring(index + 1);
        }
        return source;
    }

    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private Map<String, ModuleDescriptor> loadModuleDescriptors(Path rootPath, List<ModuleInfoIssue> issues) {
        Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
        Map<String, String> modulePathOwners = new LinkedHashMap<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toString().endsWith("/pom.xml")) {
                        return FileVisitResult.CONTINUE;
                    }

                    String content = Files.readString(file);
                    String artifactId = extractArtifactId(content);
                    if (artifactId == null) {
                        return FileVisitResult.CONTINUE;
                    }

                    Path moduleInfoFile = file.getParent().resolve("src/main/resources/META-INF/mango/module.properties");
                    ModuleType moduleType = classifyModule(artifactId);
                    if (moduleType != ModuleType.STARTER) {
                        if (Files.exists(moduleInfoFile)) {
                            issues.add(new ModuleInfoIssue("CRITICAL", moduleInfoFile.toString(),
                                    artifactId + " 不允许声明 module.properties；只有本地 starter 可以声明模块信息"));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    if ("mango-infra-module-starter".equals(artifactId)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!Files.exists(moduleInfoFile)) {
                        return FileVisitResult.CONTINUE;
                    }

                    Properties properties = new Properties();
                    try (InputStream inputStream = Files.newInputStream(moduleInfoFile)) {
                        properties.load(inputStream);
                    }

                    String moduleName = properties.getProperty("module-name", "").trim();
                    String modulePath = normalizeModulePath(properties.getProperty("module-path", "").trim());
                    if (moduleName.isEmpty()) {
                        issues.add(new ModuleInfoIssue("CRITICAL", moduleInfoFile.toString(),
                                artifactId + " 的 module-name 不能为空"));
                        return FileVisitResult.CONTINUE;
                    }
                    if (!MODULE_NAME_PATTERN.matcher(moduleName).matches()) {
                        issues.add(new ModuleInfoIssue("MAJOR", moduleInfoFile.toString(),
                                artifactId + " 的 module-name 只能使用小写字母、数字和中划线: " + moduleName));
                    }
                    if (modulePath.isEmpty()) {
                        issues.add(new ModuleInfoIssue("CRITICAL", moduleInfoFile.toString(),
                                artifactId + " 的 module-path 不能为空"));
                        return FileVisitResult.CONTINUE;
                    }

                    String owner = modulePathOwners.putIfAbsent(modulePath, artifactId);
                    if (owner != null && !owner.equals(artifactId)) {
                        issues.add(new ModuleInfoIssue("CRITICAL", moduleInfoFile.toString(),
                                "module-path 冲突: " + artifactId + " 与 " + owner + " 都声明为 " + modulePath));
                    }
                    descriptors.put(artifactId, new ModuleDescriptor(artifactId, moduleName, modulePath,
                            moduleInfoFile.toString()));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            issues.add(new ModuleInfoIssue("MAJOR", rootPath.toString(), "模块信息扫描失败: " + e.getMessage()));
        }
        return descriptors;
    }

    private String normalizeModulePath(String modulePath) {
        if (modulePath == null || modulePath.isBlank()) {
            return "";
        }
        String normalized = modulePath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("/+$", "");
        if ("/".equals(normalized) || normalized.startsWith("/_")) {
            return "";
        }
        return normalized;
    }

    private String reverseModulePath(String modulePath) {
        return "/_" + modulePath.substring(1);
    }

    private List<Path> starterJavaFiles(Path moduleDir) throws IOException {
        List<Path> files = new ArrayList<>();
        Path sourceRoot = moduleDir.resolve("src/main/java");
        if (!Files.exists(sourceRoot)) {
            return files;
        }
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private Path siblingRemoteModule(Path starterDir, String artifactId) {
        String remoteArtifactId = artifactId + "-remote";
        Path remoteDir = starterDir.getParent().resolve(remoteArtifactId);
        if (Files.exists(remoteDir.resolve("pom.xml"))) {
            return remoteDir;
        }
        return null;
    }

    private String extractControllerRootPath(Path javaFile) throws IOException {
        String content = Files.readString(javaFile);
        if (!isControllerSource(content)) {
            return "";
        }
        Matcher matcher = REQUEST_MAPPING_PATTERN.matcher(content);
        while (matcher.find()) {
            String mapping = normalizeMappingPath(extractMappingValue(matcher.group(1)));
            if (!mapping.isEmpty()) {
                return mapping;
            }
        }
        return "";
    }

    private String extractMappingValue(String annotationBody) {
        String value = extractAnnotationAttribute(annotationBody, "value");
        if (!value.isEmpty()) {
            return value;
        }
        return extractAnnotationAttribute(annotationBody, "path");
    }

    private String extractAnnotationAttribute(String annotationBody, String attributeName) {
        if (annotationBody == null || annotationBody.isBlank()) {
            return "";
        }
        Matcher namedMatcher = Pattern.compile(attributeName + "\\s*=\\s*\"([^\"]+)\"").matcher(annotationBody);
        if (namedMatcher.find()) {
            return namedMatcher.group(1).trim();
        }
        if ("value".equals(attributeName)) {
            Matcher valueMatcher = Pattern.compile("^\\s*\"([^\"]+)\"").matcher(annotationBody.trim());
            if (valueMatcher.find()) {
                return valueMatcher.group(1).trim();
            }
        }
        return "";
    }

    private String normalizeMappingPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = resolvePlaceholderDefault(path.trim());
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/+$", "");
    }

    private String resolvePlaceholderDefault(String path) {
        if (!path.startsWith("${") || !path.endsWith("}")) {
            return path;
        }
        int separator = path.indexOf(':');
        if (separator < 0 || separator >= path.length() - 1) {
            return path;
        }
        return path.substring(separator + 1, path.length() - 1).trim();
    }

    private String artifactIdFromPath(Path file) {
        String artifactId = "";
        for (Path part : file) {
            String name = part.toString();
            if (name.startsWith("mango-")) {
                artifactId = name;
            }
        }
        return artifactId;
    }

    private boolean isControllerSource(String content) {
        return content.contains("@RestController") || content.contains("@Controller");
    }

    private void checkNaming() {
        getLog().info("Checking Mango-specific naming conventions...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<NamingIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if ("pom.xml".equals(file.getFileName().toString())) {
                        analyzeMavenModuleName(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (NamingIssue issue : issues) {
                result.addIssue("NAMING", issue.severity, issue.file, issue.line,
                        issue.description, "NAMING", DOC_NAMING_RULES, SOURCE_MANGO_CHECK);
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " naming violation(s)");
        } else {
            getLog().info("Mango-specific naming checks passed");
        }
    }

    private void analyzeMavenModuleName(Path file, List<NamingIssue> issues) {
        try {
            String content = Files.readString(file);
            String projectContent = content.replaceFirst("(?s)<parent>.*?</parent>", "");
            Matcher matcher = Pattern.compile("<artifactId>\\s*([^<\\s]+)\\s*</artifactId>").matcher(projectContent);
            if (!matcher.find()) {
                return;
            }

            String artifactId = matcher.group(1).trim();
            if (!artifactId.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")) {
                issues.add(new NamingIssue("MAJOR", file.toString(), lineNumber(content, content.indexOf(artifactId)),
                        "Mango module artifactId must use kebab-case: " + artifactId));
            }
        } catch (IOException e) {
            issues.add(new NamingIssue("MAJOR", file.toString(), 0,
                    "模块命名检查失败: " + e.getMessage()));
        }
    }

    /**
     * Rules:
     * 1. Mango modules are evaluated in two dimensions: first-level layer
     *    (common / infra / platform / app), then second-level role
     *    (api / support / core / starter / starter-remote).
     * 2. *-api cannot depend on *-core, *-starter, *-starter-remote.
     * 3. *-core cannot depend on *-starter, *-starter-remote.
     * 4. *-starter-remote can only depend on its own *-api, *-support and mango-infra-feign-starter inside io.mango modules.
     * 5. *-starter-remote must not directly depend on spring-cloud-starter-openfeign.
     */
    private void checkDependency() {
        getLog().info("Checking module dependencies...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<DependencyIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith("/pom.xml")) {
                        analyzePomDependency(file, issues);
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
                        issue.description, "DEPENDENCY", DOC_MODULE_RULES, SOURCE_MANGO_CHECK);
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " dependency violation(s)");
        } else {
            getLog().info("All dependency checks passed");
        }
    }

    private void analyzePomDependency(Path pomFile, List<DependencyIssue> issues) {
        try {
            String content = Files.readString(pomFile);
            String artifactId = extractArtifactId(content);
            if (artifactId == null) {
                return;
            }

            ModuleType moduleType = classifyModule(artifactId);
            for (String dependencyBlock : extractDependencies(content)) {
                String depArtifactId = extractArtifactIdFromDep(dependencyBlock);
                if (depArtifactId == null || depArtifactId.isEmpty()) {
                    continue;
                }
                String depGroupId = extractGroupIdFromDep(dependencyBlock);
                if (moduleType == ModuleType.STARTER_REMOTE
                        && "org.springframework.cloud".equals(depGroupId)
                        && "spring-cloud-starter-openfeign".equals(depArtifactId)) {
                    DependencyIssue issue = new DependencyIssue("CRITICAL",
                            "*-starter-remote 模块禁止直接依赖 spring-cloud-starter-openfeign，请依赖 mango-infra-feign-starter: "
                                    + artifactId + " -> " + depArtifactId);
                    issue.file = pomFile.toString();
                    issues.add(issue);
                    continue;
                }
                if (!"io.mango".equals(depGroupId)) {
                    continue;
                }

                DependencyIssue issue = validateDependency(moduleType, artifactId, depArtifactId);
                if (issue != null) {
                    issue.file = pomFile.toString();
                    issues.add(issue);
                }
            }
        } catch (Exception e) {
            getLog().warn("Failed to analyze " + pomFile + ": " + e.getMessage());
        }
    }

    private ModuleType classifyModule(String artifactId) {
        if (artifactId.endsWith("-api")) {
            return ModuleType.API;
        }
        if (artifactId.endsWith("-support")) {
            return ModuleType.SUPPORT;
        }
        if (artifactId.endsWith("-core")) {
            return ModuleType.CORE;
        }
        if (artifactId.endsWith("-starter-remote")) {
            return ModuleType.STARTER_REMOTE;
        }
        if (artifactId.endsWith("-starter")) {
            return ModuleType.STARTER;
        }
        if (artifactId.endsWith("-app")) {
            return ModuleType.APP;
        }
        if (artifactId.startsWith("mango-") && !"mango".equals(artifactId) && !"mango-parent".equals(artifactId)) {
            return ModuleType.OTHER;
        }
        return ModuleType.ROOT;
    }

    private DependencyIssue validateDependency(ModuleType consumer, String consumerArtifact, String depArtifact) {
        if (consumer == ModuleType.API) {
            if (depArtifact.endsWith("-core")) {
                return new DependencyIssue("CRITICAL",
                        "*_api 模块不能依赖 *-core: " + consumerArtifact + " -> " + depArtifact);
            }
            if (depArtifact.endsWith("-starter")) {
                return new DependencyIssue("CRITICAL",
                        "*_api 模块不能依赖 *-starter: " + consumerArtifact + " -> " + depArtifact);
            }
            if (depArtifact.endsWith("-starter-remote")) {
                return new DependencyIssue("CRITICAL",
                        "*_api 模块不能依赖 *-starter-remote: " + consumerArtifact + " -> " + depArtifact);
            }
        }

        if (consumer == ModuleType.CORE) {
            if (depArtifact.endsWith("-starter")) {
                return new DependencyIssue("CRITICAL",
                        "*_core 模块不能依赖 *-starter: " + consumerArtifact + " -> " + depArtifact);
            }
            if (depArtifact.endsWith("-starter-remote")) {
                return new DependencyIssue("CRITICAL",
                        "*_core 模块不能依赖 *-starter-remote: " + consumerArtifact + " -> " + depArtifact);
            }
        }

        if (consumer == ModuleType.STARTER_REMOTE) {
            if (isAuthorizationRemoteSupportDependency(consumerArtifact, depArtifact)) {
                return null;
            }
            if (isRemoteInfrastructureDependency(depArtifact)) {
                return null;
            }
            String expectedApi = expectedRemoteApi(consumerArtifact);
            String expectedSupport = expectedRemoteSupport(consumerArtifact);
            if (!depArtifact.equals(expectedApi) && !depArtifact.equals(expectedSupport)) {
                return new DependencyIssue("CRITICAL",
                        "*-starter-remote 模块只能依赖本模块 api/support 或 mango-infra-feign-starter: " + consumerArtifact
                                + " -> " + depArtifact + "，期望依赖 " + expectedApi + "、" + expectedSupport
                                + " 或 mango-infra-feign-starter");
            }
        }

        return null;
    }

    private boolean isAuthorizationRemoteSupportDependency(String consumerArtifact, String depArtifact) {
        return "mango-authorization-starter-remote".equals(consumerArtifact)
                && "mango-authorization-support".equals(depArtifact);
    }

    private boolean isRemoteInfrastructureDependency(String depArtifact) {
        return "mango-infra-feign-starter".equals(depArtifact);
    }

    private String expectedRemoteApi(String consumerArtifact) {
        if (consumerArtifact.endsWith("-starter-remote")) {
            return consumerArtifact.substring(0, consumerArtifact.length() - "-starter-remote".length()) + "-api";
        }
        return consumerArtifact;
    }

    private String expectedRemoteSupport(String consumerArtifact) {
        if (consumerArtifact.endsWith("-starter-remote")) {
            return consumerArtifact.substring(0, consumerArtifact.length() - "-starter-remote".length()) + "-support";
        }
        return consumerArtifact;
    }

    /**
     * Rules:
     * 1. *-api can depend on mango-infra-web-api, but not mango-infra-web-starter.
     * 2. Modules that depend on mango-infra-web-starter must not also declare spring-boot-starter-web.
     * 3. Refactored common/infra modules must not depend directly on spring-boot-starter-web.
     */
    private void checkWebBoundary() {
        getLog().info("Checking web boundary declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<WebBoundaryIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith("/pom.xml")) {
                        analyzeWebBoundaryPom(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (WebBoundaryIssue issue : issues) {
                result.addIssue("WEB_BOUNDARY", issue.severity, issue.file, 0,
                        issue.description, "WEB_BOUNDARY", DOC_MODULE_RULES, SOURCE_MANGO_CHECK);
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " web boundary violation(s)");
        } else {
            getLog().info("All web boundary checks passed");
        }
    }

    private void analyzeWebBoundaryPom(Path pomFile, List<WebBoundaryIssue> issues) {
        try {
            String content = Files.readString(pomFile);
            String artifactId = extractArtifactId(content);
            if (artifactId == null) {
                return;
            }
            List<String> dependencies = extractDependencies(content);
            boolean dependsWebApi = hasDependency(dependencies, "io.mango", "mango-infra-web-api");
            boolean dependsWebStarter = hasDependency(dependencies, "io.mango", "mango-infra-web-starter");
            boolean dependsSpringWebStarter = hasDependency(dependencies,
                    "org.springframework.boot", "spring-boot-starter-web");

            if (artifactId.endsWith("-api") && dependsWebStarter) {
                issues.add(new WebBoundaryIssue("CRITICAL", pomFile.toString(),
                        "*-api 如需 Web 边界注解只能依赖 mango-infra-web-api，禁止依赖 mango-infra-web-starter"));
            }
            if (dependsWebStarter && dependsSpringWebStarter) {
                issues.add(new WebBoundaryIssue("MAJOR", pomFile.toString(),
                        artifactId + " 已依赖 mango-infra-web-starter，不应重复声明 spring-boot-starter-web"));
            }
            if (isRefactoredWebScope(pomFile, artifactId)
                    && dependsSpringWebStarter
                    && !"mango-infra-web-starter".equals(artifactId)) {
                issues.add(new WebBoundaryIssue("MAJOR", pomFile.toString(),
                        artifactId + " 属于已重构 Web 边界范围，不应直接依赖 spring-boot-starter-web"));
            }
            if (artifactId.endsWith("-api") && sourceUsesInner(pomFile) && !dependsWebApi) {
                issues.add(new WebBoundaryIssue("CRITICAL", pomFile.toString(),
                        artifactId + " 使用 @Inner 时必须显式依赖 mango-infra-web-api"));
            }
        } catch (IOException e) {
            issues.add(new WebBoundaryIssue("MAJOR", pomFile.toString(),
                    "Web 边界检查失败: " + e.getMessage()));
        }
    }

    private boolean hasDependency(List<String> dependencyBlocks, String groupId, String artifactId) {
        for (String dependencyBlock : dependencyBlocks) {
            if (artifactId.equals(extractArtifactIdFromDep(dependencyBlock))
                    && groupId.equals(extractGroupIdFromDep(dependencyBlock))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRefactoredWebScope(Path pomFile, String artifactId) {
        String normalized = pomFile.toString().replace('\\', '/');
        return artifactId.startsWith("mango-infra-kv")
                || artifactId.startsWith("mango-infra-realtime")
                || artifactId.startsWith("mango-infra-web")
                || normalized.contains("/mango-common/");
    }

    private boolean sourceUsesInner(Path pomFile) throws IOException {
        Path sourceRoot = pomFile.getParent().resolve("src/main/java");
        if (!Files.exists(sourceRoot)) {
            return false;
        }
        final boolean[] found = {false};
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java") && Files.readString(file).contains("@Inner")) {
                    found[0] = true;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return found[0];
    }

    /**
     * Rules:
     * 1. Local *-starter modules must provide META-INF/mango/module.properties.
     * 2. module.properties must declare module-name, module-path.
     * 3. module-name must be stable and kebab-case.
     * 4. module-path must be unique across starter modules.
     * 5. starter controller root path must start with module-path or /_{module-path}.
     * 6. starter-remote reverse controller root path must start with /_{module-path}.
     */
    private void checkModuleInfo() {
        getLog().info("Checking module info declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<ModuleInfoIssue> issues = new ArrayList<>();
        Map<String, ModuleDescriptor> descriptors = loadModuleDescriptors(rootPath, issues);
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith("/pom.xml")) {
                        analyzeModuleInfo(file, descriptors, issues);
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
                        issue.description, "MODULE_INFO", DOC_MODULE_RULES, SOURCE_MANGO_CHECK);
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " module info violation(s)");
        } else {
            getLog().info("All module info checks passed");
        }
    }

    private void analyzeModuleInfo(Path pomFile, Map<String, ModuleDescriptor> descriptors, List<ModuleInfoIssue> issues) {
        try {
            String content = Files.readString(pomFile);
            String artifactId = extractArtifactId(content);
            if (artifactId == null || classifyModule(artifactId) != ModuleType.STARTER) {
                return;
            }
            if ("mango-infra-module-starter".equals(artifactId)) {
                return;
            }

            Path moduleInfoFile = pomFile.getParent().resolve("src/main/resources/META-INF/mango/module.properties");
            if (!Files.exists(moduleInfoFile)) {
                issues.add(new ModuleInfoIssue("CRITICAL", moduleInfoFile.toString(),
                        artifactId + " 缺少 META-INF/mango/module.properties"));
                return;
            }

            ModuleDescriptor descriptor = descriptors.get(artifactId);
            if (descriptor == null) {
                return;
            }

            for (Path javaFile : starterJavaFiles(pomFile.getParent())) {
                String rootPath = extractControllerRootPath(javaFile);
                if (rootPath.isEmpty()) {
                    continue;
                }
                if (!isModuleControllerPath(rootPath, descriptor.modulePath)) {
                    issues.add(new ModuleInfoIssue("CRITICAL", javaFile.toString(),
                            artifactId + " 的 Controller 根路径必须以 module-path " + descriptor.modulePath
                                    + " 或反向 module-path " + reverseModulePath(descriptor.modulePath)
                                    + " 开头，当前为 " + rootPath));
                }
            }

            Path remoteModuleDir = siblingRemoteModule(pomFile.getParent(), artifactId);
            if (remoteModuleDir != null) {
                String reversePath = reverseModulePath(descriptor.modulePath);
                for (Path javaFile : starterJavaFiles(remoteModuleDir)) {
                    String rootPath = extractControllerRootPath(javaFile);
                    if (rootPath.isEmpty()) {
                        continue;
                    }
                    if (!rootPath.startsWith(reversePath)) {
                        issues.add(new ModuleInfoIssue("CRITICAL", javaFile.toString(),
                                remoteModuleDir.getFileName() + " 的反向 Controller 根路径必须以 " + reversePath
                                        + " 开头，当前为 " + rootPath));
                    }
                }
            }
        } catch (Exception e) {
            issues.add(new ModuleInfoIssue("MAJOR", pomFile.toString(),
                    "模块信息检查失败: " + e.getMessage()));
        }
    }

    private boolean isModuleControllerPath(String rootPath, String modulePath) {
        return rootPath.startsWith(modulePath) || rootPath.startsWith(reverseModulePath(modulePath));
    }

    /**
     * Rules:
     * 1. starter-remote Feign client name must use the target module-name.
     * 2. starter-remote Feign client path must start with the target module-path.
     */
    private void checkRemoteAdapter() {
        getLog().info("Checking remote adapter declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<RemoteAdapterIssue> issues = new ArrayList<>();
        Map<String, ModuleDescriptor> descriptors = loadModuleDescriptors(rootPath, new ArrayList<>());

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isStarterRemoteJavaFile(file)) {
                        analyzeRemoteAdapter(file, descriptors, issues);
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
                        issue.description, "REMOTE_ADAPTER", DOC_MODULE_RULES, SOURCE_MANGO_CHECK);
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

    private void analyzeRemoteAdapter(Path file, Map<String, ModuleDescriptor> descriptors, List<RemoteAdapterIssue> issues) {
        try {
            String content = Files.readString(file);
            Matcher matcher = FEIGN_CLIENT_PATTERN.matcher(content);
            String artifactId = artifactIdFromPath(file);
            String expectedArtifactId = artifactId.endsWith("-starter-remote")
                    ? artifactId.substring(0, artifactId.length() - "-starter-remote".length()) + "-starter"
                    : artifactId;
            ModuleDescriptor descriptor = descriptors.get(expectedArtifactId);
            while (matcher.find()) {
                String annotationBody = matcher.group(1);
                String feignName = extractAnnotationAttribute(annotationBody, "name");
                String feignPath = normalizeMappingPath(extractAnnotationAttribute(annotationBody, "path"));
                if (descriptor == null) {
                    issues.add(new RemoteAdapterIssue("CRITICAL", file.toString(),
                            artifactId + " 缺少目标 starter 的 module.properties，无法校验 @FeignClient"));
                    continue;
                }
                if (!descriptor.moduleName.equals(feignName)) {
                    issues.add(new RemoteAdapterIssue("CRITICAL", file.toString(),
                            "@FeignClient name 必须使用目标模块 module-name " + descriptor.moduleName + "，当前为 " + feignName));
                }
                if (!feignPath.startsWith(descriptor.modulePath)) {
                    issues.add(new RemoteAdapterIssue("CRITICAL", file.toString(),
                            "@FeignClient path 必须以目标模块 module-path " + descriptor.modulePath + " 开头，当前为 " + feignPath));
                }
            }
        } catch (IOException e) {
            issues.add(new RemoteAdapterIssue("MAJOR", file.toString(),
                    "远程适配器检查失败: " + e.getMessage()));
        }
    }

    /**
     * Rules:
     * 1. *-api source must not declare @FeignClient.
     * 2. *-api must not contain local implementation-collaboration types such as *Service/*Manager.
     * 3. HTTP-facing contract types in *-api must use XxxApi naming.
     * 4. Controllers must not hold XxxApi fields.
     * 5. Services must not directly implement XxxApi.
     * 6. Feign clients must implement XxxApi.
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
                    if (isMainJavaFile(file)) {
                        analyzeApiLayerContract(file, issues);
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
                        issue.description, "API_CONTRACT", DOC_API_RULES, SOURCE_MANGO_CHECK);
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
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            if (code.contains("@FeignClient")) {
                issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                        "*-api 禁止声明 @FeignClient"));
            }
            String typeName = declaredTypeName(code, file);
            if (isLocalCollaborationType(typeName) && !isApiSpiJavaFile(file) && !isAllowedInfrastructureApi(typeName)) {
                issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                        "*-api 只允许放跨模块契约，禁止出现本地协作类型: " + typeName));
            }
            if (typeName.endsWith("Api") && containsLocalImplementationWord(typeName)) {
                issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                        "XxxApi 名称只表达跨模块能力，禁止包含 Registry/Dispatcher/Manager/Session 等内部实现词: " + typeName));
            }
            if (declaresHttpContract(code) && !typeName.endsWith("Api")) {
                issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                        "Controller/远程契约接口在 *-api 中必须命名为 XxxApi: " + typeName));
            }
            Matcher apiMethodMatcher = API_METHOD_PATTERN.matcher(code);
            while (apiMethodMatcher.find()) {
                int parameterCount = countTopLevelParameters(apiMethodMatcher.group(2));
                if (parameterCount > 1) {
                    issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                            "XxxApi 方法超过 1 个入参时必须收敛为 Query/Command 对象: "
                                    + apiMethodMatcher.group(1) + " 入参数=" + parameterCount));
                }
            }
        } catch (IOException e) {
            issues.add(new ApiContractIssue("MAJOR", file.toString(),
                    "API 契约检查失败: " + e.getMessage()));
        }
    }

    private int countTopLevelParameters(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return 0;
        }
        int count = 1;
        int angleDepth = 0;
        int parenDepth = 0;
        for (int i = 0; i < parameters.length(); i++) {
            char current = parameters.charAt(i);
            if (current == '<') {
                angleDepth++;
            } else if (current == '>' && angleDepth > 0) {
                angleDepth--;
            } else if (current == '(') {
                parenDepth++;
            } else if (current == ')' && parenDepth > 0) {
                parenDepth--;
            } else if (current == ',' && angleDepth == 0 && parenDepth == 0) {
                count++;
            }
        }
        return count;
    }

    private void analyzeApiLayerContract(Path file, List<ApiContractIssue> issues) {
        if (isMangoToolingFile(file)) {
            return;
        }
        try {
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            if (code.contains("@RestController")) {
                Matcher apiFieldMatcher = API_FIELD_PATTERN.matcher(code);
                while (apiFieldMatcher.find()) {
                    issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                            "Controller 禁止持有 XxxApi 字段，应实现 XxxApi 并依赖 IXxxService: "
                                    + apiFieldMatcher.group(1)));
                }
            }
            Matcher serviceMatcher = SERVICE_IMPLEMENTS_API_PATTERN.matcher(code);
            if (serviceMatcher.find()) {
                issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                        "XxxService 禁止直接实现 XxxApi，应实现 IXxxService: " + serviceMatcher.group(1)));
            }
            if (code.contains("@FeignClient") && !FEIGN_EXTENDS_API_PATTERN.matcher(code).find()) {
                issues.add(new ApiContractIssue("CRITICAL", file.toString(),
                        "XxxFeignClient 必须继承本域 XxxApi"));
            }
        } catch (IOException e) {
            issues.add(new ApiContractIssue("MAJOR", file.toString(),
                    "API 分层检查失败: " + e.getMessage()));
        }
    }

    private String declaredTypeName(String content, Path file) {
        Matcher matcher = TYPE_DECLARATION_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String fileName = file.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private boolean isLocalCollaborationType(String typeName) {
        return typeName.endsWith("Service")
                || typeName.endsWith("Manager")
                || typeName.endsWith("Registry")
                || typeName.endsWith("Session")
                || typeName.endsWith("Dispatcher");
    }

    private boolean isApiSpiJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("-api/src/main/java/") && normalized.contains("/api/spi/");
    }

    private boolean isMangoToolingFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/mango-tools/mango-maven-plugin/src/main/java/");
    }

    private boolean isAllowedInfrastructureApi(String typeName) {
        return "ModuleInfoRegistry".equals(typeName);
    }

    private String stripStringLiterals(String content) {
        StringBuilder result = new StringBuilder(content.length());
        boolean inString = false;
        boolean inTextBlock = false;
        boolean escaped = false;
        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (inTextBlock) {
                if (i + 2 < content.length()
                        && content.charAt(i) == '"'
                        && content.charAt(i + 1) == '"'
                        && content.charAt(i + 2) == '"') {
                    result.append("   ");
                    i += 2;
                    inTextBlock = false;
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }
            if (inString) {
                if (current == '\n') {
                    result.append('\n');
                    inString = false;
                    escaped = false;
                    continue;
                }
                result.append(' ');
                if (current == '"' && !escaped) {
                    inString = false;
                }
                escaped = current == '\\' && !escaped;
                if (current != '\\') {
                    escaped = false;
                }
                continue;
            }
            if (i + 2 < content.length()
                    && content.charAt(i) == '"'
                    && content.charAt(i + 1) == '"'
                    && content.charAt(i + 2) == '"') {
                result.append("   ");
                i += 2;
                inTextBlock = true;
                continue;
            }
            if (current == '"') {
                result.append(' ');
                inString = true;
                escaped = false;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private boolean containsLocalImplementationWord(String typeName) {
        return typeName.contains("Service")
                || typeName.contains("Manager")
                || typeName.contains("Registry")
                || typeName.contains("Session")
                || typeName.contains("Dispatcher");
    }

    private boolean declaresHttpContract(String content) {
        return content.contains("@RequestMapping")
                || content.contains("@GetMapping")
                || content.contains("@PostMapping")
                || content.contains("@PutMapping")
                || content.contains("@DeleteMapping")
                || content.contains("@PatchMapping");
    }

    /**
     * Rules:
     * 1. KV annotation key must not hardcode mango:kv prefix.
     * 2. Dynamic key must use SpEL template syntax such as user:#{#userId}.
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
                        issue.description, "KV_KEY", DOC_NAMING_RULES, SOURCE_MANGO_CHECK);
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

    /**
     * 禁止 HTTP API 使用 /{id} 这类路径变量。统一使用 query 参数或 command/query 对象。
     */
    private void checkPathParam() {
        getLog().info("Checking path parameter usage...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<PathParamIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isMainJavaFile(file)) {
                        analyzePathParam(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (PathParamIssue issue : issues) {
                result.addIssue("PATH_PARAM", issue.severity, issue.file, issue.line,
                        issue.description, "PATH_PARAM", DOC_API_RULES, SOURCE_MANGO_CHECK);
                getLog().warn("  [" + issue.severity + "] " + issue.description
                        + " at " + issue.file + ":" + issue.line);
            }
            getLog().warn("Found " + issues.size() + " path parameter violation(s)");
        } else {
            getLog().info("All path parameter checks passed");
        }
    }

    private void analyzePathParam(Path file, List<PathParamIssue> issues) {
        try {
            String content = Files.readString(file);
            int pathVariableIndex = content.indexOf(PATH_VARIABLE_ANNOTATION);
            if (pathVariableIndex >= 0) {
                issues.add(new PathParamIssue("CRITICAL", file.toString(),
                        lineNumber(content, pathVariableIndex),
                        "HTTP API 禁止使用 " + PATH_VARIABLE_ANNOTATION
                                + "，请改为 @RequestParam 或 Query/Command 对象"));
            }
            Matcher matcher = REQUEST_MAPPING_PATTERN.matcher(content);
            while (matcher.find()) {
                String mapping = extractMappingValue(matcher.group(1));
                if (usesPathTemplateVariable(mapping)) {
                    issues.add(new PathParamIssue("CRITICAL", file.toString(),
                            lineNumber(content, matcher.start()),
                            "HTTP API 禁止使用路径模板变量 " + mapping + "，请改为 query 参数"));
                }
            }
        } catch (IOException e) {
            issues.add(new PathParamIssue("MAJOR", file.toString(), 0,
                    "路径参数检查失败: " + e.getMessage()));
        }
    }

    private boolean usesPathTemplateVariable(String mapping) {
        if (mapping == null || mapping.isBlank()) {
            return false;
        }
        String resolved = resolvePlaceholderDefault(mapping.trim());
        Matcher matcher = Pattern.compile("\\{([^}:]+)}").matcher(resolved);
        return matcher.find();
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
                if (key.startsWith("mango:kv:")) {
                    issues.add(new KvKeyIssue("CRITICAL", file.toString(), line,
                            "KV 注解 key 不得手写 mango:kv 前缀，应由 capability 自动补齐"));
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

    /**
     * Rules:
     * 1. Migration CREATE TABLE statements must include audit and tenant columns.
     * 2. Tables managed by external infrastructure can opt out with an explicit mango-check comment.
     */
    private void checkPersistenceSchema() {
        getLog().info("Checking persistence schema declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<PersistenceSchemaIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isMigrationSqlFile(file)) {
                        analyzePersistenceSchema(file, issues);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (PersistenceSchemaIssue issue : issues) {
                result.addIssue("PERSISTENCE_SCHEMA", issue.severity, issue.file, issue.line,
                        issue.description, "PERSISTENCE_SCHEMA", DOC_PERSISTENCE_RULES, SOURCE_MANGO_CHECK);
                getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
            }
            getLog().warn("Found " + issues.size() + " persistence schema violation(s)");
        } else {
            getLog().info("All persistence schema checks passed");
        }
    }

    private boolean isMigrationSqlFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/src/main/resources/db/migration/") && normalized.endsWith(".sql");
    }

    private void analyzePersistenceSchema(Path file, List<PersistenceSchemaIssue> issues) {
        try {
            String content = Files.readString(file);
            Matcher matcher = CREATE_TABLE_PATTERN.matcher(content);
            while (matcher.find()) {
                String tableName = normalizeSqlName(matcher.group(2));
                int statementEnd = findStatementEnd(content, matcher.end());
                String statement = content.substring(matcher.start(), statementEnd);
                int line = lineNumber(content, matcher.start());
                if (shouldSkipPersistenceTable(tableName) || isPersistenceSchemaDisabled(content, matcher.start())) {
                    continue;
                }
                Set<String> columns = extractSqlColumns(statement);
                for (String requiredColumn : REQUIRED_PERSISTENCE_COLUMNS) {
                    if (!columns.contains(requiredColumn)) {
                        issues.add(new PersistenceSchemaIssue("CRITICAL", file.toString(), line,
                                "表 " + tableName + " 缺少标准持久化字段 " + requiredColumn));
                    }
                }
            }
        } catch (IOException e) {
            issues.add(new PersistenceSchemaIssue("MAJOR", file.toString(), 0,
                    "持久化表结构检查失败: " + e.getMessage()));
        }
    }

    private int findStatementEnd(String content, int offset) {
        int index = content.indexOf(';', offset);
        return index >= 0 ? index + 1 : content.length();
    }

    private boolean shouldSkipPersistenceTable(String tableName) {
        return DEFAULT_PERSISTENCE_EXCLUDED_TABLES.contains(tableName);
    }

    private boolean isPersistenceSchemaDisabled(String content, int offset) {
        int start = Math.max(0, offset - 300);
        String prefix = content.substring(start, offset).toLowerCase(Locale.ROOT);
        return prefix.contains("mango-check: disable persistence-audit-fields");
    }

    private Set<String> extractSqlColumns(String statement) {
        Set<String> columns = new HashSet<>();
        Matcher matcher = Pattern.compile("(?im)^\\s*`?([a-zA-Z][a-zA-Z0-9_]*)`?\\s+").matcher(statement);
        while (matcher.find()) {
            String column = normalizeSqlName(matcher.group(1));
            if (!isSqlConstraintKeyword(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    private boolean isSqlConstraintKeyword(String value) {
        return Set.of("create", "primary", "key", "unique", "constraint", "index",
                "foreign", "fulltext", "spatial", "check").contains(value);
    }

    private String normalizeSqlName(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("`", "").trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Rules:
     * 1. Redis*Test must not use MemoryKvStore or JdbcKvStore as its core store fixture.
     * 2. Jdbc*Test must not use MemoryKvStore or RedisKvStore as its core store fixture.
     * 3. Memory*Test must not use RedisKvStore or JdbcKvStore as its core store fixture.
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
                        issue.description, "TEST_FIXTURE", DOC_TEST_RULES, SOURCE_MANGO_CHECK);
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

            for (String candidate : List.of("MemoryKvStore", "RedisKvStore", "JdbcKvStore")) {
                if (!candidate.startsWith(expected) && content.contains(candidate)) {
                    issues.add(new TestFixtureIssue("CRITICAL", file.toString(),
                            lineNumber(content, content.indexOf(candidate)),
                            fileName + " 表示测试 " + expected + " 实现，但测试物料出现 " + candidate
                                    + "；实现类型测试必须使用同名实现，通用能力测试应按能力命名并参数化注入"));
                }
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

    private String extractSignature(String line) {
        int parenStart = line.indexOf('(');
        int parenEnd = line.lastIndexOf(')');
        if (parenStart > 0 && parenEnd > parenStart) {
            int braceStart = line.indexOf('{');
            String signature = line.substring(0, parenEnd + 1).trim();
            if (braceStart > 0) {
                signature = line.substring(0, braceStart).trim();
            }
            return signature.replaceAll("\\s+", " ");
        }
        return null;
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

    private String extractArtifactId(String content) {
        String projectContent = content.replaceFirst("(?s)<parent>.*?</parent>", "");
        int start = projectContent.indexOf("<artifactId>");
        if (start == -1) {
            return null;
        }
        start += "<artifactId>".length();
        int end = projectContent.indexOf("</artifactId>", start);
        if (end == -1) {
            return null;
        }
        return projectContent.substring(start, end).trim();
    }

    private List<String> extractDependencies(String content) {
        List<String> dependencies = new ArrayList<>();
        int depStart = content.indexOf("<dependencies>");
        if (depStart == -1) {
            return dependencies;
        }
        int depEnd = content.indexOf("</dependencies>", depStart);
        if (depEnd == -1) {
            return dependencies;
        }

        String depsSection = content.substring(depStart, depEnd);
        int marker = 0;
        while ((marker = depsSection.indexOf("<dependency>", marker)) != -1) {
            int end = depsSection.indexOf("</dependency>", marker);
            if (end == -1) {
                break;
            }
            dependencies.add(depsSection.substring(marker, end + "</dependency>".length()));
            marker = end + "</dependency>".length();
        }
        return dependencies;
    }

    private String extractArtifactIdFromDep(String dependencyBlock) {
        int start = dependencyBlock.indexOf("<artifactId>");
        if (start == -1) {
            return "";
        }
        start += "<artifactId>".length();
        int end = dependencyBlock.indexOf("</artifactId>", start);
        if (end == -1) {
            return "";
        }
        return dependencyBlock.substring(start, end).trim();
    }

    private String extractGroupIdFromDep(String dependencyBlock) {
        int start = dependencyBlock.indexOf("<groupId>");
        if (start == -1) {
            return "";
        }
        start += "<groupId>".length();
        int end = dependencyBlock.indexOf("</groupId>", start);
        if (end == -1) {
            return "";
        }
        return dependencyBlock.substring(start, end).trim();
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

    private void outputText() {
        getLog().info("=== Check Result ===");
        getLog().info("Status: " + (result.passed ? "PASSED" : "FAILED"));
        getLog().info("Issues: " + result.issues.size());

        for (Issue issue : result.issues) {
            getLog().warn("  [" + issue.severity + "][" + issue.source + "] " + issue.description);
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
            File parent = report.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            String json = objectMapper.writeValueAsString(result);
            Files.writeString(report.toPath(), json);
            getLog().info("Report saved to: " + reportFile);
        } catch (Exception e) {
            getLog().error("Failed to save report", e);
        }
    }

    public static class Issue {
        public String type;
        public String severity;
        public String file;
        public int line;
        public String description;
        public String rule;
        public String reference;
        public String source;

        Issue() {
        }
    }

    public static class CheckResult {
        public boolean passed = true;
        public List<Issue> issues = new ArrayList<>();

        void addIssue(String type, String severity, String file, int line, String description,
                      String rule, String reference, String source) {
            Issue issue = new Issue();
            issue.type = type;
            issue.severity = severity;
            issue.file = file;
            issue.line = line;
            issue.description = description;
            issue.rule = rule;
            issue.reference = reference;
            issue.source = source;
            issues.add(issue);
            passed = false;
        }
    }

    private static class NamingIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private NamingIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class DependencyIssue {
        private final String severity;
        private final String description;
        private String file;

        private DependencyIssue(String severity, String description) {
            this.severity = severity;
            this.description = description;
        }
    }

    private static class WebBoundaryIssue {
        private final String severity;
        private final String file;
        private final String description;

        private WebBoundaryIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class ModuleInfoIssue {
        private final String severity;
        private final String file;
        private final String description;

        private ModuleInfoIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class ModuleDescriptor {
        private final String artifactId;
        private final String moduleName;
        private final String modulePath;
        private final String file;

        private ModuleDescriptor(String artifactId, String moduleName, String modulePath, String file) {
            this.artifactId = artifactId;
            this.moduleName = moduleName;
            this.modulePath = modulePath;
            this.file = file;
        }
    }

    private static class RemoteAdapterIssue {
        private final String severity;
        private final String file;
        private final String description;

        private RemoteAdapterIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class ApiContractIssue {
        private final String severity;
        private final String file;
        private final String description;

        private ApiContractIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class PathParamIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private PathParamIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class KvKeyIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private KvKeyIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class PersistenceSchemaIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private PersistenceSchemaIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class TestFixtureIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private TestFixtureIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private enum ModuleType {
        ROOT,
        OTHER,
        APP,
        API,
        SUPPORT,
        CORE,
        STARTER,
        STARTER_REMOTE,
    }
}
