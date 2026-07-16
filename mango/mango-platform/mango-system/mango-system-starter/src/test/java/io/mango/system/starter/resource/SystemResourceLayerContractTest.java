package io.mango.system.starter.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SystemResourceLayerContractTest {

    private static final Pattern DECLARATION = Pattern.compile("(?m)^\\s+- (?:\\{|id:)");
    private static final Pattern ID = Pattern.compile("(?m)[\"']?id[\"']?\\s*:\\s*[\"']?([0-9]{19})");

    @Test
    void requiredAndDemoResourcesRemainSeparatedAndComplete() throws IOException {
        String tenant = resource("META-INF/mango/resources/system-common-tenant.yml");
        String area = resource("META-INF/mango/resources/system-common-area.yml");
        String i18n = resource("META-INF/mango/resources/system-common-i18n.yml");
        String demo = resource("META-INF/mango/demo/system-demo-tenant.yml");

        assertThat(declarationCount(tenant)).isEqualTo(1);
        assertThat(declarationCount(area)).isEqualTo(524);
        assertThat(declarationCount(i18n)).isEqualTo(20);
        assertThat(declarationCount(demo)).isEqualTo(3);
        assertThat(tenant).contains("SYSTEM_TENANT", "tenantCode: { type: STRING, value: default }")
                .doesNotContain("company-a", "company-b", "company-c");
        assertThat(demo).contains("SYSTEM_TENANT", "company-a", "company-b", "company-c");
    }

    @Test
    void systemResourceIdsAreStableAndGloballyUniqueWithinTheModule() throws IOException {
        Set<String> ids = systemResourceIds();

        assertThat(ids).hasSize(548);
    }

    @Test
    void systemResourceIdsDoNotCollideWithOtherRepositoryDeclarations() throws IOException {
        Set<String> systemIds = systemResourceIds();
        Path repositoryRoot = findRepositoryRoot();
        Set<String> matchedSystemIds = new HashSet<>();

        try (var paths = Files.walk(repositoryRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isResourceDeclaration)
                    .forEach(path -> collectSystemIdMatches(path, systemIds, matchedSystemIds));
        }

        assertThat(matchedSystemIds).containsExactlyInAnyOrderElementsOf(systemIds);
    }

    private long declarationCount(String content) {
        return DECLARATION.matcher(content).results().count();
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Set<String> systemResourceIds() throws IOException {
        Set<String> ids = new HashSet<>();
        int total = 0;
        for (String path : new String[]{
                "META-INF/mango/resources/system-common-tenant.yml",
                "META-INF/mango/resources/system-common-area.yml",
                "META-INF/mango/resources/system-common-i18n.yml",
                "META-INF/mango/demo/system-demo-tenant.yml"}) {
            var matcher = ID.matcher(resource(path));
            while (matcher.find()) {
                total++;
                assertThat(ids.add(matcher.group(1))).as("duplicate system resource id %s", matcher.group(1)).isTrue();
            }
        }
        assertThat(total).isEqualTo(548);
        return ids;
    }

    private Path findRepositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("mango-platform")) && Files.exists(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Mango repository root not found");
    }

    private boolean isResourceDeclaration(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/src/main/resources/META-INF/mango/")
                && (normalized.endsWith(".json") || normalized.endsWith(".yml") || normalized.endsWith(".yaml"));
    }

    private void collectSystemIdMatches(Path path, Set<String> systemIds, Set<String> matchedSystemIds) {
        try {
            var matcher = ID.matcher(Files.readString(path, StandardCharsets.UTF_8));
            while (matcher.find()) {
                String id = matcher.group(1);
                if (systemIds.contains(id)) {
                    assertThat(matchedSystemIds.add(id))
                            .as("system resource id %s collides at %s", id, path)
                            .isTrue();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Read resource declaration failed: " + path, e);
        }
    }
}
