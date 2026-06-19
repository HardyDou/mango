package io.mango.authorization.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Menu Baseline")
class MenuBaselineTest {

    private static final String BASELINE_DIR = "menu-baseline/current/";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("current baseline should record stable menu data counts")
    void currentBaseline_counts_matchRecordedData() throws IOException {
        assertDataRows("authorization-menu.tsv", 336);
        assertDataRows("frontend-menu-runtime-config.tsv", 210);
        assertDataRows("authorization-menu-package-item.tsv", 283);
        assertDataRows("authorization-role-menu.tsv", 915);
    }

    @Test
    @DisplayName("current baseline should explicitly record workflow orphan menus")
    void currentBaseline_orphanMenus_recordsKnownWorkflowDefect() throws IOException {
        List<String> rows = readDataRows("orphan-menus.tsv");

        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(row -> row.startsWith("260401\t2604\t流程模板\tworkflow:template")));
        assertTrue(rows.stream().anyMatch(row -> row.startsWith("260402\t2604\t流程定义\tworkflow:definition")));
    }

    @Test
    @DisplayName("resource menu manifests should cover every active baseline menu code")
    void resourceManifests_coverActiveBaselineMenuCodes() throws IOException {
        Set<String> declaredCodes = readDeclaredResourceMenuCodes();
        List<String> missingCodes = readDataRows("authorization-menu.tsv").stream()
                .map(row -> row.split("\t", -1))
                .filter(columns -> "0".equals(columns[19]))
                .map(columns -> columns[7])
                .filter(code -> !code.isBlank())
                .filter(code -> !declaredCodes.contains(code))
                .toList();

        assertEquals(List.of(), missingCodes);
    }

    private static void assertDataRows(String filename, int expectedRows) throws IOException {
        assertEquals(expectedRows, readDataRows(filename).size(), filename);
    }

    private static List<String> readDataRows(String filename) throws IOException {
        InputStream inputStream = MenuBaselineTest.class.getClassLoader()
                .getResourceAsStream(BASELINE_DIR + filename);
        assertNotNull(inputStream, filename + " must exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .toList();
        }
    }

    private static Set<String> readDeclaredResourceMenuCodes() throws IOException {
        Path platformDir = findPlatformDir();
        Set<String> declaredCodes = new HashSet<>();
        try (Stream<Path> paths = Files.walk(platformDir)) {
            List<Path> manifests = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("common-menu.json"))
                    .filter(path -> path.toString().contains("META-INF/mango/resources"))
                    .toList();
            for (Path manifest : manifests) {
                collectDeclaredCodes(OBJECT_MAPPER.readTree(manifest.toFile()), declaredCodes);
            }
        }
        return declaredCodes;
    }

    private static Path findPlatformDir() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("mango-platform");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("mango-platform directory not found");
    }

    private static void collectDeclaredCodes(JsonNode node, Set<String> declaredCodes) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            addText(node.get("menuCode"), declaredCodes);
            addText(node.get("permissionCode"), declaredCodes);
            JsonNode permissions = node.get("permissions");
            if (permissions != null && permissions.isArray()) {
                permissions.forEach(permission -> addText(permission, declaredCodes));
            }
            node.properties().forEach(entry -> collectDeclaredCodes(entry.getValue(), declaredCodes));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectDeclaredCodes(child, declaredCodes));
        }
    }

    private static void addText(JsonNode node, Set<String> codes) {
        if (node != null && node.isTextual() && !node.asText().isBlank()) {
            codes.add(node.asText());
        }
    }
}
