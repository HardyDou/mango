package io.mango.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MangoArchitectureEnforcerRuleTest {

    @Test
    void changedPomPathsAreRelativeToNestedMavenRoot(@TempDir Path repository) throws Exception {
        run(repository, "git", "init");
        run(repository, "git", "config", "user.email", "architecture-test@example.com");
        run(repository, "git", "config", "user.name", "Architecture Test");

        Path mavenRoot = repository.resolve("mango");
        Path modulePom = mavenRoot.resolve("demo/pom.xml");
        Files.createDirectories(modulePom.getParent());
        Files.writeString(modulePom, "<project/>\n");
        Files.writeString(repository.resolve("pom.xml"), "<project/>\n");
        run(repository, "git", "add", ".");
        run(repository, "git", "commit", "-m", "baseline");

        Files.writeString(modulePom, "<project><name>demo</name></project>\n");
        Files.writeString(repository.resolve("pom.xml"), "<project><name>outside</name></project>\n");

        Properties userProperties = new Properties();
        userProperties.setProperty("mango.architecture.base", "HEAD");
        MavenSession session = mock(MavenSession.class);
        when(session.getExecutionRootDirectory()).thenReturn(mavenRoot.toString());
        when(session.getUserProperties()).thenReturn(userProperties);
        MangoArchitectureEnforcerRule rule = new MangoArchitectureEnforcerRule(session);

        Method method = MangoArchitectureEnforcerRule.class.getDeclaredMethod("changedPomPaths");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> changed = (Set<String>) method.invoke(rule);

        assertThat(changed).containsExactly("demo/pom.xml");
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
