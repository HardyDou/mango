package io.mango.plugin.check;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MangoBomCoverageTest {

    @Test
    void bomCoversPublishedMangoJarsAndExplicitReactorDependencies() throws Exception {
        Path repositoryRoot = findRepositoryRoot();
        Path mavenRoot = repositoryRoot.resolve("mango");
        Model bom = readModel(mavenRoot.resolve("mango-bom/pom.xml"));
        Set<DependencyKey> managedDependencies = managedDependencyKeys(bom);

        List<DependencyKey> missingMangoJars = new ArrayList<>();
        List<DependencyKey> unmanagedExplicitDependencies = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(mavenRoot)) {
            for (Path pom : paths.filter(this::isReactorPom).toList()) {
                Model model = readModel(pom);
                if (isPublishedMangoJar(mavenRoot, pom, model)) {
                    DependencyKey projectKey = new DependencyKey(
                            effectiveGroupId(model), model.getArtifactId(), "jar", "");
                    if (!managedDependencies.contains(projectKey)) {
                        missingMangoJars.add(projectKey);
                    }
                }

                List<Dependency> dependencies = new ArrayList<>(model.getDependencies());
                for (Profile profile : model.getProfiles()) {
                    dependencies.addAll(profile.getDependencies());
                }
                for (Dependency dependency : dependencies) {
                    if (hasText(dependency.getVersion())) {
                        DependencyKey dependencyKey = DependencyKey.from(dependency);
                        if (!managedDependencies.contains(dependencyKey)) {
                            unmanagedExplicitDependencies.add(dependencyKey);
                        }
                    }
                }
            }
        }

        assertTrue(missingMangoJars.isEmpty(),
                "Every published non-app Mango jar must be managed by mango-bom: " + missingMangoJars);
        assertTrue(unmanagedExplicitDependencies.isEmpty(),
                "Every explicit reactor dependency coordinate must be managed by mango-bom: "
                        + unmanagedExplicitDependencies);
        assertEquals("11.20.3", bom.getProperties().getProperty("flyway.version"),
                "The MySQL 8.4 compatible Flyway version must be owned by mango-bom");
        assertEquals("3.27.0", bom.getProperties().getProperty("redisson.version"),
                "The validated Redisson version must be owned by mango-bom");
    }

    @Test
    void parentDelegatesDependencyManagementToBom() throws Exception {
        Path repositoryRoot = findRepositoryRoot();
        Model parent = readModel(repositoryRoot.resolve("mango/mango-parent/pom.xml"));

        List<Dependency> dependencies = parent.getDependencyManagement().getDependencies();
        assertEquals(1, dependencies.size(), "mango-parent must import one dependency catalog");
        Dependency dependency = dependencies.get(0);
        assertEquals("io.mango", dependency.getGroupId());
        assertEquals("mango-bom", dependency.getArtifactId());
        assertEquals("pom", dependency.getType());
        assertEquals("import", dependency.getScope());

        Model bom = readModel(repositoryRoot.resolve("mango/mango-bom/pom.xml"));
        assertEquals(bom.getProperties().getProperty("spring-boot.version"),
                parent.getProperties().getProperty("spring-boot.version"),
                "The Spring Boot Maven plugin baseline must match mango-bom");
    }

    private Set<DependencyKey> managedDependencyKeys(Model bom) {
        Set<DependencyKey> keys = new HashSet<>();
        List<DependencyKey> duplicates = new ArrayList<>();
        for (Dependency dependency : bom.getDependencyManagement().getDependencies()) {
            DependencyKey key = DependencyKey.from(dependency);
            if (!keys.add(key)) {
                duplicates.add(key);
            }
        }
        assertTrue(duplicates.isEmpty(), "mango-bom must not contain duplicate dependency keys: " + duplicates);
        return keys;
    }

    private boolean isReactorPom(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return path.getFileName().toString().equals("pom.xml")
                && !normalized.contains("/target/")
                && !normalized.contains("/src/");
    }

    private boolean isPublishedMangoJar(Path mavenRoot, Path pom, Model model) {
        Path relativePath = mavenRoot.relativize(pom);
        String normalized = relativePath.toString().replace('\\', '/');
        String moduleName = pom.getParent().getFileName().toString();
        String packaging = hasText(model.getPackaging()) ? model.getPackaging() : "jar";
        return !normalized.startsWith("mango-app/")
                && !moduleName.endsWith("-test")
                && "jar".equals(packaging)
                && effectiveGroupId(model).startsWith("io.mango");
    }

    private String effectiveGroupId(Model model) {
        if (hasText(model.getGroupId())) {
            return model.getGroupId();
        }
        return model.getParent().getGroupId();
    }

    private Model readModel(Path pom) throws IOException {
        try (Reader reader = Files.newBufferedReader(pom)) {
            return new MavenXpp3Reader().read(reader);
        } catch (Exception exception) {
            throw new IOException("Failed to read Maven model: " + pom, exception);
        }
    }

    private Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("mango/pom.xml"))
                    && Files.isDirectory(current.resolve("mango-pmo"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate the Mango repository root");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record DependencyKey(String groupId, String artifactId, String type, String classifier) {
        private static DependencyKey from(Dependency dependency) {
            String type = dependency.getType() == null || dependency.getType().isBlank()
                    ? "jar" : dependency.getType();
            String classifier = dependency.getClassifier() == null
                    ? "" : dependency.getClassifier();
            return new DependencyKey(
                    dependency.getGroupId(), dependency.getArtifactId(), type, classifier);
        }
    }
}
