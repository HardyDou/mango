package io.mango.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MangoPmdCheckerTest {

    @Test
    void standardWorktreeAndOrdinarySourcePathsProduceIdenticalResults(
            @TempDir Path temporaryDirectory) throws Exception {
        String source = """
                package example;
                import io.mango.common.result.R;
                final class BadService { R<String> create() { return null; } }
                """;
        Path ordinary = temporaryDirectory.resolve("ordinary/src/main/java");
        Path worktree = temporaryDirectory.resolve("task-worktree/src/main/java");
        Files.createDirectories(ordinary);
        Files.createDirectories(worktree);
        Files.writeString(ordinary.resolve("BadService.java"), source);
        Files.writeString(worktree.resolve("BadService.java"), source);

        MangoPmdChecker checker = new MangoPmdChecker();
        Path commonClasspath = Path.of(io.mango.common.result.R.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        List<ArchitectureIssue> ordinaryIssues = checker.check(
                List.of(ordinary), "21", List.of(commonClasspath));
        List<ArchitectureIssue> worktreeIssues = checker.check(
                List.of(worktree), "21", List.of(commonClasspath));

        assertThat(normalize(ordinaryIssues)).containsExactly(
                "MANGO-ARCH-SVC-001|BadService.java:3");
        assertThat(normalize(worktreeIssues)).isEqualTo(normalize(ordinaryIssues));
    }

    @Test
    void resolvesRequireAndBizCodeFromExternalDependencyJar(@TempDir Path temporaryDirectory)
            throws Exception {
        Path dependencyJar = createResultContractJar(temporaryDirectory);
        Path sources = temporaryDirectory.resolve("business/src/main/java/example");
        Files.createDirectories(sources);
        Files.writeString(sources.resolve("OrderService.java"), """
                package example;
                import io.mango.common.result.Require;
                final class OrderService {
                    public void create(Object value) { Require.notNull(value, "BAD"); }
                }
                """);

        List<ArchitectureIssue> issues = new MangoPmdChecker().check(
                List.of(temporaryDirectory.resolve("business/src/main/java")),
                "21",
                List.of(dependencyJar));

        assertThat(issues).extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-003");
        assertThat(issues.get(0).message()).contains("resolved=<missing>");
    }

    @Test
    void pmdSuppressionsCannotDisableMangoArchitectureRules(@TempDir Path temporaryDirectory)
            throws Exception {
        Path sources = temporaryDirectory.resolve("business/src/main/java/example");
        Files.createDirectories(sources);
        Files.writeString(sources.resolve("AnnotationRuleService.java"), """
                package example;
                import io.mango.common.result.R;
                @SuppressWarnings("PMD.MangoJavaArchitecture")
                final class AnnotationRuleService {
                    R<String> create() { return null; }
                }
                """);
        Files.writeString(sources.resolve("AnnotationAllService.java"), """
                package example;
                import io.mango.common.result.R;
                @SuppressWarnings("PMD")
                final class AnnotationAllService {
                    R<String> create() { return null; }
                }
                """);
        Files.writeString(sources.resolve("NoPmdService.java"), """
                package example;
                import io.mango.common.result.R;
                final class NoPmdService {
                    R<String> create() { return null; } // NOPMD
                }
                """);
        Path commonClasspath = Path.of(io.mango.common.result.R.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());

        List<ArchitectureIssue> issues = new MangoPmdChecker().check(
                List.of(temporaryDirectory.resolve("business/src/main/java")),
                "21",
                List.of(commonClasspath));

        assertThat(issues).extracting(ArchitectureIssue::ruleId)
                .containsExactly(
                        "MANGO-ARCH-SVC-001",
                        "MANGO-ARCH-SVC-001",
                        "MANGO-ARCH-SVC-001");
        assertThat(issues).allMatch(issue ->
                issue.message().contains("PMD suppression is forbidden"));
    }

    @Test
    void java21PatternSwitchIsParsedWhenReactorTargetsJava21(@TempDir Path temporaryDirectory)
            throws Exception {
        Path sources = temporaryDirectory.resolve("business/src/main/java/example");
        Files.createDirectories(sources);
        Files.writeString(sources.resolve("PatternSwitchParser.java"), """
                package example;
                final class PatternSwitchParser {
                    String render(Object value) {
                        return switch (value) {
                            case String text -> text;
                            default -> "";
                        };
                    }
                }
                """);

        List<ArchitectureIssue> issues = new MangoPmdChecker().check(
                List.of(temporaryDirectory.resolve("business/src/main/java")),
                "21",
                List.of());

        assertThat(issues).isEmpty();
    }

    private Path createResultContractJar(Path temporaryDirectory) throws Exception {
        Path sourceRoot = temporaryDirectory.resolve("dependency-source/io/mango/common/result");
        Path classes = temporaryDirectory.resolve("dependency-classes");
        Files.createDirectories(sourceRoot);
        Files.createDirectories(classes);
        Path bizCode = sourceRoot.resolve("BizCode.java");
        Path require = sourceRoot.resolve("Require.java");
        Files.writeString(bizCode, """
                package io.mango.common.result;
                public interface BizCode {}
                """);
        Files.writeString(require, """
                package io.mango.common.result;
                public final class Require {
                    public static void notNull(Object value, BizCode code) {}
                    public static void notNull(Object value, String code) {}
                }
                """);
        int compilation = ToolProvider.getSystemJavaCompiler().run(
                null, null, null, "-d", classes.toString(), bizCode.toString(), require.toString());
        assertThat(compilation).isZero();

        Path jar = temporaryDirectory.resolve("mango-result-contract.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
                var paths = Files.walk(classes)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                output.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
        return jar;
    }

    private List<String> normalize(List<ArchitectureIssue> issues) {
        return issues.stream().map(issue -> {
            String subject = issue.subject().replace('\\', '/');
            return issue.ruleId() + "|" + subject.substring(subject.lastIndexOf('/') + 1);
        }).toList();
    }
}
