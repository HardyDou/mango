package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CheckMojo 单元测试
 */
class CheckMojoTest {

    @TempDir
    Path tempDir;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void governedFullScopeRejectsChangedOnlyOverride() throws Exception {
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "requireFullScope", true);
        setField(mojo, "changedOnly", true);
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(exception.getMessage().contains("changedOnly=true is forbidden"));
    }

    @Test
    void governedFullScopeRejectsExcludedModulesOverride() throws Exception {
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "requireFullScope", true);
        setField(mojo, "codeLevelExcludedModules", "mango-demo-core");
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(exception.getMessage().contains("codeLevelExcludedModules is forbidden"));
    }

    @Test
    void governedFullScopeRequiresEveryJavaModuleStaticReportAndIgnoresPomOnlyModules()
            throws Exception {
        MavenProject moduleA = reactorProject("modules/a", "module-a", true);
        MavenProject moduleB = reactorProject("modules/b", "module-b", true);
        MavenProject pomOnly = reactorProject("aggregator", "aggregator", false);
        MavenSession reactorSession = mock(MavenSession.class);
        when(reactorSession.getProjects()).thenReturn(List.of(moduleA, moduleB, pomOnly));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "requireFullScope", true);
        setField(mojo, "session", reactorSession);
        for (Map.Entry<String, String> tool : Map.of(
                "pmd", "pmd.xml",
                "checkstyle", "checkstyle-result.xml",
                "spotbugs", "spotbugsXml.xml").entrySet()) {
            Path reportA = Path.of(moduleA.getBuild().getDirectory()).resolve(tool.getValue());
            Path reportB = Path.of(moduleB.getBuild().getDirectory()).resolve(tool.getValue());
            Files.createDirectories(reportA.getParent());
            Files.writeString(reportA, "<report/>\n");

            MojoExecutionException missing = assertThrows(
                    MojoExecutionException.class,
                    () -> mojo.requireStaticReports(tempDir, tool.getValue(), tool.getKey()));
            assertTrue(missing.getMessage().contains("module-b"));
            assertFalse(missing.getMessage().contains("aggregator"));

            Files.createDirectories(reportB.getParent());
            Files.writeString(reportB, "<report/>\n");
            assertEquals(2,
                    mojo.requireStaticReports(tempDir, tool.getValue(), tool.getKey()).size());
        }
    }

    @Test
    void governedFullScopeStaticReportsFailClosedWithoutReactorSession() throws Exception {
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "requireFullScope", true);
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class,
                () -> mojo.requireStaticReports(tempDir, "pmd.xml", "pmd"));
        assertTrue(exception.getMessage().contains("requires the Maven Reactor session"));
    }

    private MavenProject reactorProject(String relativePath, String artifactId, boolean withJava)
            throws Exception {
        Path basedir = tempDir.resolve(relativePath);
        Files.createDirectories(basedir);
        Path pom = basedir.resolve("pom.xml");
        Files.writeString(pom, "<project/>\n");
        Build build = new Build();
        build.setDirectory(basedir.resolve("target").toString());
        if (withJava) {
            Path sourceRoot = basedir.resolve("src/main/java");
            Files.createDirectories(sourceRoot.resolve("example"));
            Files.writeString(sourceRoot.resolve("example/Demo.java"),
                    "package example; final class Demo {}\n");
            build.setSourceDirectory(sourceRoot.toString());
        }
        MavenProject project = new MavenProject();
        project.setFile(pom.toFile());
        project.setArtifactId(artifactId);
        project.setBuild(build);
        if (withJava) {
            project.addCompileSourceRoot(build.getSourceDirectory());
        }
        return project;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> applyAlterTableStatementToColumns(String statement,
                                                                  Map<String, String> columns) throws Exception {
        CheckMojo mojo = new CheckMojo();
        Class<?> schemaClass = Class.forName("io.mango.plugin.check.CheckMojo$PersistenceTableSchema");
        var constructor = schemaClass.getDeclaredConstructor(String.class, String.class, int.class, Map.class,
                boolean.class);
        constructor.setAccessible(true);
        Object schema = constructor.newInstance("demo_user", "V1__init_demo.sql", 1, columns, true);
        Method method = CheckMojo.class.getDeclaredMethod("applyAlterTableStatement", String.class, schemaClass);
        method.setAccessible(true);
        method.invoke(mojo, statement, schema);
        Field columnDefinitions = schemaClass.getDeclaredField("columnDefinitions");
        columnDefinitions.setAccessible(true);
        return (Map<String, String>) columnDefinitions.get(schema);
    }

    private void createStarterModule(String artifactId, String moduleName,
                                     String modulePath, String controllerPath) throws Exception {
        Path starterDir = tempDir.resolve(artifactId);
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.createDirectories(starterDir.resolve("src/main/java/io/mango/demo/starter"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0</version>
                </project>
                """.formatted(artifactId));
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=%s
                module-path=%s
                """.formatted(moduleName, modulePath));
        Files.writeString(starterDir.resolve("src/main/java/io/mango/demo/starter/DemoController.java"), """
                package io.mango.demo.starter;

                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("%s")
                public class DemoController {
                }
                """.formatted(controllerPath));
    }

    @Test
    void checkNaming_ruleProvided_executesSuccessfully() throws Exception {
        // given
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // when & then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkNaming_withNonKebabArtifactId_reportsIssue() throws Exception {
        // given
        Path pomFile = tempDir.resolve("mango-demo/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>io.mango</groupId>
                        <artifactId>mango-parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>mangoDemoCore</artifactId>
                </project>
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkNaming_noNewViolationsWithHistoricalIssue_passes() throws Exception {
        // given
        Path pomFile = tempDir.resolve("mango-demo/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <artifactId>mangoDemoCore</artifactId>
                </project>
                """);
        Path reportFile = tempDir.resolve("target/no-new-report.json");

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "gate", "no-new-violations");
        setField(mojo, "changedFiles", "mango-demo/src/main/java/io/mango/demo/NewService.java");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "reportFile", reportFile.toString());
        setField(mojo, "session", null);

        // when & then
        assertDoesNotThrow(() -> mojo.execute());
        String report = Files.readString(reportFile);
        assertTrue(report.contains("\"gateStatus\" : \"PASS\""));
        assertTrue(report.contains("\"newIssueCount\" : 0"));
        assertTrue(report.contains("\"baselineIssueCount\" : 1"));
    }

    @Test
    void checkNaming_noNewViolationsWithChangedIssue_fails() throws Exception {
        // given
        Path pomFile = tempDir.resolve("mango-demo/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <artifactId>mangoDemoCore</artifactId>
                </project>
                """);
        Path reportFile = tempDir.resolve("target/no-new-failed-report.json");

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "gate", "no-new-violations");
        setField(mojo, "changedFiles", "mango-demo/pom.xml");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "reportFile", reportFile.toString());
        setField(mojo, "session", null);

        // when & then
        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().contains("newIssues=1"));
        String report = Files.readString(reportFile);
        assertTrue(report.contains("\"gateStatus\" : \"FAIL\""));
        assertTrue(report.contains("\"newIssueCount\" : 1"));
    }

    @Test
    void checkNaming_noNewViolationsWithBaselineFingerprint_passes() throws Exception {
        // given
        Path pomFile = tempDir.resolve("mango-demo/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <artifactId>mangoDemoCore</artifactId>
                </project>
                """);
        Path baselineFile = tempDir.resolve("target/baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, """
                {
                  "issues" : [ {
                    "type" : "NAMING",
                    "severity" : "MAJOR",
                    "file" : "%s",
                    "line" : 3,
                    "description" : "Mango module artifactId must use kebab-case: mangoDemoCore",
                    "rule" : "NAMING",
                    "reference" : "naming-rules.md",
                    "source" : "mango-check"
                  } ]
                }
                """.formatted(pomFile.toString().replace("\\", "\\\\")));
        Path reportFile = tempDir.resolve("target/no-new-baseline-report.json");

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "gate", "no-new-violations");
        setField(mojo, "changedFiles", "mango-demo/pom.xml");
        setField(mojo, "baselineFile", baselineFile.toString());
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "reportFile", reportFile.toString());
        setField(mojo, "session", null);

        // when & then
        assertDoesNotThrow(() -> mojo.execute());
        String report = Files.readString(reportFile);
        assertTrue(report.contains("\"gateStatus\" : \"PASS\""));
        assertTrue(report.contains("\"newIssueCount\" : 0"));
        assertTrue(report.contains("\"baseline\" : true"));
    }

    @Test
    void finalizeResult_noNewViolationsWithBaselineDoesNotRequireChangedFiles() throws Exception {
        // given
        CheckIssue issue = new CheckIssue();
        issue.type = "NAMING";
        issue.severity = "MAJOR";
        issue.file = tempDir.resolve("mango-demo/pom.xml").toString();
        issue.line = 3;
        issue.description = "Mango module artifactId must use kebab-case: mangoDemoCore";
        issue.rule = "NAMING";
        issue.reference = "naming-rules.md";
        issue.source = "mango-check";

        CheckResult baseline = new CheckResult();
        baseline.issues.add(issue);
        Path baselineFile = tempDir.resolve("target/baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        result.issues.add(issue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, null, null, baselineFile.toString(),
                        "no-new-violations", "block"));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertTrue(result.passed);
        assertEquals("PASS", result.gateStatus);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
        assertFalse(result.gateMessages.contains("no-new-violations gate requires changed files; set "
                + "-Dmango.check.changedFiles, -Dmango.check.baseRef or -Dmango.check.baselineFile"));
    }

    @Test
    void finalizeResult_changedHardRulesCannotBeWaivedByBaseline() throws Exception {
        Path changedFile = tempDir.resolve(
                "mango-demo-api/src/main/java/io/mango/demo/api/DemoEntity.java");
        Files.createDirectories(changedFile.getParent());
        Files.writeString(changedFile, "class DemoEntity {}\n");

        for (String hardRule : List.of("MODULE_INFO")) {
            CheckIssue issue = new CheckIssue();
            issue.type = hardRule;
            issue.severity = "CRITICAL";
            issue.file = changedFile.toString();
            issue.line = 1;
            issue.description = "hard rule violation: " + hardRule;
            issue.rule = hardRule;
            issue.reference = "api-rules.md";
            issue.source = "mango-check";

            CheckResult baseline = new CheckResult();
            baseline.issues.add(issue);
            Path baselineFile = tempDir.resolve("target/hard-rule-baseline-" + hardRule + ".json");
            Files.createDirectories(baselineFile.getParent());
            Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

            CheckResult result = new CheckResult();
            result.issues.add(issue);
            CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                    new CheckGateOptions(tempDir,
                            "mango-demo-api/src/main/java/io/mango/demo/api/DemoEntity.java",
                            null, baselineFile.toString(), "no-new-violations", "block"));

            assertDoesNotThrow(() -> finalizer.finalizeResult(result), hardRule);
            assertFalse(result.passed, hardRule);
            assertEquals("FAIL", result.gateStatus, hardRule);
            assertEquals(1, result.newIssueCount, hardRule);
        }
    }

    @Test
    void finalizeResult_noNewViolationsWithBaselineMarksUnmatchedIssueAsNewWithoutChangedFiles() throws Exception {
        // given
        CheckResult baseline = new CheckResult();
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.type = "NAMING";
        baselineIssue.severity = "MAJOR";
        baselineIssue.file = tempDir.resolve("mango-demo/pom.xml").toString();
        baselineIssue.line = 3;
        baselineIssue.description = "Mango module artifactId must use kebab-case: mangoDemoCore";
        baselineIssue.rule = "NAMING";
        baselineIssue.reference = "naming-rules.md";
        baselineIssue.source = "mango-check";
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckIssue newIssue = new CheckIssue();
        newIssue.type = "NAMING";
        newIssue.severity = "MAJOR";
        newIssue.file = tempDir.resolve("mango-new/pom.xml").toString();
        newIssue.line = 3;
        newIssue.description = "Mango module artifactId must use kebab-case: mangoNewCore";
        newIssue.rule = "NAMING";
        newIssue.reference = "naming-rules.md";
        newIssue.source = "mango-check";
        CheckResult result = new CheckResult();
        result.issues.add(newIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, null, null, baselineFile.toString(),
                        "no-new-violations", "block"));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertFalse(result.passed);
        assertEquals("FAIL", result.gateStatus);
        assertEquals(1, result.newIssueCount);
        assertEquals(0, result.baselineIssueCount);
    }

    @Test
    void finalizeResult_changedOnlyWithBaselineIgnoresUnmatchedIssueOutsideChangedFiles() throws Exception {
        // given
        CheckIssue baselineIssue = issue("mango-existing/pom.xml", "mangoExistingCore");
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/changed-only-baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        result.issues.add(issue("mango-historical/pom.xml", "mangoHistoricalCore"));
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, "mango-changed/Changed.java", null, baselineFile.toString(),
                        "no-new-violations", "block", true));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertTrue(result.passed);
        assertEquals("PASS", result.gateStatus);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
        assertFalse(result.baselineIssues.get(0).baseline);
        assertFalse(result.baselineIssues.get(0).inChangedFiles);
    }

    @Test
    void finalizeResult_changedOnlyWithoutScopeSourceFailsClosed() throws Exception {
        CheckResult baseline = new CheckResult();
        baseline.issues.add(issue("mango-existing/pom.xml", "mangoExistingCore"));
        Path baselineFile = tempDir.resolve("target/missing-scope-baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        result.issues.add(issue("mango-historical/pom.xml", "mangoHistoricalCore"));
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, null, null, baselineFile.toString(),
                        "no-new-violations", "block", true));

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class, () -> finalizer.finalizeResult(result));
        assertTrue(exception.getMessage().contains("changedOnly=true requires"));
    }

    @Test
    void finalizeResult_configuredMissingBaselineFailsClosed() {
        CheckResult result = new CheckResult();
        result.issues.add(issue("mango-historical/pom.xml", "mangoHistoricalCore"));
        Path missingBaseline = tempDir.resolve("target/missing-baseline.json");
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, "mango-changed/Changed.java", null,
                        missingBaseline.toString(), "no-new-violations", "block", true));

        MojoExecutionException exception = assertThrows(
                MojoExecutionException.class, () -> finalizer.finalizeResult(result));
        assertTrue(exception.getMessage().contains("baseline file does not exist"));
    }

    @Test
    void checkNaming_changedOnlyWithTrustedEmptyGitDiffPassesThroughMojo() throws Exception {
        Path pomFile = tempDir.resolve("mango-demo/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <artifactId>mangoDemoCore</artifactId>
                </project>
                """);
        runGit("init", "-q");
        runGit("config", "user.name", "Mango Test");
        runGit("config", "user.email", "mango-test@example.invalid");
        runGit("add", ".");
        runGit("commit", "-qm", "baseline");

        CheckResult baseline = new CheckResult();
        baseline.issues.add(issue("mango-existing/pom.xml", "mangoExistingCore"));
        Path baselineFile = tempDir.resolve("baseline.json");
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "gate", "no-new-violations");
        setField(mojo, "changedOnly", true);
        setField(mojo, "baseRef", "HEAD");
        setField(mojo, "baselineFile", baselineFile.toString());
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void finalizeResult_changedOnlyWithBaselineRejectsUnmatchedIssueInChangedFile() throws Exception {
        // given
        CheckIssue baselineIssue = issue("mango-existing/pom.xml", "mangoExistingCore");
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/changed-only-with-change-baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        result.issues.add(issue("mango-changed/pom.xml", "mangoChangedCore"));
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, "mango-changed/pom.xml", null, baselineFile.toString(),
                        "no-new-violations", "block", true));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertFalse(result.passed);
        assertEquals("FAIL", result.gateStatus);
        assertEquals(1, result.newIssueCount);
        assertEquals(0, result.baselineIssueCount);
        assertTrue(result.newIssues.get(0).inChangedFiles);
    }

    @Test
    void finalizeResult_noNewViolationsMatchesBaselineAcrossWorktreeRoots() throws Exception {
        // given
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.type = "MagicNumberCheck";
        baselineIssue.severity = "MINOR";
        baselineIssue.file = "/workspace/mango/mango/mango-platform/mango-notice/src/main/java/Demo.java";
        baselineIssue.line = 12;
        baselineIssue.description = "magic number";
        baselineIssue.rule = "MagicNumberCheck";
        baselineIssue.reference = "auto-check-mapping.md";
        baselineIssue.source = "checkstyle";
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckIssue currentIssue = new CheckIssue();
        currentIssue.type = "MagicNumberCheck";
        currentIssue.severity = "MINOR";
        currentIssue.file = "/workspace/mango-issue-205/mango/mango-platform/mango-notice/src/main/java/Demo.java";
        currentIssue.line = 12;
        currentIssue.description = "magic number";
        currentIssue.rule = "MagicNumberCheck";
        currentIssue.reference = "auto-check-mapping.md";
        currentIssue.source = "checkstyle";
        CheckResult result = new CheckResult();
        result.issues.add(currentIssue);
        Path currentRoot = Path.of("/workspace/mango-issue-205/mango");
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(currentRoot, null, null, baselineFile.toString(),
                        "no-new-violations", "block"));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertTrue(result.passed);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
    }

    @Test
    void finalizeResult_allGatePersistsStableRepositoryRelativeFingerprint() throws Exception {
        Path projectRoot = tempDir.resolve("baohan-system/baohan-backend");
        CheckIssue issue = new CheckIssue();
        issue.type = "MagicNumberCheck";
        issue.severity = "MINOR";
        issue.file = projectRoot.resolve("app/src/main/java/com/example/Demo.java").toString();
        issue.line = 12;
        issue.description = "'5' is a magic number.";
        issue.rule = "MagicNumberCheck";
        issue.reference = "auto-check-mapping.md";
        issue.source = "checkstyle";
        CheckIssue excludedIssue = new CheckIssue();
        excludedIssue.file = projectRoot.resolve(
                "app/src/main/java/com/example/ExcludedDemo.java").toString();
        excludedIssue.description = "'8' is a magic number.";
        excludedIssue.rule = "MagicNumberCheck";
        excludedIssue.source = "checkstyle";
        CheckResult result = new CheckResult();
        result.issues.add(issue);
        result.excludedIssues.add(excludedIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(projectRoot, null, null, null, "all", "block"));

        finalizer.finalizeResult(result);

        assertEquals(
                "checkstyle|magicnumbercheck|app/src/main/java/com/example/Demo.java|literal:5",
                issue.fingerprint);
        assertEquals(
                "checkstyle|magicnumbercheck|app/src/main/java/com/example/ExcludedDemo.java|literal:8",
                excludedIssue.fingerprint);
        assertSame(issue, result.newIssues.get(0));
    }

    @Test
    void finalizeResult_noNewViolationsUsesPersistedFingerprintAcrossBusinessWorktrees()
            throws Exception {
        Path baselineRoot = tempDir.resolve("baohan-system/baohan-backend");
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.type = "MagicNumberCheck";
        baselineIssue.severity = "MINOR";
        baselineIssue.file = baselineRoot.resolve(
                "app/src/main/java/com/yunxinbaokeji/baohan/app/service/UserService.java").toString();
        baselineIssue.line = 12;
        baselineIssue.description = "'5' is a magic number.";
        baselineIssue.rule = "MagicNumberCheck";
        baselineIssue.reference = "auto-check-mapping.md";
        baselineIssue.source = "checkstyle";
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(baselineRoot, null, null, null, "all", "block"))
                .finalizeResult(baseline);
        Path baselineFile = tempDir.resolve("target/baohan-baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        Path currentRoot = tempDir.resolve("baohan-system-latest-upgrade/baohan-backend");
        CheckIssue currentIssue = new CheckIssue();
        currentIssue.type = "MagicNumberCheck";
        currentIssue.severity = "MINOR";
        currentIssue.file = currentRoot.resolve(
                "app/src/main/java/com/yunxinbaokeji/baohan/app/service/UserService.java").toString();
        currentIssue.line = 18;
        currentIssue.description = "'5' is a magic number.";
        currentIssue.rule = "MagicNumberCheck";
        currentIssue.reference = "auto-check-mapping.md";
        currentIssue.source = "checkstyle";
        CheckResult result = new CheckResult();
        result.issues.add(currentIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(
                        currentRoot,
                        "app/src/main/java/com/yunxinbaokeji/baohan/app/service/UserService.java",
                        null,
                        baselineFile.toString(),
                        "no-new-violations",
                        "block"));

        finalizer.finalizeResult(result);

        assertTrue(result.passed);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
        assertTrue(currentIssue.baseline);
        assertEquals(baselineIssue.fingerprint, currentIssue.fingerprint);
    }

    @Test
    void finalizeResult_noNewViolationsUpgradesLegacyLineFingerprint() throws Exception {
        Path projectRoot = tempDir.resolve("business-backend");
        Path sourceFile = projectRoot.resolve("app/src/main/java/com/example/UserService.java");
        Path baselineFile = tempDir.resolve("legacy-baseline.json");
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.source = "checkstyle";
        baselineIssue.rule = "MagicNumberCheck";
        baselineIssue.file = sourceFile.toString();
        baselineIssue.line = 12;
        baselineIssue.description = "Magic number: '5'";
        baselineIssue.fingerprint = String.join(
                "|",
                "checkstyle",
                "magicnumbercheck",
                "app/src/main/java/com/example/UserService.java",
                "12",
                "magic number: '5'");
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        objectMapper().writeValue(baselineFile.toFile(), baseline);

        CheckIssue currentIssue = new CheckIssue();
        currentIssue.source = "checkstyle";
        currentIssue.rule = "MagicNumberCheck";
        currentIssue.file = sourceFile.toString();
        currentIssue.line = 18;
        currentIssue.description = "Magic number: '5'";
        CheckResult current = new CheckResult();
        current.issues.add(currentIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(
                        projectRoot,
                        null,
                        null,
                        baselineFile.toString(),
                        "no-new-violations",
                        "block"));

        finalizer.finalizeResult(current);

        assertTrue(current.passed);
        assertEquals(0, current.newIssueCount);
        assertEquals(1, current.baselineIssueCount);
        assertEquals(
                "checkstyle|magicnumbercheck|app/src/main/java/com/example/UserService.java|literal:5",
                currentIssue.fingerprint);
    }

    @Test
    void finalizeResult_legacyBusinessBaselineWithUnknownOldRootFailsClosed() throws Exception {
        Path baselineRoot = tempDir.resolve("baohan-system/baohan-backend");
        Path baselineSource = baselineRoot.resolve(
                "app/src/main/java/com/example/UserService.java");
        Path baselineFile = tempDir.resolve("legacy-cross-root-baseline.json");
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.source = "checkstyle";
        baselineIssue.rule = "MagicNumberCheck";
        baselineIssue.file = baselineSource.toString();
        baselineIssue.line = 12;
        baselineIssue.description = "Magic number: '5'";
        baselineIssue.fingerprint = String.join(
                "|",
                "checkstyle",
                "magicnumbercheck",
                "app/src/main/java/com/example/UserService.java",
                "12",
                "magic number: '5'");
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        objectMapper().writeValue(baselineFile.toFile(), baseline);

        Path currentRoot = tempDir.resolve("baohan-system-upgrade/baohan-backend");
        CheckIssue currentIssue = new CheckIssue();
        currentIssue.source = "checkstyle";
        currentIssue.rule = "MagicNumberCheck";
        currentIssue.file = currentRoot.resolve(
                "app/src/main/java/com/example/UserService.java").toString();
        currentIssue.line = 18;
        currentIssue.description = "Magic number: '5'";
        CheckResult current = new CheckResult();
        current.issues.add(currentIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(
                        currentRoot,
                        null,
                        null,
                        baselineFile.toString(),
                        "no-new-violations",
                        "block"));

        finalizer.finalizeResult(current);

        assertFalse(current.passed);
        assertEquals(1, current.newIssueCount);
        assertEquals(0, current.baselineIssueCount);
    }

    @Test
    void finalizeResult_noNewViolationsMatchesBaselineWhenLineDrifts() throws Exception {
        // given
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.type = "MagicNumberCheck";
        baselineIssue.severity = "MINOR";
        baselineIssue.file = tempDir.resolve("mango-platform/mango-notice/src/main/java/Demo.java").toString();
        baselineIssue.line = 12;
        baselineIssue.description = "magic number";
        baselineIssue.rule = "MagicNumberCheck";
        baselineIssue.reference = "auto-check-mapping.md";
        baselineIssue.source = "checkstyle";
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckIssue currentIssue = new CheckIssue();
        currentIssue.type = "MagicNumberCheck";
        currentIssue.severity = "MINOR";
        currentIssue.file = tempDir.resolve("mango-platform/mango-notice/src/main/java/Demo.java").toString();
        currentIssue.line = 18;
        currentIssue.description = "magic number";
        currentIssue.rule = "MagicNumberCheck";
        currentIssue.reference = "auto-check-mapping.md";
        currentIssue.source = "checkstyle";
        CheckResult result = new CheckResult();
        result.issues.add(currentIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, null, null, baselineFile.toString(),
                        "no-new-violations", "block"));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertTrue(result.passed);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
    }

    @Test
    void finalizeResult_checkstyleBaselineMatchesAcrossLocaleAndLineDrift() throws Exception {
        CheckIssue baselineIssue = checkstyleIssue(
                "MagicNumberCheck", 12, "'5' 是一个魔术数字（直接常数）。");
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/localized-checkstyle-baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        result.issues.add(checkstyleIssue("MagicNumberCheck", 18, "'5' is a magic number."));
        CheckGateFinalizer finalizer = new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(
                        tempDir,
                        "mango-platform/mango-notice/src/main/java/Demo.java",
                        null,
                        baselineFile.toString(),
                        "no-new-violations",
                        "block"));

        assertDoesNotThrow(() -> finalizer.finalizeResult(result));
        assertTrue(result.passed);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
    }

    @Test
    void finalizeResult_checkstyleBaselineRejectsDifferentLiteralAcrossLocale() throws Exception {
        CheckResult baseline = new CheckResult();
        baseline.issues.add(checkstyleIssue(
                "MagicNumberCheck", 12, "'5' 是一个魔术数字（直接常数）。"));
        Path baselineFile = tempDir.resolve("target/replaced-checkstyle-baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        result.issues.add(checkstyleIssue("MagicNumberCheck", 18, "'6' is a magic number."));
        CheckGateFinalizer finalizer = new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(
                        tempDir,
                        "mango-platform/mango-notice/src/main/java/Demo.java",
                        null,
                        baselineFile.toString(),
                        "no-new-violations",
                        "block"));

        assertDoesNotThrow(() -> finalizer.finalizeResult(result));
        assertFalse(result.passed);
        assertEquals(1, result.newIssueCount);
        assertEquals(0, result.baselineIssueCount);
    }

    @Test
    void finalizeResult_checkstyleComplexityMatchesAcrossLocale() throws Exception {
        CheckResult baseline = new CheckResult();
        baseline.issues.add(checkstyleIssue(
                "NPathComplexityCheck", 12, "方法分支复杂度： 1,542 （最多： 200 ）。"));
        Path baselineFile = tempDir.resolve("target/localized-complexity-baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        result.issues.add(checkstyleIssue(
                "NPathComplexityCheck",
                18,
                "NPath Complexity is 1,542 (max allowed is 200)."));
        CheckGateFinalizer finalizer = new CheckGateFinalizer(
                objectMapper(),
                new CheckGateOptions(
                        tempDir,
                        "mango-platform/mango-notice/src/main/java/Demo.java",
                        null,
                        baselineFile.toString(),
                        "no-new-violations",
                        "block"));

        assertDoesNotThrow(() -> finalizer.finalizeResult(result));
        assertTrue(result.passed);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
    }

    @Test
    void finalizeResult_noNewViolationsDoesNotReuseOneBaselineForDuplicateStableIssues() throws Exception {
        // given
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.type = "MagicNumberCheck";
        baselineIssue.severity = "MINOR";
        baselineIssue.file = tempDir.resolve("mango-platform/mango-notice/src/main/java/Demo.java").toString();
        baselineIssue.line = 12;
        baselineIssue.description = "magic number";
        baselineIssue.rule = "MagicNumberCheck";
        baselineIssue.reference = "auto-check-mapping.md";
        baselineIssue.source = "checkstyle";
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckResult result = new CheckResult();
        CheckIssue existingIssue = new CheckIssue();
        existingIssue.type = "MagicNumberCheck";
        existingIssue.severity = "MINOR";
        existingIssue.file = tempDir.resolve("mango-platform/mango-notice/src/main/java/Demo.java").toString();
        existingIssue.line = 12;
        existingIssue.description = "magic number";
        existingIssue.rule = "MagicNumberCheck";
        existingIssue.reference = "auto-check-mapping.md";
        existingIssue.source = "checkstyle";
        result.issues.add(existingIssue);

        CheckIssue duplicateIssue = new CheckIssue();
        duplicateIssue.type = "MagicNumberCheck";
        duplicateIssue.severity = "MINOR";
        duplicateIssue.file = tempDir.resolve("mango-platform/mango-notice/src/main/java/Demo.java").toString();
        duplicateIssue.line = 24;
        duplicateIssue.description = "magic number";
        duplicateIssue.rule = "MagicNumberCheck";
        duplicateIssue.reference = "auto-check-mapping.md";
        duplicateIssue.source = "checkstyle";
        result.issues.add(duplicateIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, "mango-platform/mango-notice/src/main/java/Demo.java", null,
                        baselineFile.toString(), "no-new-violations", "block"));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertFalse(result.passed);
        assertEquals("FAIL", result.gateStatus);
        assertEquals(1, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
    }

    private CheckIssue checkstyleIssue(String rule, int line, String description) {
        CheckIssue issue = new CheckIssue();
        issue.type = rule;
        issue.severity = "MINOR";
        issue.file = tempDir.resolve(
                        "mango-platform/mango-notice/src/main/java/Demo.java")
                .toString();
        issue.line = line;
        issue.description = description;
        issue.rule = rule;
        issue.reference = "auto-check-mapping.md";
        issue.source = "checkstyle";
        return issue;
    }

    @Test
    void finalizeResult_noNewViolationsMatchesFileLengthBaselineWhenCountChanges() throws Exception {
        // given
        CheckIssue baselineIssue = new CheckIssue();
        baselineIssue.type = "FileLengthCheck";
        baselineIssue.severity = "MINOR";
        baselineIssue.file = tempDir.resolve("mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/check/CheckMojo.java").toString();
        baselineIssue.line = 1;
        baselineIssue.description = "文件 3,600 行 （最多：2,000 行）。";
        baselineIssue.rule = "FileLengthCheck";
        baselineIssue.reference = "auto-check-mapping.md";
        baselineIssue.source = "checkstyle";
        CheckResult baseline = new CheckResult();
        baseline.issues.add(baselineIssue);
        Path baselineFile = tempDir.resolve("target/baseline.json");
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, objectMapper().writeValueAsString(baseline));

        CheckIssue currentIssue = new CheckIssue();
        currentIssue.type = "FileLengthCheck";
        currentIssue.severity = "MINOR";
        currentIssue.file = tempDir.resolve("mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/check/CheckMojo.java").toString();
        currentIssue.line = 1;
        currentIssue.description = "文件 3,770 行 （最多：2,000 行）。";
        currentIssue.rule = "FileLengthCheck";
        currentIssue.reference = "auto-check-mapping.md";
        currentIssue.source = "checkstyle";
        CheckResult result = new CheckResult();
        result.issues.add(currentIssue);
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, null, null, baselineFile.toString(),
                        "no-new-violations", "block"));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertTrue(result.passed);
        assertEquals(0, result.newIssueCount);
        assertEquals(1, result.baselineIssueCount);
    }

    @Test
    void parseCheckstyleReport_excludesConfiguredCodeLevelModule() throws Exception {
        // given
        Path report = tempDir.resolve("mango-platform/mango-file-preview/target/checkstyle-result.xml");
        Files.createDirectories(report.getParent());
        Files.writeString(report, """
                <?xml version="1.0" encoding="UTF-8"?>
                <checkstyle version="10.0">
                  <file name="%s">
                    <error line="10" severity="warning" message="magic number" source="com.puppycrawl.tools.checkstyle.checks.coding.MagicNumberCheck"/>
                  </file>
                </checkstyle>
                """.formatted(tempDir.resolve("mango-platform/mango-file-preview/src/main/java/Demo.java")));
        CheckMojo mojo = new CheckMojo();
        CheckResult result = new CheckResult();
        setField(mojo, "result", result);
        setField(mojo, "codeLevelExcludedModules", "mango-platform/mango-file-preview");

        Method method = CheckMojo.class.getDeclaredMethod("parseCheckstyleReport", Path.class);
        method.setAccessible(true);

        // when
        method.invoke(mojo, report);

        // then
        assertEquals(0, result.issues.size());
        assertEquals(1, result.excludedIssues.size());
        assertEquals("checkstyle", result.excludedIssues.get(0).source);
    }

    @Test
    void parseCheckstyleReport_keepsUnconfiguredModuleIssue() throws Exception {
        // given
        Path report = tempDir.resolve("mango-platform/mango-notice/target/checkstyle-result.xml");
        Files.createDirectories(report.getParent());
        Files.writeString(report, """
                <?xml version="1.0" encoding="UTF-8"?>
                <checkstyle version="10.0">
                  <file name="%s">
                    <error line="10" severity="warning" message="magic number" source="com.puppycrawl.tools.checkstyle.checks.coding.MagicNumberCheck"/>
                  </file>
                </checkstyle>
                """.formatted(tempDir.resolve("mango-platform/mango-notice/src/main/java/Demo.java")));
        CheckMojo mojo = new CheckMojo();
        CheckResult result = new CheckResult();
        setField(mojo, "result", result);
        setField(mojo, "codeLevelExcludedModules", "mango-platform/mango-file-preview");

        Method method = CheckMojo.class.getDeclaredMethod("parseCheckstyleReport", Path.class);
        method.setAccessible(true);

        // when
        method.invoke(mojo, report);

        // then
        assertEquals(1, result.issues.size());
        assertEquals(0, result.excludedIssues.size());
        assertEquals("checkstyle", result.issues.get(0).source);
    }

    @Test
    void checkMethodLength_genericRule_reportsUnsupported() throws Exception {
        // given - create a file with a long method
        Path javaFile = tempDir.resolve("TestService.java");
        StringBuilder longMethod = new StringBuilder();
        longMethod.append("public class TestService {\n");
        longMethod.append("    public void longMethod() {\n");
        // Add 60 lines (over 50 limit)
        for (int i = 0; i < 60; i++) {
            longMethod.append("        System.out.println(\"line " + i + "\");\n");
        }
        longMethod.append("    }\n");
        longMethod.append("}\n");
        Files.writeString(javaFile, longMethod.toString());

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "method-length");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);
        setField(mojo, "maxMethodLength", 50);

        // then - generic code quality belongs to PMD/P3C/Checkstyle
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkClassLength_genericRule_reportsUnsupported() throws Exception {
        // given - public class declarations must not be counted as methods
        Path javaFile = tempDir.resolve("Response.java");
        StringBuilder source = new StringBuilder();
        source.append("public class Response {\n");
        for (int i = 0; i < 70; i++) {
            source.append("    private String field").append(i).append(";\n");
        }
        source.append("    public static Response ok() {\n");
        source.append("        return new Response();\n");
        source.append("    }\n");
        source.append("    public boolean success()\n");
        source.append("    {\n");
        source.append("        return true;\n");
        source.append("    }\n");
        source.append("}\n");
        Files.writeString(javaFile, source.toString());

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "class-length");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);
        setField(mojo, "maxClassLength", 50);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDuplicate_genericRule_reportsUnsupported() throws Exception {
        // given
        Path javaFile = tempDir.resolve("UniqueService.java");
        String content = """
                public class UniqueService {
                    public void methodA() { }
                    public void methodB() { }
                    public void methodC() { }
                }
                """;
        Files.writeString(javaFile, content);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "duplicate");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkModuleMenu_withAuthMenuJson_passes() throws Exception {
        Path resourceFile = tempDir.resolve(
                "mango-workflow-starter/src/main/resources/META-INF/mango/resources/workflow-common-menu.json");
        Files.createDirectories(resourceFile.getParent());
        Files.writeString(resourceFile, """
                {
                  "mango": {
                    "resource": {
                      "moduleCode": "workflow",
                      "declarations": {
                        "AUTH_MENU": [ {
                          "id": "2951300000000009001",
                          "bizKey": "workflow.menu.internal-admin",
                          "fields": {
                            "appCode": { "type": "STRING", "value": "internal-admin" },
                            "menus": { "type": "LIST", "value": [ { "menuCode": "workflow" } ] }
                          }
                        } ]
                      }
                    }
                  }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleMenu_withAuthMenuYaml_passes() throws Exception {
        Path resourceFile = tempDir.resolve(
                "mango-demo-starter/src/main/resources/META-INF/mango/resources/demo-common-menu.yaml");
        Files.createDirectories(resourceFile.getParent());
        Files.writeString(resourceFile, """
                mango:
                  resource:
                    module-code: demo
                    declarations:
                      AUTH_MENU:
                        - id: "2951300000000009101"
                          biz-key: demo.menu.internal-admin
                          fields:
                            appCode:
                              type: STRING
                              value: internal-admin
                            menus:
                              type: LIST
                              value:
                                - menuCode: demo
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleMenu_withProjectScope_ignoresSiblingModuleDebt() throws Exception {
        Path workflowModule = tempDir.resolve("mango-workflow-starter");
        Path resourceFile = workflowModule.resolve(
                "src/main/resources/META-INF/mango/resources/workflow-common-menu.json");
        Files.createDirectories(resourceFile.getParent());
        Files.writeString(resourceFile, """
                {
                  "mango": {
                    "resource": {
                      "moduleCode": "workflow",
                      "declarations": {
                        "AUTH_MENU": [ {
                          "id": "2951300000000009001",
                          "bizKey": "workflow.menu.internal-admin",
                          "fields": {
                            "appCode": { "type": "STRING", "value": "internal-admin" },
                            "menus": { "type": "LIST", "value": [ { "menuCode": "workflow" } ] }
                          }
                        } ]
                      }
                    }
                  }
                }
                """);
        Path siblingSql = tempDir.resolve(
                "mango-authorization-core/src/main/resources/db/migration/authorization/V2__menu.sql");
        Files.createDirectories(siblingSql.getParent());
        Files.writeString(siblingSql, "INSERT INTO authorization_menu (id, menu_code) VALUES (1, 'legacy');");

        MavenProject project = new MavenProject();
        project.setFile(workflowModule.resolve("pom.xml").toFile());

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", workflowModule.toString());
        setField(mojo, "project", project);
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleMenu_withChangedOnly_reportsSiblingDebtAsOutOfScope() throws Exception {
        Path workflowResource = tempDir.resolve(
                "mango-workflow-starter/src/main/resources/META-INF/mango/resources/workflow-common-menu.json");
        Files.createDirectories(workflowResource.getParent());
        Files.writeString(workflowResource, """
                {
                  "mango": {
                    "resource": {
                      "moduleCode": "workflow",
                      "declarations": {
                        "AUTH_MENU": [ {
                          "id": "2951300000000009001",
                          "bizKey": "workflow.menu.internal-admin",
                          "fields": {
                            "appCode": { "type": "STRING", "value": "internal-admin" },
                            "menus": { "type": "LIST", "value": [ { "menuCode": "workflow" } ] }
                          }
                        } ]
                      }
                    }
                  }
                }
                """);
        Path legacySql = tempDir.resolve(
                "mango-cms-core/src/main/resources/db/migration/mango-cms/V4__remove_cms_banner_menu.sql");
        Files.createDirectories(legacySql.getParent());
        Files.writeString(legacySql, "DELETE FROM authorization_menu WHERE menu_code = 'cms:banner';");
        Path reportFile = tempDir.resolve("target/module-menu-scope-report.json");

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "changedOnly", true);
        setField(mojo, "changedFiles",
                "mango-workflow-starter/src/main/resources/META-INF/mango/resources/workflow-common-menu.json");
        setField(mojo, "reportFile", reportFile.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
        CheckResult result = new ObjectMapper().readValue(reportFile.toFile(), CheckResult.class);
        assertEquals(0, result.issues.size());
        assertEquals(1, result.excludedIssues.size());
        assertTrue(result.excludedIssues.get(0).description.contains("out-of-scope existing issue"));
    }

    @Test
    void checkModuleMenu_withChangedOnlyFailsForCurrentScopeSql() throws Exception {
        Path sqlFile = tempDir.resolve(
                "mango-demo-core/src/main/resources/db/migration/demo/V2__demo_menu.sql");
        Files.createDirectories(sqlFile.getParent());
        Files.writeString(sqlFile, "INSERT INTO authorization_menu (id, menu_code) VALUES (1, 'demo');");

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "changedOnly", true);
        setField(mojo, "changedFiles",
                "mango-demo-core/src/main/resources/db/migration/demo/V2__demo_menu.sql");
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().contains("issues=1"));
    }

    @Test
    void checkModuleMenu_withMenuFlywaySql_fails() throws Exception {
        Path sqlFile = tempDir.resolve(
                "mango-authorization-core/src/main/resources/db/migration/authorization/V2__menu.sql");
        Files.createDirectories(sqlFile.getParent());
        Files.writeString(sqlFile, """
                INSERT INTO authorization_menu (id, menu_code) VALUES (1, 'workflow');
                UPDATE frontend_menu_runtime_config SET page_type = 'LOCAL_ROUTE' WHERE menu_id = 1;
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().contains("issues=2"));
    }

    @Test
    void checkModuleMenu_withRetireMenuFlywaySql_fails() throws Exception {
        Path sqlFile = tempDir.resolve(
                "mango-authorization-core/src/main/resources/db/migration/authorization/V38__retire_route_menu.sql");
        Files.createDirectories(sqlFile.getParent());
        Files.writeString(sqlFile, """
                DELETE FROM authorization_role_menu WHERE menu_id = 21;
                DELETE FROM authorization_menu_package_item WHERE menu_id = 21;
                DELETE role_menu FROM authorization_role_menu role_menu
                  INNER JOIN authorization_menu menu ON role_menu.menu_id = menu.id
                  WHERE menu.menu_code = 'system:route';
                DELETE FROM authorization_menu WHERE id = 21 OR menu_code = 'system:route';
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().contains("issues=4"));
    }

    @Test
    void checkModuleMenu_withLegacyResourceManifestMenus_fails() throws Exception {
        Path manifestFile = tempDir.resolve(
                "mango-job-starter/src/main/resources/META-INF/mango/resource-manifest.json");
        Files.createDirectories(manifestFile.getParent());
        Files.writeString(manifestFile, """
                {
                  "appCode": "internal-admin",
                  "menus": [ { "menuCode": "job" } ]
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-menu");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkAll_withoutMavenStaticToolchainFailsClosed() throws Exception {
        // given
        Path javaFile = tempDir.resolve("src/main/java/io/mango/demo/TestService.java");
        Files.createDirectories(javaFile.getParent());
        StringBuilder source = new StringBuilder();
        source.append("package io.mango.demo;\n");
        source.append("public class TestService {\n");
        source.append("    public void longMethod() {\n");
        for (int i = 0; i < 60; i++) {
            source.append("        System.out.println(\"line " + i + "\");\n");
        }
        source.append("    }\n");
        source.append("}\n");
        Files.writeString(javaFile, source.toString());

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "all");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void resolveStaticAnalysisProjects_withSessionReactor_keepsDependencyJarsAndExcludesGovernanceAggregators()
            throws Exception {
        // given
        Path rootPom = tempDir.resolve("pom.xml");
        Files.writeString(rootPom, """
                <project>
                    <modules>
                        <module>mango-platform</module>
                        <module>mango-infra</module>
                    </modules>
                </project>
                """);
        Path jobRoot = tempDir.resolve("mango-platform/mango-job");
        Path jobSupport = jobRoot.resolve("mango-job-support");
        Path jobCore = jobRoot.resolve("mango-job-core");
        Path architectureVerification = tempDir.resolve("architecture-verification");
        Path mangoArchitectureVerification = tempDir.resolve("mango-architecture-verification");
        Path adminStarter = tempDir.resolve("mango-admin-starter");
        Path infraKv = tempDir.resolve("mango-infra/mango-infra-kv");
        Files.createDirectories(jobSupport);
        Files.createDirectories(jobCore);
        Files.createDirectories(architectureVerification);
        Files.createDirectories(mangoArchitectureVerification);
        Files.createDirectories(adminStarter);
        Files.createDirectories(infraKv);
        Files.writeString(jobSupport.resolve("pom.xml"), "<project/>");
        Files.writeString(jobCore.resolve("pom.xml"), "<project/>");
        Files.writeString(architectureVerification.resolve("pom.xml"), "<project/>");
        Files.writeString(mangoArchitectureVerification.resolve("pom.xml"), "<project/>");
        Files.writeString(adminStarter.resolve("pom.xml"), "<project/>");
        Files.writeString(infraKv.resolve("pom.xml"), "<project/>");
        Path supportSource = jobSupport.resolve("src/main/java/io/mango/job/Support.java");
        Path coreSource = jobCore.resolve("src/main/java/io/mango/job/Core.java");
        Files.createDirectories(supportSource.getParent());
        Files.createDirectories(coreSource.getParent());
        Files.writeString(supportSource, "package io.mango.job; final class Support {}\n");
        Files.writeString(coreSource, "package io.mango.job; final class Core {}\n");

        MavenSession session = mock(MavenSession.class);
        MavenProject rootProject = new MavenProject();
        rootProject.setFile(rootPom.toFile());
        MavenProject supportProject = new MavenProject();
        supportProject.setFile(jobSupport.resolve("pom.xml").toFile());
        supportProject.addCompileSourceRoot(jobSupport.resolve("src/main/java").toString());
        MavenProject coreProject = new MavenProject();
        coreProject.setFile(jobCore.resolve("pom.xml").toFile());
        coreProject.addCompileSourceRoot(jobCore.resolve("src/main/java").toString());
        MavenProject architectureProject = new MavenProject();
        architectureProject.setFile(architectureVerification.resolve("pom.xml").toFile());
        MavenProject mangoArchitectureProject = new MavenProject();
        mangoArchitectureProject.setFile(mangoArchitectureVerification.resolve("pom.xml").toFile());
        MavenProject adminStarterProject = new MavenProject();
        adminStarterProject.setFile(adminStarter.resolve("pom.xml").toFile());
        adminStarterProject.setArtifactId("mango-admin-starter");
        adminStarterProject.setPackaging("jar");
        when(session.getProjects()).thenReturn(
                List.of(
                        rootProject,
                        supportProject,
                        coreProject,
                        architectureProject,
                        mangoArchitectureProject,
                        adminStarterProject));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", session);

        Method method = CheckMojo.class.getDeclaredMethod("resolveStaticAnalysisProjects", Path.class);
        method.setAccessible(true);

        // when
        @SuppressWarnings("unchecked")
        List<String> projects = (List<String>) method.invoke(mojo, tempDir);

        // then
        assertEquals(List.of(
                "mango-platform/mango-job/mango-job-support",
                "mango-platform/mango-job/mango-job-core",
                "mango-admin-starter"
        ), projects);
    }

    @Test
    void sessionContainsJavaCompileSource_withDependencyOnlyReactor_returnsFalse() throws Exception {
        Path adminStarter = tempDir.resolve("mango-admin-starter");
        Files.createDirectories(adminStarter);
        Files.writeString(adminStarter.resolve("pom.xml"), "<project/>");
        MavenProject adminProject = new MavenProject();
        adminProject.setFile(adminStarter.resolve("pom.xml").toFile());
        adminProject.setArtifactId("mango-admin-starter");
        adminProject.setPackaging("jar");
        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(adminProject));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "session", session);
        Method containsSource =
                CheckMojo.class.getDeclaredMethod("sessionContainsJavaCompileSource");
        containsSource.setAccessible(true);
        Method resolveProjects =
                CheckMojo.class.getDeclaredMethod("resolveStaticAnalysisProjects", Path.class);
        resolveProjects.setAccessible(true);

        assertFalse((boolean) containsSource.invoke(mojo));
        assertEquals(List.of("mango-admin-starter"), resolveProjects.invoke(mojo, tempDir));
    }

    @Test
    void stripStringLiterals_preservesLayoutAcrossEscapesTextBlocksAndNewlines()
            throws Exception {
        CheckMojo mojo = new CheckMojo();
        Method method = CheckMojo.class.getDeclaredMethod("stripStringLiterals", String.class);
        method.setAccessible(true);

        assertEquals(
                "call(" + " ".repeat(6) + ");",
                method.invoke(mojo, "call(\"a\\\"b\");"));
        assertEquals(
                "before " + " ".repeat(9) + "\n" + " ".repeat(8) + " after",
                method.invoke(mojo, "before \"\"\"secret\nvalue\"\"\" after"));
        assertEquals(
                "call(" + " ".repeat(5) + "\nnext());",
                method.invoke(mojo, "call(\"open\nnext());"));
    }

    @Test
    void invokeSingleGoal_whenDelegatedMavenCommandHangs_timesOut() throws Exception {
        // given
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>");
        Path slowMaven = tempDir.resolve("slow-mvn.sh");
        Path childPidFile = tempDir.resolve("child.pid");
        Files.writeString(slowMaven, """
                #!/bin/sh
                sleep 5 &
                child_pid=$!
                printf '%%s' "$child_pid" > "%s"
                wait "$child_pid"
                """.formatted(childPidFile));
        assertTrue(slowMaven.toFile().setExecutable(true));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "staticTimeoutSeconds", 1L);

        Method method = CheckMojo.class.getDeclaredMethod(
                "invokeSingleGoal", File.class, Path.class, String.class, List.class);
        method.setAccessible(true);

        // when
        long startedAt = System.nanoTime();
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> method.invoke(mojo, slowMaven.toFile(), tempDir, "pmd:check", List.of()));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        // then
        assertTrue(elapsedMillis < 4_000, "timeout should not wait for the full child process sleep");
        assertInstanceOf(MojoExecutionException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("timed out after 1s"));
        assertTrue(exception.getCause().getMessage().contains("pmd:check"));
        long childPid = Long.parseLong(Files.readString(childPidFile));
        assertFalse(
                ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false),
                "timeout must terminate delegated child processes");
    }

    @Test
    void invokeSingleGoal_includesCompileBeforeStaticGoal() throws Exception {
        // given
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>");
        Path fakeMaven = tempDir.resolve("fake-mvn.sh");
        Path commandFile = tempDir.resolve("command.txt");
        Files.writeString(fakeMaven, """
                #!/bin/sh
                printf '%%s\\n' "$@" > "%s"
                """.formatted(commandFile));
        assertTrue(fakeMaven.toFile().setExecutable(true));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "staticTimeoutSeconds", 5L);

        Method method = CheckMojo.class.getDeclaredMethod(
                "invokeSingleGoal", File.class, Path.class, String.class, List.class);
        method.setAccessible(true);

        // when
        method.invoke(mojo, fakeMaven.toFile(), tempDir, "pmd:check", List.of("mango-demo-core"));

        // then
        List<String> command = Files.readAllLines(commandFile);
        assertTrue(command.contains("compile"));
        assertEquals(command.indexOf("compile") + 1, command.indexOf("pmd:check"));
        assertFalse(command.contains("-am"));
        assertFalse(command.contains("-amd"));
    }

    @Test
    void invokeSingleGoal_withoutCheckstyleConfig_passesBundledMangoRules() throws Exception {
        // given
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>\n");
        Path fakeMaven = tempDir.resolve("fake-mvn.sh");
        Path commandFile = tempDir.resolve("command.txt");
        Files.writeString(fakeMaven, """
                #!/bin/sh
                printf '%%s\n' "$@" > "%s"
                """.formatted(commandFile));
        assertTrue(fakeMaven.toFile().setExecutable(true));

        CheckMojo mojo = new CheckMojo();
        CheckResult checkResult = new CheckResult();
        setField(mojo, "staticTimeoutSeconds", 5L);
        setField(mojo, "result", checkResult);

        Method method = CheckMojo.class.getDeclaredMethod(
                "invokeSingleGoal", File.class, Path.class, String.class, List.class);
        method.setAccessible(true);

        // when
        method.invoke(mojo, fakeMaven.toFile(), tempDir, "checkstyle:checkstyle", List.of());

        // then
        List<String> command = Files.readAllLines(commandFile);
        String property = command.stream()
                .filter(argument -> argument.startsWith("-Dcheckstyle.config.location="))
                .findFirst()
                .orElseThrow();
        Path configFile = Path.of(property.substring(property.indexOf('=') + 1));
        assertTrue(Files.isRegularFile(configFile));
        assertFalse(Files.readString(configFile).contains("DesignForExtension"));
        assertTrue(checkResult.gateMessages.contains("Checkstyle rules: default:mango-bundled"));
    }

    @Test
    void staticAnalysisReportGoals_deferViolationEnforcementToMangoGate() {
        // given
        CheckMojo mojo = new CheckMojo();

        // when
        List<String> goals = mojo.staticAnalysisReportGoals();

        // then
        assertEquals(List.of("pmd:pmd", "checkstyle:checkstyle", "spotbugs:spotbugs"), goals);
        assertFalse(goals.contains("pmd:check"));
        assertFalse(goals.contains("checkstyle:check"));
    }

    @Test
    void staticReportScanIncludesTargetAndMissingReportFailsClosed() throws Exception {
        Path report = tempDir.resolve("demo/target/pmd.xml");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "<pmd/>");
        CheckMojo mojo = new CheckMojo();

        Method findReports = CheckMojo.class.getDeclaredMethod(
                "findReports", Path.class, String.class);
        findReports.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Path> reports = (List<Path>) findReports.invoke(mojo, tempDir, "pmd.xml");
        assertEquals(List.of(report), reports);

        Method requireReports = CheckMojo.class.getDeclaredMethod(
                "requireStaticReports", Path.class, String.class, String.class);
        requireReports.setAccessible(true);
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> requireReports.invoke(mojo, tempDir, "spotbugsXml.xml", "spotbugs"));
        assertInstanceOf(MojoExecutionException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("produced no spotbugs report"));
    }

    @Test
    void recordStaticFailure_withReportPolicy_recordsToolFailure() throws Exception {
        // given
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "staticFailurePolicy", "report");
        CheckResult result = new CheckResult();
        result.staticFailurePolicy = "report";
        setField(mojo, "result", result);

        Method method = CheckMojo.class.getDeclaredMethod(
                "recordStaticFailure", String.class, MojoExecutionException.class);
        method.setAccessible(true);

        // when
        boolean recorded = (boolean) method.invoke(mojo, "spotbugs:spotbugs",
                new MojoExecutionException("timed out"));

        // then
        assertTrue(recorded);
        assertEquals(1, result.toolFailures.size());
        assertEquals("spotbugs:spotbugs", result.toolFailures.get(0).goal);
        assertEquals("timed out", result.toolFailures.get(0).message);
    }

    @Test
    void finalizeResult_withReportPolicyAndToolFailure_reportsInconclusive() throws Exception {
        // given
        CheckResult result = new CheckResult();
        result.addToolFailure("spotbugs:spotbugs", "timed out");
        CheckGateFinalizer finalizer = new CheckGateFinalizer(objectMapper(),
                new CheckGateOptions(tempDir, null, null, null, "all", "report"));

        // when
        assertDoesNotThrow(() -> finalizer.finalizeResult(result));

        // then
        assertTrue(result.passed);
        assertEquals("INCONCLUSIVE", result.gateStatus);
        assertEquals(1, result.toolFailureCount);
        assertTrue(result.gateMessages.contains("static analysis has reported tool failure(s)"));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    void checkDependency_withValidPom_passes() throws Exception {
        // given
        Path projectDir = tempDir.resolve("mango-user");
        Files.createDirectories(projectDir);
        Path pomFile = projectDir.resolve("pom.xml");
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-user-api</artifactId>
                    <version>1.0.0</version>
                </project>
                """;
        Files.writeString(pomFile, pomContent);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withStarterRemoteDependingOnCore_reportsIssue() throws Exception {
        // given
        Path projectDir = tempDir.resolve("mango-demo-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-core</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withStarterRemoteDependingOnSupport_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-support</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-feign-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withCoreDependingOnSupport_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-core");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-support</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withApiDependingOnSupport_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-api");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-api</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-support</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withSupportDependingOnCore_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-support");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-support</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-core</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withSupportContainingPersistenceContent_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-support");
        Path sourceDir = projectDir.resolve("src/main/java/io/mango/demo/support");
        Files.createDirectories(sourceDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-support</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-api</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);
        Files.writeString(sourceDir.resolve("DemoEntity.java"), """
                package io.mango.demo.support;

                import com.baomidou.mybatisplus.annotation.TableName;

                @TableName("demo")
                public class DemoEntity {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withSupportContainingAutoConfiguration_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-support");
        Path sourceDir = projectDir.resolve("src/main/java/io/mango/demo/support");
        Files.createDirectories(sourceDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-support</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(sourceDir.resolve("DemoAutoConfiguration.java"), """
                package io.mango.demo.support;

                import org.springframework.boot.autoconfigure.AutoConfiguration;

                @AutoConfiguration
                public class DemoAutoConfiguration {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withSupportContainingModuleProperties_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-support");
        Path resourceDir = projectDir.resolve("src/main/resources/META-INF/mango");
        Files.createDirectories(resourceDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-support</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(resourceDir.resolve("module.properties"), """
                module-name=mango-demo
                module-path=/demo
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withSupportContainingController_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-support");
        Path sourceDir = projectDir.resolve("src/main/java/io/mango/demo/support");
        Files.createDirectories(sourceDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-support</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.support;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DemoController {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withSupportContainingFeignClient_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-support");
        Path sourceDir = projectDir.resolve("src/main/java/io/mango/demo/support");
        Files.createDirectories(sourceDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-support</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(sourceDir.resolve("DemoFeignClient.java"), """
                package io.mango.demo.support;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-demo")
                public interface DemoFeignClient {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withStarterRemoteDependingOnSpringCloudOpenFeign_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-demo-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-starter-openfeign</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withAuthorizationRemoteSupport_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-authorization-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-authorization-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-authorization-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-authorization-support</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withSessionReactor_scansOnlySessionProjects() throws Exception {
        Path sessionDir = tempDir.resolve("mango-platform/mango-job/mango-job-core");
        Path historicalDir = tempDir.resolve("mango-platform/mango-file/mango-file-core");
        Files.createDirectories(sessionDir);
        Files.createDirectories(historicalDir);
        Files.writeString(sessionDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.job</groupId>
                    <artifactId>mango-job-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.infra.persistence</groupId>
                            <artifactId>mango-infra-persistence-api</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);
        Files.writeString(historicalDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.file</groupId>
                    <artifactId>mango-file-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.infra.persistence</groupId>
                            <artifactId>mango-infra-persistence-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        MavenSession session = mock(MavenSession.class);
        MavenProject sessionProject = new MavenProject();
        sessionProject.setFile(sessionDir.resolve("pom.xml").toFile());
        when(session.getProjects()).thenReturn(List.of(sessionProject));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", session);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withSessionReactor_reportsViolationInsideSession() throws Exception {
        Path sessionDir = tempDir.resolve("mango-platform/mango-job/mango-job-core");
        Files.createDirectories(sessionDir);
        Files.writeString(sessionDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.job</groupId>
                    <artifactId>mango-job-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.infra.persistence</groupId>
                            <artifactId>mango-infra-persistence-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        MavenSession session = mock(MavenSession.class);
        MavenProject sessionProject = new MavenProject();
        sessionProject.setFile(sessionDir.resolve("pom.xml").toFile());
        when(session.getProjects()).thenReturn(List.of(sessionProject));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", session);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkDependency_withNonResourceModuleDependingOnResourceStarter_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-platform/mango-domain/mango-domain-core");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.domain</groupId>
                    <artifactId>mango-domain-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.platform.resource</groupId>
                            <artifactId>mango-resource-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().contains("issues=1"));
    }

    @Test
    void checkDependency_withAppDependingOnResourceStarter_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-app/platform-capability/mango-resource-capability-app");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.app</groupId>
                    <artifactId>mango-resource-capability-app</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.platform.resource</groupId>
                            <artifactId>mango-resource-starter</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.mango.platform.resource</groupId>
                            <artifactId>mango-resource-sync-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withNonResourceModuleDependingOnResourceSupport_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-platform/mango-domain/mango-domain-core");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.domain</groupId>
                    <artifactId>mango-domain-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.platform.resource</groupId>
                            <artifactId>mango-resource-support</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withResourceStarterExceptionAndReason_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-platform/mango-domain/mango-domain-core");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.domain</groupId>
                    <artifactId>mango-domain-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.platform.resource</groupId>
                            <artifactId>mango-resource-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);
        setField(mojo, "resourceStarterDependencyExceptions", "mango-domain-core=confirmed deployment adapter");

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withResourceStarterExceptionWithoutReason_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-platform/mango-domain/mango-domain-core");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.domain</groupId>
                    <artifactId>mango-domain-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.platform.resource</groupId>
                            <artifactId>mango-resource-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);
        setField(mojo, "resourceStarterDependencyExceptions", "mango-domain-core=");

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().contains("issues=1"));
    }

    @Test
    void checkDependency_withResourceStarterInDependencyManagement_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-platform/mango-domain/mango-domain-parent");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.domain</groupId>
                    <artifactId>mango-domain-parent</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.mango.platform.resource</groupId>
                                <artifactId>mango-resource-starter</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withCoreDependingOnStarterInTestScope_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-platform/mango-domain/mango-domain-core");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.domain</groupId>
                    <artifactId>mango-domain-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.infra.persistence</groupId>
                            <artifactId>mango-infra-persistence-starter</artifactId>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_withResourceRemoteDependingOnCore_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-platform/mango-resource/mango-resource-starter-remote");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.resource</groupId>
                    <artifactId>mango-resource-starter-remote</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango.platform.resource</groupId>
                            <artifactId>mango-resource-core</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().contains("issues=1"));
    }

    @Test
    void checkWebBoundary_withApiDependingOnWebApi_passes() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-api");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-api</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-web-api</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "web-boundary");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkWebBoundary_withApiDependingOnWebStarter_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-api");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-api</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-web-starter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "web-boundary");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkWebBoundary_withDuplicateSpringWebStarter_reportsIssue() throws Exception {
        Path projectDir = tempDir.resolve("mango-demo-starter");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-demo-starter</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.mango</groupId>
                            <artifactId>mango-infra-web-starter</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "web-boundary");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withStarterModuleProperties_passes() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.createDirectories(starterDir.resolve("src/main/java/io/mango/rbac/starter"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>io.mango</groupId>
                        <artifactId>mango-parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/java/io/mango/rbac/starter/RbacController.java"), """
                package io.mango.rbac.starter;

                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/rbac/user")
                public class RbacController {
                }
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"),
                """
                module-name=mango-rbac
                module-path=/rbac
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withMultipleModulePaths_passes() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-payment-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.createDirectories(starterDir.resolve("src/main/java/io/mango/payment/starter"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-payment-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/java/io/mango/payment/starter/PaymentController.java"), """
                package io.mango.payment.starter;

                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/payment/orders")
                public class PaymentController {
                }
                """);
        Files.writeString(starterDir.resolve("src/main/java/io/mango/payment/starter/PaymentOpenApiController.java"), """
                package io.mango.payment.starter;

                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/openapi/pay")
                public class PaymentOpenApiController {
                }
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-payment
                module-path=/payment,/openapi/pay
                """);

        Path remoteDir = tempDir.resolve("mango-payment-starter-remote/src/main/java/io/mango/payment/starter/remote");
        Files.createDirectories(remoteDir);
        Files.writeString(remoteDir.resolve("PaymentInboundController.java"), """
                package io.mango.payment.starter.remote;

                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/_payment/callbacks")
                public class PaymentInboundController {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withMissingModuleProperties_reportsIssue() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir);
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withModuleName_passes() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-rbac
                module-path=/rbac
                """);
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                import io.mango.rbac.api.SysUserApi;

                @FeignClient(name = "mango-rbac", contextId = "sysUserFeignClient", path = "/rbac/user")
                public interface SysUserFeignClient extends SysUserApi {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withServiceName_reportsIssue() throws Exception {
        // given
        Path starterDir = tempDir.resolve("mango-rbac-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-rbac
                module-path=/rbac
                """);
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                import io.mango.rbac.api.SysUserApi;

                @FeignClient(name = "permission-service", contextId = "sysUserFeignClient", path = "/rbac/user")
                public interface SysUserFeignClient extends SysUserApi {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withoutContextId_reportsIssue() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/rbac", "/rbac/user");
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import io.mango.rbac.api.SysUserApi;
                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", path = "/rbac/user")
                public interface SysUserFeignClient extends SysUserApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withMultipleApiContracts_reportsIssue() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/rbac", "/rbac/user");
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import io.mango.rbac.api.SysRoleApi;
                import io.mango.rbac.api.SysUserApi;
                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", contextId = "sysUserFeignClient", path = "/rbac/user")
                public interface SysUserFeignClient extends SysUserApi, SysRoleApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withDuplicateContextIdForSameName_reportsIssue() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/rbac", "/rbac/user");
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import io.mango.rbac.api.SysUserApi;
                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", contextId = "sysUserFeignClient", path = "/rbac/user")
                public interface SysUserFeignClient extends SysUserApi {
                }
                """);
        Files.writeString(sourceDir.resolve("SysRoleFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import io.mango.rbac.api.SysRoleApi;
                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", contextId = "sysUserFeignClient", path = "/rbac/role")
                public interface SysRoleFeignClient extends SysRoleApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withMultipleModulePaths_passes() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/rbac,/post", "/rbac/user");
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("PostFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import io.mango.rbac.api.PostApi;
                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", contextId = "postFeignClient", path = "/post")
                public interface PostFeignClient extends PostApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withReverseModulePath_passes() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/rbac", "/rbac/user");
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import io.mango.rbac.api.SysUserApi;
                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac", contextId = "sysUserFeignClient", path = "/_rbac/targets")
                public interface SysUserFeignClient extends SysUserApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withDuplicateModulePath_reportsIssue() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/shared", "/shared/user");
        createStarterModule("mango-auth-starter", "mango-auth", "/shared", "/shared/login");

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withNonStarterModuleProperties_reportsIssue() throws Exception {
        Path coreDir = tempDir.resolve("mango-rbac-core");
        Files.createDirectories(coreDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(coreDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango</groupId>
                    <artifactId>mango-rbac-core</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(coreDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-rbac
                module-path=/rbac
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withSyncStarterWithoutModuleProperties_passes() throws Exception {
        Path starterDir = tempDir.resolve("mango-resource-starter");
        Files.createDirectories(starterDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(starterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.resource</groupId>
                    <artifactId>mango-resource-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(starterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-resource
                module-path=/resource
                """);
        Path syncStarterDir = tempDir.resolve("mango-resource-sync-starter");
        Files.createDirectories(syncStarterDir);
        Files.writeString(syncStarterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.resource</groupId>
                    <artifactId>mango-resource-sync-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkModuleInfo_withSyncStarterModuleProperties_reportsIssue() throws Exception {
        Path syncStarterDir = tempDir.resolve("mango-resource-sync-starter");
        Files.createDirectories(syncStarterDir.resolve("src/main/resources/META-INF/mango"));
        Files.writeString(syncStarterDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>io.mango.platform.resource</groupId>
                    <artifactId>mango-resource-sync-starter</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(syncStarterDir.resolve("src/main/resources/META-INF/mango/module.properties"), """
                module-name=mango-resource-sync
                module-path=/resource-sync
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "module-info");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkRemoteAdapter_withWrongPath_reportsIssue() throws Exception {
        createStarterModule("mango-rbac-starter", "mango-rbac", "/rbac", "/rbac/user");
        Path sourceDir = tempDir.resolve("mango-rbac-starter-remote/src/main/java/io/mango/rbac/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserFeignClient.java"), """
                package io.mango.rbac.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                import io.mango.rbac.api.SysUserApi;

                @FeignClient(name = "mango-rbac", contextId = "sysUserFeignClient", path = "/internal/rbac/user")
                public interface SysUserFeignClient extends SysUserApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "remote-adapter");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withoutFeignClient_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-rbac-api/src/main/java/io/mango/rbac/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserApi.java"), """
                package io.mango.rbac.api;

                public interface SysUserApi {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withFeignClient_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-rbac-api/src/main/java/io/mango/rbac/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SysUserApi.java"), """
                package io.mango.rbac.api;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-rbac")
                public interface SysUserApi {
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withLocalCollaborationType_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-realtime-api/src/main/java/io/mango/realtime/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RealtimePollingService.java"), """
                package io.mango.realtime.api;

                public interface RealtimePollingService {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withSessionType_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-realtime-api/src/main/java/io/mango/realtime/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RealtimeSession.java"), """
                package io.mango.realtime.api;

                public interface RealtimeSession {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withHttpContractNotEndingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("AuthContract.java"), """
                package io.mango.demo.api;

                import org.springframework.web.bind.annotation.PostMapping;

                public interface AuthContract {
                    @PostMapping("/login")
                    void login();
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withRegistryApiName_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoRegistryApi.java"), """
                package io.mango.demo.api;

                public interface DemoRegistryApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withControllerHoldingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import io.mango.demo.api.DemoApi;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DemoController {
                    private final DemoApi demoApi;
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withServiceImplementingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.demo.api.DemoApi;

                public class DemoService implements DemoApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withFeignClientNotExtendingApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter-remote/src/main/java/io/mango/demo/starter/remote");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoFeignClient.java"), """
                package io.mango.demo.starter.remote;

                import org.springframework.cloud.openfeign.FeignClient;

                @FeignClient(name = "mango-demo", path = "/demo")
                public interface DemoFeignClient {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withMoreThanTwoMethodParameters_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoApi.java"), """
                package io.mango.demo.api;

                import io.mango.common.result.R;

                public interface DemoApi {
                    R<Boolean> updateValue(Long id, String value, Boolean enabled);
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withTwoSimpleMethodParameters_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoApi.java"), """
                package io.mango.demo.api;

                import io.mango.common.result.R;

                public interface DemoApi {
                    R<Boolean> updateValue(Long id, String value);
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withDefaultMethodDoesNotTreatReturnAsDeclaration() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoApi.java"), """
                package io.mango.demo.api;

                import io.mango.common.result.R;

                public interface DemoApi {
                    R<Boolean> updateValue(Long id, String value);
                    default R<Boolean> updateDefault(Long id, String value) {
                        return updateValue(id, value);
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withResponseEntityDoesNotTreatFrameworkWrapperAsPersistenceModel() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import org.springframework.http.ResponseEntity;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @Validated
                @RestController
                public class DemoController implements DemoApi {
                    @GetMapping("/download")
                    public ResponseEntity<byte[]> download() {
                        return ResponseEntity.ok(new byte[0]);
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute(),
                "ResponseEntity is not a persistence model, but non-R HTTP return remains a contract violation");
        Field resultField = CheckMojo.class.getDeclaredField("result");
        resultField.setAccessible(true);
        CheckResult result = (CheckResult) resultField.get(mojo);
        assertTrue(result.issues.stream().noneMatch(issue -> issue.description.contains("持久化模型")));
    }

    @Test
    void checkUnknownRuleFailsClosed() throws Exception {
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "mapper");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withEntityInApi_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api/entity");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoEntity.java"), """
                package io.mango.demo.api.entity;
                public class DemoEntity {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withFeignOutsideStarterRemote_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoFeignClient.java"), """
                package io.mango.demo.core;
                import org.springframework.cloud.openfeign.FeignClient;
                @FeignClient(name = "mango-demo")
                public interface DemoFeignClient extends DemoApi {
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withCompliantController_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;
                import io.mango.common.result.R;
                import jakarta.validation.Valid;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                @Validated
                public class DemoController implements DemoApi {
                    private final IDemoService demoService;
                    public DemoController(IDemoService demoService) { this.demoService = demoService; }
                    @PostMapping("/demo")
                    public R<DemoVO> create(@RequestBody @Valid CreateDemoCommand command) {
                        return R.ok(demoService.create(command));
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withApiOwnedValidation_doesNotRequireControllerToRepeatValid() throws Exception {
        Path apiDir = tempDir.resolve("mango-demo-api/src/main/java/io/mango/demo/api");
        Path starterDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(apiDir);
        Files.createDirectories(starterDir);
        Files.writeString(apiDir.resolve("DemoApi.java"), """
                package io.mango.demo.api;
                import io.mango.common.result.R;
                import jakarta.validation.Valid;
                public interface DemoApi {
                    R<DemoVO> create(@Valid CreateDemoCommand command);
                }
                """);
        Files.writeString(starterDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;
                import io.mango.common.result.R;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                @Validated
                public class DemoController implements DemoApi {
                    private final IDemoService demoService;
                    public DemoController(IDemoService demoService) { this.demoService = demoService; }
                    @PostMapping("/demo")
                    public R<DemoVO> create(@RequestBody CreateDemoCommand command) {
                        return R.ok(demoService.create(command));
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withControllerMapperAndMissingValidation_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                public class DemoController {
                    private final DemoMapper demoMapper;
                    public DemoController(DemoMapper demoMapper) { this.demoMapper = demoMapper; }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withServiceActionWithoutRequire_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core/service/impl");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service.impl;
                import org.springframework.stereotype.Service;
                @Service
                public class DemoServiceImpl {
                    public Long create(CreateDemoCommand command) { return 1L; }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withServiceStringErrorInsteadOfBizCode_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core/service/impl");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service.impl;
                import io.mango.common.result.Require;
                import org.springframework.stereotype.Service;
                @Service
                public class DemoServiceImpl {
                    public Long create(CreateDemoCommand command) {
                        Require.notNull(command, "command required");
                        return 1L;
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_withServiceRequireBizCode_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core/service/impl");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service.impl;
                import io.mango.common.result.Require;
                import org.springframework.stereotype.Service;
                @Service
                public class DemoServiceImpl {
                    public Long create(CreateDemoCommand command) {
                        Require.notNull(command, DemoCode.COMMAND_REQUIRED);
                        return 1L;
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withServiceConsumingRemoteR_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core/service/impl");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service.impl;
                import io.mango.common.result.R;
                import org.springframework.stereotype.Service;
                @Service
                public class DemoServiceImpl {
                    private final UserApi userApi;
                    public DemoServiceImpl(UserApi userApi) { this.userApi = userApi; }
                    public UserVO findUser(Long id) {
                        R<UserVO> response = userApi.findUser(id);
                        return response.getData();
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withTechnicalExceptionOutsideBusinessAction_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core/service/impl");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service.impl;
                import org.springframework.stereotype.Service;
                @Service
                public class DemoServiceImpl {
                    public String serialize(Object value) {
                        try {
                            return value.toString();
                        } catch (RuntimeException exception) {
                            throw new IllegalStateException("serialization failed", exception);
                        }
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkApiContract_withServiceReturningR_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core/service/impl");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service.impl;
                import io.mango.common.result.R;
                import org.springframework.stereotype.Service;
                @Service
                public class DemoServiceImpl {
                    public R<UserVO> findUser(Long id) { return R.ok(new UserVO()); }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkApiContract_ignoresRuntimeAndNodeModulesSources() throws Exception {
        for (String ignoredRoot : List.of(".runtime/generated", "node_modules/generated", "target/generated")) {
            Path sourceDir = tempDir.resolve(ignoredRoot)
                    .resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
            Files.createDirectories(sourceDir);
            Files.writeString(sourceDir.resolve("InvalidController.java"), """
                    package io.mango.demo.starter;
                    import org.springframework.web.bind.annotation.RestController;
                    @RestController
                    public class InvalidController {
                        private final InvalidMapper invalidMapper;
                    }
                    """);
        }

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "api-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkDependency_ignoresRuntimePomFiles() throws Exception {
        Path pomFile = tempDir.resolve(".runtime/generated/mango-demo-core/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
                <project>
                  <groupId>io.mango.demo</groupId>
                  <artifactId>mango-demo-core</artifactId>
                  <dependencies>
                    <dependency>
                      <groupId>io.mango.demo</groupId>
                      <artifactId>mango-other-starter</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "dependency");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPathParam_withPathVariable_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DemoController {
                    @GetMapping("/demo/{id}")
                    String detail(@PathVariable Long id) {
                        return "ok";
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "path-param");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPathParam_withRequestParam_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DemoController {
                    @GetMapping("/demo/detail")
                    String detail(@RequestParam Long id) {
                        return "ok";
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "path-param");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPermissionParam_withPermissionAccessValue_passes() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import io.mango.authorization.api.annotation.PermissionAccess;

                public class DemoController {
                    @PermissionAccess("demo:view")
                    String detail() {
                        return "ok";
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "permission-param");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPermissionParam_withPermissionModeMissingPermission_reportsIssue() throws Exception {
        Path sourceDir = tempDir.resolve("mango-demo-starter/src/main/java/io/mango/demo/starter");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoController.java"), """
                package io.mango.demo.starter;

                import io.mango.authorization.api.annotation.ApiAccess;
                import io.mango.authorization.api.enums.ApiResourceAccessMode;

                public class DemoController {
                    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION)
                    String detail() {
                        return "ok";
                    }
                }
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "permission-param");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkKvKey_withSpelTemplate_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Cacheable;

                public class DemoService {
                    @Cacheable(key = "user:#{#userId}")
                    public String find(String userId) {
                        return userId;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkKvKey_withInlinePlaceholder_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Cacheable;

                public class DemoService {
                    @Cacheable(key = "user:#userId")
                    public String find(String userId) {
                        return userId;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkKvKey_withMultilineInlinePlaceholder_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Cacheable;

                public class DemoService {
                    @Cacheable(
                            key = "user:#userId",
                            ttl = 60
                    )
                    public String find(String userId) {
                        return userId;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkKvKey_withKvPrefix_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-demo-core/src/main/java/io/mango/demo/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core;

                import io.mango.infra.kv.api.annotation.Locker;

                public class DemoService {
                    @Locker(key = "mango:kv:prod:lock:order:1")
                    public void lock() {
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "kv-key");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withRequiredColumns_passes() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_entityTableMustExistInMigration() throws Exception {
        Path migrationFile = tempDir.resolve(
                "mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Path entityFile = tempDir.resolve(
                "mango-demo-core/src/main/java/com/example/demo/core/entity/DemoUserEntity.java");
        Files.createDirectories(entityFile.getParent());
        Files.writeString(entityFile, """
                package com.example.demo.core.entity;
                import com.baomidou.mybatisplus.annotation.TableName;
                @TableName("demo_user_typo")
                public class DemoUserEntity {}
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(exception.getMessage().contains("issues=1"));
    }

    @Test
    void checkPersistenceSchema_entityCannotBorrowSiblingModuleTable() throws Exception {
        Path migrationFile = tempDir.resolve(
                "modules/user/user-core/src/main/resources/db/migration/user/V1__init_user.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE user_account (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Path entityFile = tempDir.resolve(
                "modules/order/order-core/src/main/java/com/example/order/core/entity/OrderEntity.java");
        Files.createDirectories(entityFile.getParent());
        Files.writeString(entityFile, """
                package com.example.order.core.entity;
                import com.baomidou.mybatisplus.annotation.TableName;
                @TableName("user_account")
                public class OrderEntity {}
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void checkPersistenceSchema_mapperXmlCannotJoinSiblingModuleTable() throws Exception {
        Path migrationFile = tempDir.resolve(
                "modules/user/user-core/src/main/resources/db/migration/user/V1__init_user.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE user_account (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Path mapperXml = tempDir.resolve(
                "modules/order/order-core/src/main/resources/mapper/OrderMapper.xml");
        Files.createDirectories(mapperXml.getParent());
        Files.writeString(mapperXml, """
                <mapper namespace="com.example.order.core.mapper.OrderMapper">
                  <select id="find">SELECT id FROM user_account</select>
                </mapper>
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void checkPersistenceSchema_tableNameMustBeLiteral() throws Exception {
        Path entityFile = tempDir.resolve(
                "mango-demo-core/src/main/java/com/example/demo/core/entity/DemoUserEntity.java");
        Files.createDirectories(entityFile.getParent());
        Files.writeString(entityFile, """
                package com.example.demo.core.entity;
                import com.baomidou.mybatisplus.annotation.TableName;
                public class DemoUserEntity {
                    private static final String TABLE = "demo_user";
                }
                @TableName(DemoUserEntity.TABLE)
                class OtherEntity {}
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(exception.getMessage().contains("issues=1"));
    }

    @Test
    void checkPersistenceSchema_businessTableCannotReuseFrameworkExclusionName() throws Exception {
        Files.createDirectories(tempDir.resolve("business-pmo"));
        Path migrationFile = tempDir.resolve(
                "backend/modules/demo/demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE infra_kv_entry (
                    `id` bigint NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void checkPersistenceSchema_frameworkExclusionIsLimitedToOwnedModule() throws Exception {
        Path migrationFile = tempDir.resolve(
                "mango-infra/mango-infra-kv/mango-infra-kv-core/src/main/resources/db/migration/kv/V1__init_kv.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE infra_kv_entry (
                    `id` bigint NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void checkPersistenceSchema_withMissingRequiredColumns_reportsIssue() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `name` varchar(64) NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withLaterAlterTableAddColumn_passes() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `name` varchar(64) NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_audit_fields.sql"), """
                ALTER TABLE `demo_user` ADD COLUMN `created_by` bigint DEFAULT NULL;
                ALTER TABLE `demo_user` ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE `demo_user` ADD COLUMN `updated_by` bigint DEFAULT NULL;
                ALTER TABLE `demo_user` ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE `demo_user` ADD COLUMN `tenant_id` varchar(64) NOT NULL DEFAULT 'default';
                ALTER TABLE `demo_user` ADD COLUMN `org_id` bigint DEFAULT NULL;
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withLaterMultiAddColumnAlterTable_passes() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `name` varchar(64) NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_audit_fields.sql"), """
                ALTER TABLE `demo_user`
                  ADD COLUMN `created_by` bigint DEFAULT NULL,
                  ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  ADD COLUMN `updated_by` bigint DEFAULT NULL,
                  ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  ADD COLUMN `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                  ADD COLUMN `org_id` bigint DEFAULT NULL;
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_ignoresLaterAddKeyAlterClause() throws Exception {
        // given
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "`id` bigint NOT NULL");
        columns.put("created_by", "`created_by` bigint DEFAULT NULL");
        columns.put("created_at", "`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP");
        columns.put("updated_by", "`updated_by` bigint DEFAULT NULL");
        columns.put("updated_at", "`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP");
        columns.put("tenant_id", "`tenant_id` varchar(64) NOT NULL DEFAULT 'default'");
        columns.put("org_id", "`org_id` bigint DEFAULT NULL");

        Map<String, String> result = applyAlterTableStatementToColumns("""
                ALTER TABLE `demo_user`
                  ADD KEY `idx_demo_user_tenant_id` (`tenant_id`);
                """, columns);

        assertFalse(result.containsKey("key"));
        assertEquals(7, result.size());
    }

    @Test
    void checkPersistenceSchema_withLaterDropRequiredColumn_reportsIssue() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_drop_audit_column.sql"), """
                ALTER TABLE `demo_user`
                  DROP COLUMN `created_by`;
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withInlineUppercasePrimaryKey_passes() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` BIGINT NOT NULL PRIMARY KEY,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_ignoresAlterTableInsideSqlComments() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                -- ALTER TABLE `demo_user` DROP COLUMN `created_by`;
                /*
                ALTER TABLE `demo_user` DROP COLUMN `created_at`;
                */
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_ignoresAlterKeywordsInsideColumnComments() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_remark.sql"), """
                ALTER TABLE `demo_user`
                  ADD COLUMN `remark` varchar(64) DEFAULT NULL COMMENT 'drop column created_by';
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_appliesLaterAlterClausesInOrder() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `create_by` bigint DEFAULT NULL,
                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_rename_audit_fields.sql"), """
                ALTER TABLE `demo_user`
                  RENAME COLUMN `create_by` TO `created_by`,
                  MODIFY COLUMN `created_by` bigint DEFAULT NULL,
                  RENAME COLUMN `create_time` TO `created_at`,
                  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  ADD KEY `idx_demo_user_tenant_id` (`tenant_id`);
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withLaterPrimaryKeyFix_passes() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_primary_key.sql"), """
                ALTER TABLE `demo_user`
                  ADD PRIMARY KEY (`id`);
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withLaterInlinePrimaryKeyIdAddColumn_passes() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_add_id.sql"), """
                ALTER TABLE `demo_user`
                  ADD COLUMN `id` bigint NOT NULL PRIMARY KEY;
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withLaterInlinePrimaryKeyIdModifyColumn_passes() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_modify_id.sql"), """
                ALTER TABLE `demo_user`
                  MODIFY COLUMN `id` bigint NOT NULL PRIMARY KEY;
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withLaterIdModifyWithoutPrimaryKey_keepsExistingPrimaryKey() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_modify_id.sql"), """
                ALTER TABLE `demo_user`
                  MODIFY COLUMN `id` bigint NOT NULL COMMENT '雪花主键';
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withDuplicateIdAddColumnDoesNotFixMissingPrimaryKey() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_duplicate_add_id.sql"), """
                ALTER TABLE `demo_user`
                  ADD COLUMN `id` bigint NOT NULL PRIMARY KEY;
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withLaterDynamicSqlAlterTableAddColumn_passes() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V1__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `name` varchar(64) NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);
        Files.writeString(migrationDir.resolve("V2__demo_user_audit_fields.sql"), """
                SET @add_created_by = (
                  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `demo_user` ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT ''创建人 ID'' AFTER `id`', 'SELECT 1')
                );
                SET @add_created_at = (
                  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `demo_user` ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `created_by`', 'SELECT 1')
                );
                SET @add_updated_by = (
                  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `demo_user` ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT ''更新人 ID'' AFTER `created_at`', 'SELECT 1')
                );
                SET @add_updated_at = (
                  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `demo_user` ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `updated_by`', 'SELECT 1')
                );
                SET @add_tenant_id = (
                  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `demo_user` ADD COLUMN `tenant_id` varchar(64) NOT NULL DEFAULT ''default'' COMMENT ''租户 ID'' AFTER `updated_at`', 'SELECT 1')
                );
                SET @add_org_id = (
                  SELECT IF(COUNT(*) = 0, 'ALTER TABLE `demo_user` ADD COLUMN `org_id` bigint DEFAULT NULL COMMENT ''组织 ID'' AFTER `tenant_id`', 'SELECT 1')
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_businessUnusedDynamicAlterStringsCannotFakeSchema() throws Exception {
        Files.createDirectories(tempDir.resolve("business-pmo"));
        Path migrationFile = tempDir.resolve(
                "backend/modules/demo/demo-core/src/main/resources/db/migration/demo/V1__init.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    PRIMARY KEY (`id`)
                );
                SET @unused1 = 'ALTER TABLE demo_user ADD COLUMN created_by bigint';
                SET @unused2 = 'ALTER TABLE demo_user ADD COLUMN created_at datetime';
                SET @unused3 = 'ALTER TABLE demo_user ADD COLUMN updated_by bigint';
                SET @unused4 = 'ALTER TABLE demo_user ADD COLUMN updated_at datetime';
                SET @unused5 = 'ALTER TABLE demo_user ADD COLUMN tenant_id varchar(64)';
                SET @unused6 = 'ALTER TABLE demo_user ADD COLUMN org_id bigint';
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void checkPersistenceSchema_ordersMigrationVersionsNumerically() throws Exception {
        // given
        Path migrationDir = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo");
        Files.createDirectories(migrationDir);
        Files.writeString(migrationDir.resolve("V10__demo_user_audit_fields.sql"), """
                ALTER TABLE `demo_user` ADD COLUMN `created_by` bigint DEFAULT NULL;
                ALTER TABLE `demo_user` ADD COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE `demo_user` ADD COLUMN `updated_by` bigint DEFAULT NULL;
                ALTER TABLE `demo_user` ADD COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
                ALTER TABLE `demo_user` ADD COLUMN `tenant_id` varchar(64) NOT NULL DEFAULT 'default';
                ALTER TABLE `demo_user` ADD COLUMN `org_id` bigint DEFAULT NULL;
                """);
        Files.writeString(migrationDir.resolve("V2__init_demo.sql"), """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL,
                    `name` varchar(64) NOT NULL,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withoutStandardId_reportsIssue() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `user_id` bigint NOT NULL,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`user_id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_withAutoIncrementId_reportsIssue() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                CREATE TABLE demo_user (
                    `id` bigint NOT NULL AUTO_INCREMENT,
                    `created_by` bigint DEFAULT NULL,
                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_by` bigint DEFAULT NULL,
                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `tenant_id` varchar(64) NOT NULL DEFAULT 'default',
                    `org_id` bigint DEFAULT NULL,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_commentCannotBypassGovernedManifest() throws Exception {
        // given
        Path migrationFile = tempDir.resolve("mango-demo-core/src/main/resources/db/migration/demo/V1__init_external.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                -- mango-check: disable persistence-audit-fields reason=外部系统同步表
                CREATE TABLE external_event (
                    `id` bigint NOT NULL,
                    `payload` text,
                    PRIMARY KEY (`id`)
                );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceSchema_commentedCreateCannotSatisfyGlobalManifest() throws Exception {
        Path manifest = tempDir.resolve("business-pmo/global-entity-exceptions.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, """
                {
                  "contractId": "global-entity-exceptions",
                  "schemaRevision": 1,
                  "version": 1,
                  "exceptions": [{
                    "entity": "com.example.demo.core.entity.PlatformSettingEntity",
                    "table": "platform_setting",
                    "owner": "platform-team",
                    "reason": "平台级配置经过架构委员会审批",
                    "approvalRef": "ADR-42",
                    "approvedBy": "chief-architect",
                    "expiresOn": "2099-12-31"
                  }]
                }
                """);
        Path migrationFile = tempDir.resolve(
                "backend/modules/demo/demo-core/src/main/resources/db/migration/demo/V1__fake.sql");
        Files.createDirectories(migrationFile.getParent());
        Files.writeString(migrationFile, """
                -- CREATE TABLE platform_setting (
                --   id bigint NOT NULL PRIMARY KEY,
                --   created_by bigint,
                --   created_at datetime,
                --   updated_by bigint,
                --   updated_at datetime
                -- );
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-schema");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "globalEntityManifest", manifest);
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void checkTestFixture_withMatchingRedisFixture_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-infra-test/src/test/java/io/mango/infra/kv/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RedisKvStoreTest.java"), """
                package io.mango.infra.kv.core;

                import io.mango.infra.kv.core.redis.RedisKvStore;

                class RedisKvStoreTest {
                    private RedisKvStore kvStore;
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkTestFixture_withRedisTestUsingMemoryFixture_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-infra-test/src/test/java/io/mango/infra/kv/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RedisCacheTest.java"), """
                package io.mango.infra.kv.core;

                import io.mango.infra.kv.core.memory.MemoryKvStore;

                class RedisCacheTest {
                    private MemoryKvStore kvStore;
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkTestFixture_withCapabilityParameterizedFixture_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-infra-test/src/test/java/io/mango/infra/kv/core/capability");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("CacheTest.java"), """
                package io.mango.infra.kv.core.capability;

                import io.mango.infra.kv.core.jdbc.JdbcKvStore;
                import io.mango.infra.kv.core.memory.MemoryKvStore;
                import io.mango.infra.kv.core.redis.RedisKvStore;

                class CacheTest {
                    private MemoryKvStore memory;
                    private JdbcKvStore jdbc;
                    private RedisKvStore redis;
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkTestFixture_withReportFile_writesJsonReport() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("mango-infra-test/src/test/java/io/mango/infra/kv/core");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("RedisKvStoreTest.java"), """
                package io.mango.infra.kv.core;

                import io.mango.infra.kv.core.redis.RedisKvStore;

                class RedisKvStoreTest {
                    private RedisKvStore kvStore;
                }
                """);
        Path reportFile = tempDir.resolve("target/check-report.json");

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "test-fixture");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "reportFile", reportFile.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
        String report = Files.readString(reportFile);
        assertTrue(report.contains("\"passed\" : true"));
        assertTrue(report.contains("\"issues\" : [ ]"));
    }

    @Test
    void checkResourceRegistry_withDuplicateResourceId_reportsIssue() throws Exception {
        Path first = tempDir.resolve("mango-demo-a/src/main/resources/META-INF/mango/resources/demo-a.yml");
        Path second = tempDir.resolve("mango-demo-b/src/main/resources/META-INF/mango/resources/demo-b.yml");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, resourceDeclarationYaml("SYSTEM_DICT", "1900000000000000001", "demo.dict.first"));
        Files.writeString(second, resourceDeclarationYaml("SEQUENCE_RULE", "1900000000000000001", "demo.numgen.second"));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "resource-registry");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void checkResourceRegistry_withDuplicateTypeAndBizKey_reportsIssue() throws Exception {
        Path first = tempDir.resolve("mango-demo-a/src/main/resources/META-INF/mango/resources/demo-a.yml");
        Path second = tempDir.resolve("mango-demo-b/src/main/resources/META-INF/mango/resources/demo-b.yml");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, resourceDeclarationYaml("SYSTEM_DICT", "1900000000000000001", "demo.dict.same"));
        Files.writeString(second, resourceDeclarationYaml("SYSTEM_DICT", "1900000000000000002", "demo.dict.same"));

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "resource-registry");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void checkResourceRegistry_withNestedFieldIds_passes() throws Exception {
        Path file = tempDir.resolve("mango-demo/src/main/resources/META-INF/mango/resources/demo.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                mango:
                  resource:
                    schema-version: 1
                    module-code: demo
                    module-name: 演示
                    declarations:
                      SEQUENCE_RULE:
                        - id: "1900000000000000001"
                          version: 1
                          biz-key: demo.numgen.order-no
                          name: 演示订单号
                          target-module: numgen
                          fields:
                            segments:
                              type: LIST
                              value:
                                - id: 900000020011
                                  segmentType: TEXT
                                - id: 900000020012
                                  segmentType: SEQ
                """);

        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "resource-registry");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        assertDoesNotThrow(mojo::execute);
    }

    private String resourceDeclarationYaml(String resourceType, String id, String bizKey) {
        return """
                mango:
                  resource:
                    schema-version: 1
                    module-code: demo
                    module-name: 演示
                    declarations:
                      %s:
                        - id: "%s"
                          version: 1
                          biz-key: %s
                          name: 演示资源
                          target-module: demo
                          fields:
                            name:
                              type: STRING
                              value: demo
                """.formatted(resourceType, id, bizKey);
    }

    @Test
    void checkPersistenceAccess_withJdbcTemplate_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service;

                import org.springframework.jdbc.core.JdbcTemplate;

                public class DemoServiceImpl {
                    private JdbcTemplate jdbcTemplate;
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-access");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkMapperSqlStyle_withAnnotationSqlAndCommandParam_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/mapper");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoMapper.java"), """
                package io.mango.demo.core.mapper;

                import io.mango.demo.api.command.CreateDemoCommand;
                import org.apache.ibatis.annotations.Select;

                public interface DemoMapper {
                    @Select("select * from demo where id = #{id}")
                    DemoEntity selectByCommand(CreateDemoCommand command);
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "mapper-sql-style");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceCrudBaseline_withBypassPatterns_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoServiceImpl.java"), """
                package io.mango.demo.core.service;

                import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
                import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
                import io.mango.demo.core.entity.DemoEntity;
                import io.mango.demo.core.mapper.DemoMapper;

                public class DemoServiceImpl extends ServiceImpl<DemoMapper, DemoEntity> {
                    public Object page(Long tenantId) {
                        DemoEntity entity = new DemoEntity();
                        entity.setTenantId(String.valueOf(tenantId));
                        query().eq("created_by", 1001L);
                        return baseMapper.selectPage(new Page<DemoEntity>(1, 10), null);
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-crud-baseline");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkPersistenceCrudBaseline_withApiProtocolTenantSetter_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("SigningWorkflowService.java"), """
                package io.mango.demo.core.service;

                import io.mango.authorization.api.query.RoleLookupQuery;
                import io.mango.demo.core.entity.DemoEntity;

                public class SigningWorkflowService {
                    private RoleLookupQuery fieldQuery;
                    private DemoEntity query;

                    public void initialize(RoleLookupQuery query) {
                        query.setTenantId(1L);
                        this.fieldQuery.setTenantId(1L);
                        new RoleLookupQuery().setTenantId(1L);
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-crud-baseline");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceCrudBaseline_withTenantEntitySetter_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core.service;

                import io.mango.demo.core.entity.DemoEntity;

                public class DemoService {
                    public void create(Long tenantId) {
                        DemoEntity entity = new DemoEntity();
                        entity.setTenantId(String.valueOf(tenantId));
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-crud-baseline");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        org.apache.maven.plugin.MojoExecutionException exception =
                assertThrows(
                        org.apache.maven.plugin.MojoExecutionException.class,
                        () -> mojo.execute());
        assertTrue(exception.getMessage().contains("newIssues=1"));
    }

    @Test
    void checkPersistenceCrudBaseline_withUnknownTenantSetterReceiver_reportsIssue()
            throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core.service;

                public class DemoService {
                    public void initialize(TenantCarrier carrier) {
                        carrier.setTenantId("1");
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-crud-baseline");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(
                org.apache.maven.plugin.MojoExecutionException.class,
                () -> mojo.execute());
    }

    @Test
    void checkPersistenceCrudBaseline_withMangoCrudService_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core.service;

                import io.mango.demo.core.entity.DemoEntity;
                import io.mango.demo.core.mapper.DemoMapper;
                import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;

                public class DemoService extends MangoCrudServiceImpl<DemoMapper, DemoEntity> {
                    @Override
                    protected Class<DemoEntity> entityType() {
                        return DemoEntity.class;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-crud-baseline");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkPersistenceCrudBaseline_withDataScopeApplier_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoService.java"), """
                package io.mango.demo.core.service;

                import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
                import io.mango.demo.core.entity.DemoEntity;
                import io.mango.demo.core.mapper.DemoMapper;
                import io.mango.infra.persistence.api.scope.DataScopeApplier;
                import io.mango.infra.persistence.api.scope.DataScopeMapping;
                import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;

                public class DemoService extends MangoCrudServiceImpl<DemoMapper, DemoEntity> {
                    private final DataScopeApplier dataScopeApplier;

                    public DemoService(DataScopeApplier dataScopeApplier) {
                        this.dataScopeApplier = dataScopeApplier;
                    }

                    @Override
                    protected void applyDataScope(QueryWrapper<DemoEntity> wrapper, Object query) {
                        dataScopeApplier.apply(wrapper, "demo:list", DataScopeMapping.builder()
                                .tableName("demo")
                                .selfField("created_by")
                                .orgField("org_id")
                                .tenantField("tenant_id")
                                .build());
                    }

                    @Override
                    protected Class<DemoEntity> entityType() {
                        return DemoEntity.class;
                    }
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "persistence-crud-baseline");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkServiceContract_withExpandedBusinessParams_reportsIssue() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("IDemoService.java"), """
                package io.mango.demo.core.service;

                public interface IDemoService {
                    void create(String name, String code, Integer sort);
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "service-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void checkServiceContract_withCommandParam_passes() throws Exception {
        // given
        Path sourceDir = tempDir.resolve("demo/src/main/java/io/mango/demo/core/service");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("IDemoService.java"), """
                package io.mango.demo.core.service;

                import io.mango.demo.api.command.CreateDemoCommand;
                import io.mango.demo.api.query.DemoPageQuery;

                public interface IDemoService {
                    Long create(CreateDemoCommand command);
                    Object page(DemoPageQuery query);
                    boolean updateStatus(Long id, Boolean enabled);
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "service-contract");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void checkAll_inBusinessProjectStillRequiresStaticToolchain() throws Exception {
        // given
        Files.createDirectories(tempDir.resolve("business-pmo"));
        Path sourceDir = tempDir.resolve("backend/demo/src/main/java/io/mango/demo/core/mapper");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("DemoMapper.java"), """
                package io.mango.demo.core.mapper;

                import org.apache.ibatis.annotations.Select;

                public interface DemoMapper {
                    @Select("select * from demo")
                    Object selectOne();
                }
                """);

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "all");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then
        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void extractSignature_withValidMethod_returnsSignature() throws Exception {
        // given
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, "public void doSomething(String arg1, int arg2) { }");

        // when
        CheckMojo mojo = new CheckMojo();
        setField(mojo, "rule", "naming");
        setField(mojo, "baseDir", tempDir.toString());
        setField(mojo, "session", null);

        // then - invoke private method via reflection
        Method method = CheckMojo.class.getDeclaredMethod("extractSignature", String.class);
        method.setAccessible(true);
        String signature = (String) method.invoke(mojo, "public void doSomething(String arg1, int arg2) { }");

        assertNotNull(signature);
        assertTrue(signature.contains("doSomething"));
    }

    private CheckIssue issue(String relativeFile, String artifactId) {
        CheckIssue issue = new CheckIssue();
        issue.type = "NAMING";
        issue.severity = "MAJOR";
        issue.file = tempDir.resolve(relativeFile).toString();
        issue.line = 3;
        issue.description = "Mango module artifactId must use kebab-case: " + artifactId;
        issue.rule = "NAMING";
        issue.reference = "naming-rules.md";
        issue.source = "mango-check";
        return issue;
    }

    private void runGit(String... arguments) throws Exception {
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command)
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "git command timed out: " + command);
        assertEquals(0, process.exitValue(), output);
    }
}
