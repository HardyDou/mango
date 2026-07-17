package io.mango.plugin.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.mango.architecture.ArchitectureIssue;
import io.mango.architecture.ModuleRole;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchitectureMojoTest {

    @Test
    void configurablePathsUseMavenBindableFileType() throws Exception {
        for (String fieldName :
                List.of(
                        "reportFile",
                        "rootDirectory",
                        "debtBaselineFile",
                        "globalEntityManifest")) {
            assertEquals(
                    File.class,
                    ArchitectureMojo.class.getDeclaredField(fieldName).getType(),
                    fieldName);
        }
    }

    @Test
    void changedPathsAreRelativeToNestedMavenRoot(@TempDir Path repository) throws Exception {
        run(repository, "git", "init");
        run(repository, "git", "config", "user.email", "architecture-test@example.com");
        run(repository, "git", "config", "user.name", "Architecture Test");

        Path mavenRoot = repository.resolve("mango");
        Path source = mavenRoot.resolve("demo/src/main/java/example/Demo.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package example; final class Demo {}\n");
        Files.writeString(mavenRoot.resolve("pom.xml"), "<project/>\n");
        Path deleted = mavenRoot.resolve("deleted/src/main/java/example/Deleted.java");
        Files.createDirectories(deleted.getParent());
        Files.writeString(deleted, "package example; final class Deleted {}\n");
        Path manifest = repository.resolve("business-pmo/global-entity-exceptions.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, "{}\n");
        Files.writeString(repository.resolve("README.md"), "outside\n");
        run(repository, "git", "add", ".");
        run(repository, "git", "commit", "-m", "baseline");

        Files.writeString(source, "package example; final class Demo { int value; }\n");
        Path untracked = mavenRoot.resolve("new-module/src/main/java/example/NewDemo.java");
        Files.createDirectories(untracked.getParent());
        Files.writeString(untracked, "package example; final class NewDemo {}\n");
        Files.delete(deleted);
        Files.writeString(manifest, "{\"version\":1}\n");
        Files.writeString(repository.resolve("README.md"), "changed outside\n");

        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "rootDirectory", mavenRoot.toFile());
        setField(mojo, "gitBase", "HEAD");
        setField(mojo, "globalEntityManifest", manifest.toFile());

        Method method = ArchitectureMojo.class.getDeclaredMethod("gitChangedPaths");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> changed = (Set<String>) method.invoke(mojo);

        assertEquals(Set.of(
                "demo/src/main/java/example/Demo.java",
                "new-module/src/main/java/example/NewDemo.java",
                "deleted/src/main/java/example/Deleted.java",
                "../business-pmo/global-entity-exceptions.json"), changed);
    }

    @Test
    void incompleteReactorFailsClosed() throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "requireFullReactor", true);
        setField(mojo, "requiredReactorArtifacts", List.of("order-api", "order-core"));

        var error = assertThrows(
                org.apache.maven.plugin.MojoExecutionException.class,
                () -> mojo.validateReactorScope(
                        Set.of("order-api"), Set.of("order-api", "order-core", "billing-core")));

        assertTrue(error.getMessage().contains("MANGO-ARCH-ENGINE-011"));
        assertTrue(error.getMessage().contains("order-core"));
        assertTrue(error.getMessage().contains("billing-core"));
    }

    @Test
    void allCompileSourceRootsAreCollected(@TempDir Path projectRoot) throws Exception {
        Path main = projectRoot.resolve("src/main/java/example/Main.java");
        Path generated = projectRoot.resolve("target/generated-sources/example/Generated.java");
        Path classes = projectRoot.resolve("target/classes");
        Files.createDirectories(main.getParent());
        Files.createDirectories(generated.getParent());
        Files.createDirectories(classes);
        Files.writeString(main, "package example; final class Main {}\n");
        Files.writeString(generated, "package example; final class Generated {}\n");

        Build build = new Build();
        build.setSourceDirectory(projectRoot.resolve("src/main/java").toString());
        build.setOutputDirectory(classes.toString());
        MavenProject project = new MavenProject();
        project.setGroupId("io.mango.test");
        project.setArtifactId("demo-core");
        project.setBuild(build);
        Path pom = projectRoot.resolve("pom.xml");
        Files.writeString(pom, "<project/>\n");
        project.setFile(pom.toFile());
        project.addCompileSourceRoot(projectRoot.resolve("target/generated-sources").toString());

        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "excludedModules", List.of());
        setField(mojo, "rootDirectory", projectRoot.toFile());
        Map<Path, ModuleRole> classDirectories = new LinkedHashMap<>();
        Map<Path, String> classDirectoryArtifacts = new LinkedHashMap<>();
        Map<Path, String> classDirectoryModules = new LinkedHashMap<>();
        List<Path> sourceDirectories = new ArrayList<>();
        Method collect = ArchitectureMojo.class.getDeclaredMethod(
                "collectJavaInputs",
                MavenProject.class,
                Map.class,
                Map.class,
                Map.class,
                List.class);
        collect.setAccessible(true);
        collect.invoke(
                mojo,
                project,
                classDirectories,
                classDirectoryArtifacts,
                classDirectoryModules,
                sourceDirectories);

        assertEquals(Set.of(
                projectRoot.resolve("src/main/java").toAbsolutePath().normalize(),
                projectRoot.resolve("target/generated-sources").toAbsolutePath().normalize()),
                Set.copyOf(sourceDirectories));
        assertEquals(Set.of(classes.toAbsolutePath().normalize()), classDirectories.keySet());
        assertEquals(Map.of(classes.toAbsolutePath().normalize(), "."), classDirectoryModules);
    }

    @Test
    void dependencyOnlyReactorKeepsDependencyArchitectureWithoutJavaSources(
            @TempDir Path root) throws Exception {
        MavenProject project = project(root, "mango-admin-starter", "mango-admin-starter");
        project.setGroupId("io.mango");
        Build build = new Build();
        build.setSourceDirectory(
                root.resolve("mango-admin-starter/src/main/java").toString());
        build.setOutputDirectory(
                root.resolve("mango-admin-starter/target/classes").toString());
        project.setBuild(build);
        Dependency forbidden = new Dependency();
        forbidden.setGroupId("io.mango.platform.order");
        forbidden.setArtifactId("mango-order-core");
        forbidden.setVersion("1.0.0-SNAPSHOT");
        project.setDependencies(List.of(forbidden));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));

        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "session", session);
        setField(mojo, "rootDirectory", root.toFile());
        setField(mojo, "excludedModules", List.of());
        setField(mojo, "businessGroupPrefixes", List.of());
        setField(mojo, "requireFullReactor", false);

        Method collect = ArchitectureMojo.class.getDeclaredMethod("collectReactorInputs");
        collect.setAccessible(true);
        Object inputs = collect.invoke(mojo);
        Method sourceDirectories = inputs.getClass().getDeclaredMethod("sourceDirectories");
        sourceDirectories.setAccessible(true);
        Method dependencyIssues = inputs.getClass().getDeclaredMethod("dependencyIssues");
        dependencyIssues.setAccessible(true);

        assertTrue(((List<?>) sourceDirectories.invoke(inputs)).isEmpty());
        @SuppressWarnings("unchecked")
        List<ArchitectureIssue> issues =
                (List<ArchitectureIssue>) dependencyIssues.invoke(inputs);
        assertEquals(List.of("MANGO-ARCH-DEP-007"),
                issues.stream().map(ArchitectureIssue::ruleId).toList());
    }

    @Test
    void moduleExclusionsFailClosed() throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "excludedModules", List.of("order-core"));

        var error = assertThrows(
                org.apache.maven.plugin.MojoExecutionException.class, mojo::execute);

        assertTrue(error.getMessage().contains("MANGO-ARCH-ENGINE-013"));
    }

    @Test
    void skipFlagFailsClosed() throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "skip", true);

        var error = assertThrows(
                org.apache.maven.plugin.MojoExecutionException.class, mojo::execute);

        assertTrue(error.getMessage().contains("MANGO-ARCH-ENGINE-015"));
    }

    @Test
    void governedVerificationCannotDisableFullReactor() throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "lockFullReactor", true);
        setField(mojo, "requireFullReactor", false);
        setField(mojo, "excludedModules", List.of());

        var error = assertThrows(
                org.apache.maven.plugin.MojoExecutionException.class, mojo::execute);

        assertTrue(error.getMessage().contains("MANGO-ARCH-ENGINE-016"));
    }

    @Test
    void governedVerificationCannotDowngradeFullMode() throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "lockFullMode", true);
        setField(mojo, "mode", "changed");
        setField(mojo, "excludedModules", List.of());

        var error = assertThrows(
                org.apache.maven.plugin.MojoExecutionException.class, mojo::execute);

        assertTrue(error.getMessage().contains("MANGO-ARCH-ENGINE-018"));
    }

    @Test
    void changedParentPomImpactsAllDescendantProjects(@TempDir Path root) throws Exception {
        MavenProject parent = project(root, "backend", "backend-parent");
        MavenProject order = project(root, "backend/modules/order", "order-parent");
        MavenProject orderCore = project(root, "backend/modules/order/order-core", "order-core");
        MavenProject billing = project(root, "backend/modules/billing", "billing-core");
        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(parent, order, orderCore, billing));

        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "rootDirectory", root.resolve("backend").toFile());
        setField(mojo, "session", session);

        assertEquals(Set.of("backend-parent", "order-parent", "order-core", "billing-core"),
                mojo.impactedArtifactsForChangedPoms(Set.of("pom.xml")));
        assertEquals(Set.of("order-parent", "order-core"),
                mojo.impactedArtifactsForChangedPoms(Set.of("modules/order/pom.xml")));
    }

    @Test
    void pomImpactFingerprintIgnoresBuildPluginsButDetectsArchitectureModelChanges()
            throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        String baseline = """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>root</artifactId>
                  <packaging>pom</packaging>
                  <modules><module>module-a</module></modules>
                </project>
                """;
        String pluginOnly = baseline.replace("</project>", """
                  <build><plugins><plugin>
                    <groupId>com.github.spotbugs</groupId>
                    <artifactId>spotbugs-maven-plugin</artifactId>
                  </plugin></plugins></build>
                </project>
                """);
        String dependency = baseline.replace("</project>", """
                  <dependencies><dependency>
                    <groupId>com.example</groupId>
                    <artifactId>contract-api</artifactId>
                  </dependency></dependencies>
                </project>
                """);
        String module = baseline.replace("module-a", "module-b");

        assertTrue(!mojo.architectureRelevantPomChange(baseline, pluginOnly));
        assertTrue(mojo.architectureRelevantPomChange(baseline, dependency));
        assertTrue(mojo.architectureRelevantPomChange(baseline, module));
    }

    @Test
    void moduleMetadataChangeImpactsEveryClassInTheSameDomain() {
        ArchitectureMojo mojo = new ArchitectureMojo();
        Map<String, String> classArtifacts = Map.of(
                "com.example.order.OrderApi", "order-api",
                "com.example.order.OrderService", "order-core",
                "com.example.billing.BillingService", "billing-core");

        assertEquals(Set.of("com.example.order.OrderApi", "com.example.order.OrderService"),
                mojo.impactedArtifactClasses(classArtifacts, Set.of(), Set.of("order")));
    }

    @Test
    void globalEntityManifestChangePromotesOnlyEntityRules(@TempDir Path root) throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "rootDirectory", root.toFile());
        ArchitectureIssue entity = new ArchitectureIssue(
                "MANGO-ARCH-ENTITY-003", "com.example.OrderEntity", "entity");
        ArchitectureIssue controller = new ArchitectureIssue(
                "MANGO-ARCH-CTRL-002", "com.example.OrderController", "controller");

        assertTrue(mojo.isChangedIssue(entity, Set.of(), Set.of(), Set.of(),
                Set.of(), Set.of(), Set.of("com.example.OrderEntity")));
        assertTrue(!mojo.isChangedIssue(controller, Set.of(), Set.of(), Set.of(),
                Set.of(), Set.of(), Set.of("com.example.OrderEntity")));
    }

    @Test
    void businessSourcesCannotShadowKernelOrFrameworkNamespaces(@TempDir Path root)
            throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "businessGroupPrefixes", List.of("com.example"));
        for (String packageName : List.of(
                "io.mango.common.result",
                "com.baomidou.mybatisplus.core.mapper",
                "org.springframework.web.bind.annotation")) {
            Path sourceRoot = root.resolve(packageName.replace('.', '/'));
            Files.createDirectories(sourceRoot);
            Files.writeString(sourceRoot.resolve("Shadow.java"),
                    "package " + packageName + "; final class Shadow {}\n");

            var error = assertThrows(
                    org.apache.maven.plugin.MojoExecutionException.class,
                    () -> mojo.validateReservedNamespaces(List.of(root)));
            assertTrue(error.getMessage().contains("MANGO-ARCH-ENGINE-017"));
            Files.delete(sourceRoot.resolve("Shadow.java"));
        }
    }

    @Test
    void readmeOnlyChangeDoesNotPromoteHistoricalJavaIssue(@TempDir Path root) throws Exception {
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "rootDirectory", root.toFile());
        ArchitectureIssue historical = new ArchitectureIssue(
                "MANGO-ARCH-TYPE-003", "com.example.OrderController", "historical");
        ArchitectureIssue generated = new ArchitectureIssue(
                "MANGO-ARCH-CTRL-002",
                root.resolve("demo/target/generated-sources/OrderController.java:1").toString(),
                "generated");

        assertTrue(!mojo.isChangedIssue(
                historical,
                Set.of("demo/README.md"),
                Set.of(), Set.of(), Set.of(), Set.of()));
        assertTrue(mojo.isChangedIssue(
                generated,
                Set.of("demo/README.md"),
                Set.of(), Set.of(), Set.of(), Set.of()));
    }

    @Test
    void baseBudgetKeepsHistoricalIdentityButHeadReplacementStillBlocks(
            @TempDir Path repository)
            throws Exception {
        run(repository, "git", "init");
        run(repository, "git", "config", "user.email", "architecture-test@example.com");
        run(repository, "git", "config", "user.name", "Architecture Test");
        Path mavenRoot = repository.resolve("mango");
        Files.createDirectories(mavenRoot);
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "rootDirectory", mavenRoot.toFile());
        ArchitectureIssue historical = new ArchitectureIssue(
                "MANGO-ARCH-BEAN-001", "com.example.OrderService", "missing @Service");
        ArchitectureIssue replacement = new ArchitectureIssue(
                "MANGO-ARCH-BEAN-001", "com.example.PaymentService", "missing @Service");

        Method identityMethod =
                ArchitectureMojo.class.getDeclaredMethod("issueIdentity", ArchitectureIssue.class);
        identityMethod.setAccessible(true);
        String historicalIdentity = (String) identityMethod.invoke(mojo, historical);
        String replacementIdentity = (String) identityMethod.invoke(mojo, replacement);
        Path baseline = repository.resolve(
                "mango-pmo/baselines/architecture/debt-budget.json");
        Files.createDirectories(baseline.getParent());
        Files.writeString(
                baseline,
                "{\"schemaVersion\":3,\"identities\":{\""
                        + historicalIdentity
                        + "\":1}}\n");
        run(repository, "git", "add", ".");
        run(repository, "git", "commit", "-m", "base budget");

        Files.writeString(
                baseline,
                "{\"schemaVersion\":3,\"identities\":{\""
                        + replacementIdentity
                        + "\":1}}\n");
        setField(mojo, "debtBaselineFile", baseline.toFile());
        setField(mojo, "resolvedGitBase", "HEAD");

        Method filter = ArchitectureMojo.class.getDeclaredMethod(
                "newIssuesAgainstDebtBaseline", List.class);
        filter.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ArchitectureIssue> unchanged =
                (List<ArchitectureIssue>) filter.invoke(mojo, List.of(historical));
        @SuppressWarnings("unchecked")
        List<ArchitectureIssue> replaced =
                (List<ArchitectureIssue>) filter.invoke(mojo, List.of(replacement));

        assertTrue(unchanged.isEmpty());
        assertEquals(List.of(replacement), replaced);
    }

    @Test
    void pmdIdentityUsesSourceContentInsteadOfMutableLineNumber(@TempDir Path repository)
            throws Exception {
        Path mavenRoot = repository.resolve("mango");
        Path source = mavenRoot.resolve("demo/src/main/java/example/Demo.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class Demo {}\n");
        ArchitectureMojo mojo = new ArchitectureMojo();
        setField(mojo, "rootDirectory", mavenRoot.toFile());
        Method identityMethod =
                ArchitectureMojo.class.getDeclaredMethod("issueIdentity", ArchitectureIssue.class);
        identityMethod.setAccessible(true);
        ArchitectureIssue first = new ArchitectureIssue(
                "MANGO-ARCH-DEMO-001", source + ":1", "demo violation");
        String firstIdentity = (String) identityMethod.invoke(mojo, first);

        Files.writeString(source, "\nclass Demo {}\n");
        ArchitectureIssue shifted = new ArchitectureIssue(
                "MANGO-ARCH-DEMO-001", source + ":2", "demo violation");
        String shiftedIdentity = (String) identityMethod.invoke(mojo, shifted);

        assertEquals(firstIdentity, shiftedIdentity);
    }

    @Test
    void pmdUsesHighestConfiguredReactorJavaVersion() {
        MavenProject java17 = new MavenProject();
        java17.getProperties().setProperty("maven.compiler.release", "17");
        MavenProject java21 = new MavenProject();
        java21.getProperties().setProperty("maven.compiler.source", "21");

        assertEquals("21", new ArchitectureMojo().resolveJavaVersion(List.of(java17, java21)));
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private MavenProject project(Path root, String relative, String artifactId) throws Exception {
        Path basedir = root.resolve(relative);
        Files.createDirectories(basedir);
        Path pom = basedir.resolve("pom.xml");
        Files.writeString(pom, "<project/>\n");
        MavenProject project = new MavenProject();
        project.setFile(pom.toFile());
        project.setArtifactId(artifactId);
        return project;
    }

    private void run(Path directory, String... command) throws Exception {
        Process process = new ProcessBuilder(List.of(command))
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try (var reader = process.inputReader()) {
            output = reader.lines().reduce("", (left, right) -> left + right + System.lineSeparator());
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + output);
        }
    }
}
