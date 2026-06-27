package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

class CheckGateFinalizer {
    private static final String GATE_ALL = "all";
    private static final String GATE_NO_NEW_VIOLATIONS = "no-new-violations";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_INCONCLUSIVE = "INCONCLUSIVE";
    private static final String POLICY_BLOCK = "block";
    private static final String POLICY_REPORT = "report";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_RULE = "rule";
    private static final String PATH_SEPARATOR = "/";
    private static final String CURRENT_DIR_PREFIX = "./";
    private static final String CHANGED_FILE_SEPARATOR = ",";
    private static final String LINE_SEPARATOR_PATTERN = "\\R";
    private static final Set<String> FILE_LEVEL_COUNT_RULES = Set.of("filelengthcheck");
    private static final Set<String> REPOSITORY_ROOT_SEGMENTS = Set.of(
            "mango-parent",
            "mango-common",
            "mango-tools",
            "mango-infra",
            "mango-platform",
            "mango-admin-starter",
            "mango-app",
            "mango-extension");
    private static final Consumer<String> NO_WARNING_SINK = warning -> {
    };

    private final ObjectMapper objectMapper;
    private final CheckGateOptions options;
    private final Consumer<String> warningSink;

    CheckGateFinalizer(ObjectMapper objectMapper, CheckGateOptions options) {
        this(objectMapper, options, NO_WARNING_SINK);
    }

    CheckGateFinalizer(ObjectMapper objectMapper, CheckGateOptions options, Consumer<String> warningSink) {
        this.objectMapper = objectMapper;
        this.options = options;
        this.warningSink = warningSink;
    }

    void initializeResultOptions(CheckResult result) {
        result.gate = normalizeGate();
        result.staticFailurePolicy = normalizeStaticFailurePolicy();
    }

    void finalizeResult(CheckResult result) throws MojoExecutionException {
        initializeResultOptions(result);
        result.totalIssueCount = result.issues.size();
        result.toolFailureCount = result.toolFailures.size();
        result.excludedIssueCount = result.excludedIssues.size();
        result.issuesBySource = countIssuesByField(result, FIELD_SOURCE);
        result.issuesByRule = countIssuesByField(result, FIELD_RULE);
        result.excludedIssuesBySource = countIssuesByField(result.excludedIssues, FIELD_SOURCE);
        result.excludedIssuesByRule = countIssuesByField(result.excludedIssues, FIELD_RULE);

        if (isNoNewViolationsGate(result)) {
            finalizeNoNewViolations(result);
            return;
        }

        finalizeAllIssuesGate(result);
    }

    boolean shouldReportStaticFailure(CheckResult result) {
        return "report".equalsIgnoreCase(result.staticFailurePolicy);
    }

    private void finalizeNoNewViolations(CheckResult result) throws MojoExecutionException {
        Set<String> changedFileSet = resolveChangedFiles(result);
        result.changedFiles.addAll(changedFileSet);
        Set<String> baselineFingerprints = loadBaselineFingerprints(result);
        boolean hasBaseline = !baselineFingerprints.isEmpty();
        for (CheckIssue issue : result.issues) {
            issue.fingerprint = fingerprint(issue);
            issue.inChangedFiles = isChangedIssue(issue, changedFileSet);
            issue.baseline = baselineFingerprints.contains(issue.fingerprint)
                    || baselineFingerprints.contains(stableFingerprint(issue));
            if (isNewIssue(issue, changedFileSet, hasBaseline)) {
                result.newIssues.add(issue);
            } else {
                result.baselineIssues.add(issue);
            }
        }
        result.newIssueCount = result.newIssues.size();
        result.baselineIssueCount = result.baselineIssues.size();
        result.passed = result.newIssues.isEmpty() && !hasBlockingToolFailure(result);
        result.gateStatus = statusFor(result.passed);
        if (!hasBaseline && changedFileSet.isEmpty()) {
            result.gateStatus = STATUS_INCONCLUSIVE;
            result.passed = false;
            result.addGateMessage("no-new-violations gate requires changed files; set "
                    + "-Dmango.check.changedFiles, -Dmango.check.baseRef or -Dmango.check.baselineFile");
        }
        applyToolFailureMessages(result);
    }

    private boolean isNewIssue(CheckIssue issue, Set<String> changedFileSet, boolean hasBaseline) {
        if (hasBaseline) {
            if (issue.baseline) {
                return false;
            }
            return changedFileSet.isEmpty() || issue.inChangedFiles;
        }
        return issue.inChangedFiles;
    }

    private void finalizeAllIssuesGate(CheckResult result) {
        result.newIssues.addAll(result.issues);
        result.newIssueCount = result.newIssues.size();
        result.baselineIssueCount = 0;
        result.passed = result.issues.isEmpty() && !hasBlockingToolFailure(result);
        result.gateStatus = statusFor(result.passed);
        applyToolFailureMessages(result);
    }

    private String statusFor(boolean passed) {
        if (passed) {
            return STATUS_PASS;
        }
        return STATUS_FAIL;
    }

    private void applyToolFailureMessages(CheckResult result) {
        if (hasBlockingToolFailure(result)) {
            result.gateStatus = STATUS_INCONCLUSIVE;
            result.addGateMessage("static analysis has blocking tool failure(s)");
        }
        if (hasReportedToolFailure(result)) {
            result.addGateMessage("static analysis has reported tool failure(s)");
            if (result.passed) {
                result.gateStatus = STATUS_INCONCLUSIVE;
            }
        }
    }

    private boolean isNoNewViolationsGate(CheckResult result) {
        return GATE_NO_NEW_VIOLATIONS.equalsIgnoreCase(result.gate);
    }

    private String normalizeGate() {
        String gate = options.gate();
        if (gate == null || gate.isBlank()) {
            return GATE_ALL;
        }
        String normalized = gate.trim().toLowerCase(Locale.ROOT);
        if (GATE_NO_NEW_VIOLATIONS.equals(normalized) || GATE_ALL.equals(normalized)) {
            return normalized;
        }
        warningSink.accept("Unknown mango.check.gate: " + gate + "; fallback to all");
        return GATE_ALL;
    }

    private String normalizeStaticFailurePolicy() {
        String staticFailurePolicy = options.staticFailurePolicy();
        if (staticFailurePolicy == null || staticFailurePolicy.isBlank()) {
            return POLICY_BLOCK;
        }
        String normalized = staticFailurePolicy.trim().toLowerCase(Locale.ROOT);
        if (POLICY_REPORT.equals(normalized) || POLICY_BLOCK.equals(normalized)) {
            return normalized;
        }
        warningSink.accept("Unknown mango.check.staticFailurePolicy: " + staticFailurePolicy + "; fallback to block");
        return POLICY_BLOCK;
    }

    private boolean hasBlockingToolFailure(CheckResult result) {
        return hasToolFailure(result) && !shouldReportStaticFailure(result);
    }

    private boolean hasReportedToolFailure(CheckResult result) {
        return hasToolFailure(result) && shouldReportStaticFailure(result);
    }

    private boolean hasToolFailure(CheckResult result) {
        return !result.toolFailures.isEmpty();
    }

    private Set<String> resolveChangedFiles(CheckResult result) throws MojoExecutionException {
        String changedFiles = options.changedFiles();
        if (changedFiles != null && !changedFiles.isBlank()) {
            return changedFilesFromParameter(changedFiles);
        }
        String baseRef = options.baseRef();
        if (baseRef == null || baseRef.isBlank() || options.basePath() == null) {
            return new LinkedHashSet<>();
        }
        return changedFilesFromGit(result, baseRef);
    }

    private Set<String> changedFilesFromParameter(String changedFiles) {
        LinkedHashSet<String> files = new LinkedHashSet<>();
        for (String changedFile : changedFiles.split(CHANGED_FILE_SEPARATOR)) {
            addNormalizedFile(files, changedFile);
        }
        return files;
    }

    private Set<String> changedFilesFromGit(CheckResult result, String baseRef) throws MojoExecutionException {
        LinkedHashSet<String> files = new LinkedHashSet<>();
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "--name-only", baseRef.trim() + "...HEAD");
        processBuilder.directory(options.basePath().toFile());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                result.addGateMessage("failed to resolve changed files from " + baseRef + ": " + output.trim());
                return files;
            }
            for (String line : output.split(LINE_SEPARATOR_PATTERN)) {
                addNormalizedFile(files, line);
            }
            return files;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to resolve changed files from git diff", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while resolving changed files from git diff", e);
        }
    }

    private void addNormalizedFile(Set<String> files, String file) {
        String normalized = normalizePath(file);
        if (!normalized.isBlank()) {
            files.add(normalized);
        }
    }

    private Set<String> loadBaselineFingerprints(CheckResult result) throws MojoExecutionException {
        LinkedHashSet<String> fingerprints = new LinkedHashSet<>();
        Path baselinePath = baselinePath();
        if (baselinePath == null) {
            return fingerprints;
        }
        if (!Files.exists(baselinePath)) {
            result.addGateMessage("baseline file does not exist: " + baselinePath);
            return fingerprints;
        }
        try {
            CheckResult baseline = objectMapper.readValue(baselinePath.toFile(), CheckResult.class);
            if (baseline.issues != null) {
                for (CheckIssue issue : baseline.issues) {
                    fingerprints.add(fingerprintForBaseline(issue));
                    fingerprints.add(stableFingerprint(issue));
                }
            }
            return fingerprints;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read mango check baseline: " + baselinePath, e);
        }
    }

    private Path baselinePath() {
        String baselineFile = options.baselineFile();
        if (baselineFile == null || baselineFile.isBlank()) {
            return null;
        }
        Path baselinePath = Paths.get(baselineFile);
        if (!baselinePath.isAbsolute() && options.basePath() != null) {
            return options.basePath().resolve(baselinePath);
        }
        return baselinePath;
    }

    private String fingerprintForBaseline(CheckIssue issue) {
        if (issue.fingerprint == null || issue.fingerprint.isBlank()) {
            return fingerprint(issue);
        }
        return issue.fingerprint;
    }

    private boolean isChangedIssue(CheckIssue issue, Set<String> changedFileSet) {
        if (changedFileSet.isEmpty()) {
            return false;
        }
        String issueFile = normalizeIssueFile(issue.file);
        if (issueFile.isBlank()) {
            return true;
        }
        for (String changedFile : changedFileSet) {
            if (samePathOrSuffix(issueFile, changedFile)) {
                return true;
            }
        }
        return false;
    }

    private boolean samePathOrSuffix(String issueFile, String changedFile) {
        return issueFile.equals(changedFile)
                || issueFile.endsWith(PATH_SEPARATOR + changedFile)
                || changedFile.endsWith(PATH_SEPARATOR + issueFile);
    }

    private String fingerprint(CheckIssue issue) {
        return String.join("|",
                safeLower(issue.source),
                safeLower(issue.rule),
                normalizeIssueFile(issue.file),
                Integer.toString(issue.line),
                normalizeFingerprintText(issue.description));
    }

    private String stableFingerprint(CheckIssue issue) {
        return String.join("|",
                safeLower(issue.source),
                safeLower(issue.rule),
                normalizeIssueFile(issue.file),
                stableFingerprintText(issue));
    }

    private String stableFingerprintText(CheckIssue issue) {
        String text = normalizeFingerprintText(issue.description);
        if (FILE_LEVEL_COUNT_RULES.contains(safeLower(issue.rule))) {
            return text.replaceAll("\\d+", "#");
        }
        return text;
    }

    private String normalizeFingerprintText(String value) {
        if (value == null) {
            return "";
        }
        return normalizeWhitespace(value).toLowerCase(Locale.ROOT);
    }

    private String safeLower(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIssueFile(String file) {
        String normalized = normalizePath(file);
        if (options.basePath() == null || normalized.isBlank()) {
            return normalized;
        }
        String root = normalizePath(options.basePath().toAbsolutePath().normalize().toString());
        if (normalized.startsWith(root + PATH_SEPARATOR)) {
            return normalized.substring(root.length() + 1);
        }
        return repositoryRelativePath(normalized);
    }

    private String repositoryRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String[] segments = path.split(PATH_SEPARATOR);
        for (int i = 0; i < segments.length; i++) {
            if (REPOSITORY_ROOT_SEGMENTS.contains(segments[i])) {
                return joinSegments(segments, i);
            }
        }
        return path;
    }

    private String joinSegments(String[] segments, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < segments.length; i++) {
            if (segments[i] == null || segments[i].isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(PATH_SEPARATOR);
            }
            builder.append(segments[i]);
        }
        return builder.toString();
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith(CURRENT_DIR_PREFIX)) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private Map<String, Integer> countIssuesByField(CheckResult result, String fieldName) {
        return countIssuesByField(result.issues, fieldName);
    }

    private Map<String, Integer> countIssuesByField(Iterable<CheckIssue> issues, String fieldName) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (CheckIssue issue : issues) {
            String key = issueField(issue, fieldName);
            if (key == null || key.isBlank()) {
                key = "unknown";
            }
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    private String issueField(CheckIssue issue, String fieldName) {
        if (FIELD_SOURCE.equals(fieldName)) {
            return issue.source;
        }
        return issue.rule;
    }
}
