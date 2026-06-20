package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    void checkAll_withGenericQualityIssue_runsOnlyMangoSpecificRules() throws Exception {
        // given - generic code quality is delegated to PMD/P3C/Checkstyle, not mango:check
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
        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void resolveStaticAnalysisProjects_withSessionReactor_usesCurrentProjectsInsteadOfFullTree() throws Exception {
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
        Path infraKv = tempDir.resolve("mango-infra/mango-infra-kv");
        Files.createDirectories(jobSupport);
        Files.createDirectories(jobCore);
        Files.createDirectories(infraKv);
        Files.writeString(jobSupport.resolve("pom.xml"), "<project/>");
        Files.writeString(jobCore.resolve("pom.xml"), "<project/>");
        Files.writeString(infraKv.resolve("pom.xml"), "<project/>");

        MavenSession session = mock(MavenSession.class);
        MavenProject rootProject = new MavenProject();
        rootProject.setFile(rootPom.toFile());
        MavenProject supportProject = new MavenProject();
        supportProject.setFile(jobSupport.resolve("pom.xml").toFile());
        MavenProject coreProject = new MavenProject();
        coreProject.setFile(jobCore.resolve("pom.xml").toFile());
        when(session.getProjects()).thenReturn(List.of(rootProject, supportProject, coreProject));

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
                "mango-platform/mango-job/mango-job-core"
        ), projects);
    }

    @Test
    void invokeSingleGoal_whenDelegatedMavenCommandHangs_timesOut() throws Exception {
        // given
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>");
        Path slowMaven = tempDir.resolve("slow-mvn.sh");
        Files.writeString(slowMaven, """
                #!/bin/sh
                sleep 5
                """);
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
    void checkApiContract_withMultipleMethodParameters_reportsIssue() throws Exception {
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

        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
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
    void checkPersistenceSchema_withDisabledTable_passes() throws Exception {
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

        assertDoesNotThrow(() -> mojo.execute());
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
    void checkAll_inBusinessProject_runsBusinessBackendStyleChecks() throws Exception {
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
        assertThrows(org.apache.maven.plugin.MojoExecutionException.class, () -> mojo.execute());
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
}
