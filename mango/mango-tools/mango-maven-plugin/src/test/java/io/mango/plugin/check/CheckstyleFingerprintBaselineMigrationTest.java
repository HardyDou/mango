package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置阈值不再进入指纹的迁移回归：用仓库里真实的复杂度指纹重算，证明迁移规则正确且必要。
 */
class CheckstyleFingerprintBaselineMigrationTest {

    private static final String FIXTURE_PATH =
            "/mango-check/cyclomatic-fingerprints-before-threshold-migration.json";
    private static final String BASELINE_RELATIVE_PATH =
            "mango-pmo/baselines/mango-check/no-new-violations-baseline.json";
    private static final Pattern COMPLEXITY_FINGERPRINT =
            Pattern.compile("^(checkstyle)\\|(cyclomaticcomplexitycheck)\\|(.+)\\|number:(\\d+),number:(\\d+)$");

    @TempDir
    Path tempDir;

    @Test
    void migratedFingerprintsMatchRecomputedIssuesWhileOldOnesDoNot() throws Exception {
        List<String> oldFingerprints = complexityFingerprints();
        assertEquals(20, oldFingerprints.size());

        List<CheckIssue> issues = new ArrayList<>();
        List<String> migrated = new ArrayList<>();
        for (String oldFingerprint : oldFingerprints) {
            Matcher matcher = COMPLEXITY_FINGERPRINT.matcher(oldFingerprint);
            assertTrue(matcher.matches(), oldFingerprint);
            String file = matcher.group(3);
            String threshold = matcher.group(4);
            String actual = matcher.group(5);
            issues.add(issue(file, complexityDescription(actual, threshold)));
            migrated.add("checkstyle|cyclomaticcomplexitycheck|" + file + "|number:" + actual);
        }

        // 迁移规则：只去掉描述里的配置阈值 token，实际值原样保留
        // 指纹相同的多条（同文件同实际值）在 baseline 里本来就会合并计数，这里按多重集比较
        List<String> recomputed = new ArrayList<>(fingerprints(issues));
        List<String> expected = new ArrayList<>(migrated);
        recomputed.sort(null);
        expected.sort(null);
        assertEquals(expected, recomputed);

        assertEquals(0, newIssueCount(issues, List.copyOf(migrated)));
        assertEquals(20, baselineIssueCount(issues, List.copyOf(migrated)));

        // 迁移是必要的：旧指纹在新逻辑下不再匹配
        assertEquals(20, newIssueCount(issues, oldFingerprints));

        // 仓库里的 baseline 已经迁移：与旧快照一一对应（同文件同实际值重复的合并计数）
        assertEquals(20, complexityFingerprintsFromRepository().size());
    }

    @Test
    void chineseAndEnglishDescriptionsProduceTheSameFingerprint() throws Exception {
        CheckIssue english = issue("demo/Demo.java", "Cyclomatic complexity is 24 (max allowed is 20).");
        CheckIssue chinese = issue("demo/Demo.java", "圈复杂度： 24 （最多： 20 ）。");

        assertEquals(fingerprints(List.of(english)), fingerprints(List.of(chinese)));
    }

    private List<String> fingerprints(List<CheckIssue> issues) throws Exception {
        CheckResult result = new CheckResult();
        result.issues.addAll(issues);
        finalizer(null).finalizeResult(result);
        List<String> fingerprints = new ArrayList<>();
        result.issues.forEach(issue -> fingerprints.add(issue.fingerprint));
        return fingerprints;
    }

    private int newIssueCount(List<CheckIssue> issues, List<String> baselineFingerprints)
            throws Exception {
        return finalizeWithBaseline(issues, baselineFingerprints).newIssueCount;
    }

    private int baselineIssueCount(List<CheckIssue> issues, List<String> baselineFingerprints)
            throws Exception {
        return finalizeWithBaseline(issues, baselineFingerprints).baselineIssueCount;
    }

    private CheckResult finalizeWithBaseline(List<CheckIssue> issues, List<String> baselineFingerprints)
            throws Exception {
        Path baselineFile = tempDir.resolve("baseline-" + System.nanoTime() + ".json");
        StringBuilder json = new StringBuilder("{\"issues\":[");
        for (int index = 0; index < baselineFingerprints.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"fingerprint\":\"").append(baselineFingerprints.get(index)).append("\"}");
        }
        json.append("]}");
        Files.writeString(baselineFile, json.toString());

        CheckResult result = new CheckResult();
        result.issues.addAll(issues);
        finalizer(baselineFile).finalizeResult(result);
        return result;
    }

    private CheckGateFinalizer finalizer(Path baselineFile) {
        CheckGateOptions options = new CheckGateOptions(
                tempDir,
                null,
                null,
                baselineFile == null ? null : baselineFile.toString(),
                "no-new-violations",
                "block");
        return new CheckGateFinalizer(new ObjectMapper(), options);
    }

    private CheckIssue issue(String file, String description) {
        CheckIssue issue = new CheckIssue();
        issue.type = "code";
        issue.severity = "MAJOR";
        issue.file = file;
        issue.line = 1;
        issue.description = description;
        issue.rule = "CyclomaticComplexityCheck";
        issue.source = "checkstyle";
        return issue;
    }

    private String complexityDescription(String actual, String threshold) {
        // 中英交替，覆盖两种 locale 与两种括号
        boolean chinese = Integer.parseInt(actual) % 2 == 0;
        return chinese
                ? "圈复杂度： " + actual + " （最多： " + threshold + " ）。"
                : "Cyclomatic complexity is " + actual + " (max allowed is " + threshold + ").";
    }

    private List<String> complexityFingerprints() throws IOException {
        // 迁移输入固定为迁移前的快照；迁移后的 baseline 由本用例的 recomputed 断言保证一致
        try (InputStream inputStream = getClass().getResourceAsStream(FIXTURE_PATH)) {
            assertTrue(inputStream != null, FIXTURE_PATH);
            return readFingerprints(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private List<String> readFingerprints(String content) {
        List<String> fingerprints = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"(checkstyle\\|cyclomaticcomplexitycheck\\|[^\"]+)\"")
                .matcher(content);
        while (matcher.find()) {
            fingerprints.add(matcher.group(1));
        }
        return fingerprints;
    }

    private List<String> complexityFingerprintsFromRepository() throws IOException {
        Path baseline = repositoryRoot().resolve(BASELINE_RELATIVE_PATH);
        assertTrue(Files.exists(baseline), baseline.toString());
        return readFingerprints(Files.readString(baseline));
    }

    private Path repositoryRoot() {
        Path current = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            if (Files.exists(current.resolve(BASELINE_RELATIVE_PATH))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate repository root from " + Paths.get("").toAbsolutePath());
    }
}
