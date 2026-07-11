package io.mango.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MangoPmdCheckerTest {

    @Test
    void standardWorktreeAndOrdinarySourcePathsProduceIdenticalResults(
            @TempDir Path temporaryDirectory) throws Exception {
        String source = """
                package example;
                final class R<T> {}
                final class BadServiceImpl { R<String> create() { return null; } }
                """;
        Path ordinary = temporaryDirectory.resolve("ordinary/src/main/java");
        Path worktree = temporaryDirectory.resolve(".mango/worktrees/demo/src/main/java");
        Files.createDirectories(ordinary);
        Files.createDirectories(worktree);
        Files.writeString(ordinary.resolve("BadServiceImpl.java"), source);
        Files.writeString(worktree.resolve("BadServiceImpl.java"), source);

        MangoPmdChecker checker = new MangoPmdChecker();
        List<ArchitectureIssue> ordinaryIssues = checker.check(List.of(ordinary), "21");
        List<ArchitectureIssue> worktreeIssues = checker.check(List.of(worktree), "21");

        assertThat(normalize(ordinaryIssues)).containsExactly(
                "MANGO-ARCH-SVC-001|BadServiceImpl.java:3");
        assertThat(normalize(worktreeIssues)).isEqualTo(normalize(ordinaryIssues));
    }

    private List<String> normalize(List<ArchitectureIssue> issues) {
        return issues.stream().map(issue -> {
            String subject = issue.subject().replace('\\', '/');
            return issue.ruleId() + "|" + subject.substring(subject.lastIndexOf('/') + 1);
        }).toList();
    }
}
