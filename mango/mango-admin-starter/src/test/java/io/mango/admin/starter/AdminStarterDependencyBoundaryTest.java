package io.mango.admin.starter;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminStarterDependencyBoundaryTest {

    @Test
    void pom_directDependencies_doNotReferenceCoreModules() throws Exception {
        NodeList dependencies = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile())
                .getElementsByTagName("dependency");

        List<String> coreDependencies = new ArrayList<>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            String groupId = childText(dependency, "groupId");
            String artifactId = childText(dependency, "artifactId");
            if (groupId.startsWith("io.mango") && artifactId.endsWith("-core")) {
                coreDependencies.add(groupId + ":" + artifactId);
            }
        }

        assertTrue(coreDependencies.isEmpty(), coreDependencies.toString());
    }

    @Test
    void resources_doNotProvideApplicationConfiguration() throws IOException {
        Path resources = Path.of("src/main/resources");
        if (!Files.exists(resources)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(resources)) {
            List<Path> applicationConfigs = paths
                    .filter(Files::isRegularFile)
                    .filter(AdminStarterDependencyBoundaryTest::isApplicationConfig)
                    .toList();

            assertTrue(applicationConfigs.isEmpty(), applicationConfigs.toString());
        }
    }

    @Test
    void classpathApplicationConfigs_doNotSetServerProperties() throws IOException {
        List<String> configNames = List.of("application.yml", "application.yaml", "application.properties");
        List<String> serverPropertyResources = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String configName : configNames) {
            try (InputStream inputStream = classLoader.getResourceAsStream(configName)) {
                if (inputStream == null) {
                    continue;
                }
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                if (content.lines().anyMatch(AdminStarterDependencyBoundaryTest::isServerPropertyLine)) {
                    serverPropertyResources.add(configName);
                }
            }
        }

        assertTrue(serverPropertyResources.isEmpty(), serverPropertyResources.toString());
    }

    private static boolean isApplicationConfig(Path path) {
        String filename = path.getFileName().toString();
        return List.of("application.yml", "application.yaml", "application.properties").contains(filename);
    }

    private static boolean isServerPropertyLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("server.") || trimmed.startsWith("server:");
    }

    private static String childText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }
}
