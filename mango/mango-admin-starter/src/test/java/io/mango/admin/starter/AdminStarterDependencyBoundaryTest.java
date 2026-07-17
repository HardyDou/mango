package io.mango.admin.starter;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminStarterDependencyBoundaryTest {

    @Test
    void pomProductionDependenciesUseLocalRuntimeStartersOnly() throws Exception {
        NodeList dependencies = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile())
                .getElementsByTagName("dependency");

        List<String> productionDependencies = new ArrayList<>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            if ("test".equals(childText(dependency, "scope"))) {
                continue;
            }
            String groupId = childText(dependency, "groupId");
            String artifactId = childText(dependency, "artifactId");
            assertTrue(groupId.startsWith("io.mango"), groupId + ":" + artifactId);
            assertTrue(artifactId.endsWith("-starter"), groupId + ":" + artifactId);
            assertFalse(artifactId.endsWith("-starter-remote"), groupId + ":" + artifactId);
            productionDependencies.add(artifactId);
        }

        assertFalse(productionDependencies.isEmpty());
        assertEquals(
                productionDependencies.size(),
                productionDependencies.stream().distinct().count(),
                productionDependencies.toString());
    }

    @Test
    void readmeDependencyInventoryMatchesPom() throws Exception {
        String readme = Files.readString(Path.of("README.md"));
        Pattern dependencyLine = Pattern.compile("(?m)^- `(?<artifact>mango-[a-z0-9-]+-starter)`$");
        List<String> documented = dependencyLine.matcher(readme).results()
                .map(result -> result.group("artifact"))
                .toList();

        assertEquals(productionArtifactIds(), documented);
    }

    @Test
    void resourcesOnlyProvideModuleMetadata() throws IOException {
        Path resources = Path.of("src/main/resources");
        try (Stream<Path> paths = Files.walk(resources)) {
            List<String> resourceFiles = paths
                    .filter(Files::isRegularFile)
                    .map(resources::relativize)
                    .map(Path::toString)
                    .toList();
            assertEquals(List.of("META-INF/mango/module.properties"), resourceFiles);
        }
    }

    @Test
    void moduleMetadataIdentifiesAdminAggregation() throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(
                Path.of("src/main/resources/META-INF/mango/module.properties"))) {
            properties.load(input);
        }

        assertEquals("mango-admin", properties.getProperty("module-name"));
        assertEquals("/admin", properties.getProperty("module-path"));
    }

    @Test
    void productionJavaSourcesRemainEmpty() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            assertTrue(paths.noneMatch(path -> path.toString().endsWith(".java")));
        }
    }

    private static List<String> productionArtifactIds() throws Exception {
        NodeList dependencies = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile())
                .getElementsByTagName("dependency");
        List<String> artifactIds = new ArrayList<>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            if (!"test".equals(childText(dependency, "scope"))) {
                artifactIds.add(childText(dependency, "artifactId"));
            }
        }
        return List.copyOf(artifactIds);
    }

    private static String childText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }
}
