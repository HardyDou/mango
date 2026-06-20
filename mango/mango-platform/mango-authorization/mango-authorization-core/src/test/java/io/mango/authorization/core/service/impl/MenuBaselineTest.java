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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    @DisplayName("current baseline should not contain orphan menus after resource manifest migration")
    void currentBaseline_orphanMenus_isEmptyAfterResourceManifestMigration() throws IOException {
        assertEquals(List.of(), readDataRows("orphan-menus.tsv"));
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

    @Test
    @DisplayName("resource menu public parents should have a single system owner")
    void resourceManifests_publicParents_haveSingleSystemOwner() throws IOException {
        Map<String, List<ResourceMenuEntry>> menusByCode = groupByCode(readDeclaredResourceMenus());

        assertSingleSystemOwner(menusByCode, "system");
        assertSingleSystemOwner(menusByCode, "system:permission");
        assertSingleSystemOwner(menusByCode, "data");
    }

    @Test
    @DisplayName("resource menu cross-file parent dependencies should be resolvable and acyclic")
    void resourceManifests_parentDependencies_resolvableAndAcyclic() throws IOException {
        List<ResourceMenuEntry> menus = readDeclaredResourceMenus();
        Map<String, List<ResourceMenuEntry>> menusByCode = groupByCode(menus);
        List<String> duplicateOwners = menusByCode.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .map(owner -> owner.moduleCode() + ":" + owner.resourceBizKey())
                        .distinct()
                        .count() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        assertEquals(List.of(), duplicateOwners, "menuCode must not be declared by multiple resources");

        List<String> missingParents = new ArrayList<>();
        Map<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
        for (ResourceMenuEntry menu : menus) {
            if (menu.parentCode() == null || menu.parentCode().isBlank()) {
                continue;
            }
            List<ResourceMenuEntry> parentOwners = menusByCode.get(menu.parentCode());
            if (parentOwners == null || parentOwners.isEmpty()) {
                missingParents.add(menu.menuCode() + " -> " + menu.parentCode() + " @ " + menu.sourcePath());
                continue;
            }
            ResourceMenuEntry parent = parentOwners.get(0);
            if (!parent.sourcePath().equals(menu.sourcePath())) {
                dependencyGraph
                        .computeIfAbsent(menu.sourcePath(), ignored -> new LinkedHashSet<>())
                        .add(parent.sourcePath());
            }
        }

        assertEquals(List.of(), missingParents);
        assertEquals(List.of(), findCycles(dependencyGraph));
    }

    @Test
    @DisplayName("resource menu permission items should not reuse page menu codes")
    void resourceManifests_permissionItems_doNotReusePageMenuCodes() throws IOException {
        List<ResourceMenuEntry> menus = readDeclaredResourceMenus();
        Set<String> pageMenuCodes = new HashSet<>();
        Set<String> permissionMenuCodes = new HashSet<>();
        for (ResourceMenuEntry menu : menus) {
            if (menu.permissionItem()) {
                permissionMenuCodes.add(menu.menuCode());
            } else {
                pageMenuCodes.add(menu.menuCode());
            }
        }
        List<String> reusedCodes = permissionMenuCodes.stream()
                .filter(pageMenuCodes::contains)
                .sorted()
                .toList();

        assertEquals(List.of(), reusedCodes);
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
        Set<String> declaredCodes = new HashSet<>();
        Path platformDir = findPlatformDir();
        try (Stream<Path> paths = Files.walk(platformDir)) {
            List<Path> manifests = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("common-menu.json"))
                    .filter(path -> path.toString().contains("META-INF/mango/resources"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList();
            for (Path manifest : manifests) {
                collectDeclaredCodes(OBJECT_MAPPER.readTree(manifest.toFile()), declaredCodes);
            }
        }
        return declaredCodes;
    }

    private static List<ResourceMenuEntry> readDeclaredResourceMenus() throws IOException {
        Path platformDir = findPlatformDir();
        List<ResourceMenuEntry> menus = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(platformDir)) {
            List<Path> manifests = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("common-menu.json"))
                    .filter(path -> path.toString().contains("META-INF/mango/resources"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList();
            for (Path manifest : manifests) {
                JsonNode resource = OBJECT_MAPPER.readTree(manifest.toFile()).path("mango").path("resource");
                String moduleCode = resource.path("moduleCode").asText();
                for (JsonNode declaration : resource.path("declarations").path("AUTH_MENU")) {
                    String resourceBizKey = declaration.path("bizKey").asText();
                    collectDeclaredMenus(
                            declaration.path("fields").path("menus").path("value"),
                            menus,
                            moduleCode,
                            resourceBizKey,
                            platformDir.relativize(manifest).toString(),
                            null);
                }
            }
        }
        return menus;
    }

    private static Map<String, List<ResourceMenuEntry>> groupByCode(List<ResourceMenuEntry> menus) {
        Map<String, List<ResourceMenuEntry>> menusByCode = new HashMap<>();
        for (ResourceMenuEntry menu : menus) {
            menusByCode.computeIfAbsent(menu.menuCode(), ignored -> new ArrayList<>()).add(menu);
        }
        return menusByCode;
    }

    private static void assertSingleSystemOwner(Map<String, List<ResourceMenuEntry>> menusByCode, String menuCode) {
        List<ResourceMenuEntry> owners = menusByCode.get(menuCode);
        assertNotNull(owners, menuCode + " must exist");
        List<String> ownerNames = owners.stream()
                .map(owner -> owner.moduleCode() + ":" + owner.resourceBizKey())
                .distinct()
                .toList();
        assertEquals(List.of("system:system.menu.internal-admin"), ownerNames, menuCode);
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

    private static void collectDeclaredMenus(
            JsonNode menusNode,
            List<ResourceMenuEntry> menus,
            String moduleCode,
            String resourceBizKey,
            String sourcePath,
            String inheritedParentCode) {
        if (!menusNode.isArray()) {
            return;
        }
        for (JsonNode menuNode : menusNode) {
            String menuCode = menuNode.path("menuCode").asText();
            if (menuCode.isBlank()) {
                continue;
            }
            String parentCode = menuNode.path("parentCode").asText();
            menus.add(new ResourceMenuEntry(
                    moduleCode,
                    resourceBizKey,
                    sourcePath,
                    menuCode,
                    parentCode.isBlank() ? inheritedParentCode : parentCode));
            collectDeclaredMenus(menuNode.path("children"), menus, moduleCode, resourceBizKey, sourcePath, menuCode);
            collectDeclaredPermissionMenus(menuNode.path("permissionItems"), menus, moduleCode, resourceBizKey,
                    sourcePath, menuCode);
        }
    }

    private static void collectDeclaredPermissionMenus(
            JsonNode permissionsNode,
            List<ResourceMenuEntry> menus,
            String moduleCode,
            String resourceBizKey,
            String sourcePath,
            String parentCode) {
        if (!permissionsNode.isArray()) {
            return;
        }
        for (JsonNode permissionNode : permissionsNode) {
            String menuCode = permissionNode.path("menuCode").asText();
            if (menuCode.isBlank()) {
                menuCode = permissionNode.path("permissionCode").asText();
            }
            if (menuCode.isBlank()) {
                continue;
            }
            menus.add(new ResourceMenuEntry(
                    moduleCode,
                    resourceBizKey,
                    sourcePath,
                    menuCode,
                    parentCode,
                    true));
        }
    }

    private static List<String> findCycles(Map<String, Set<String>> dependencyGraph) {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<String> cycles = new ArrayList<>();
        for (String node : dependencyGraph.keySet()) {
            detectCycle(node, dependencyGraph, visited, visiting, new ArrayList<>(), cycles);
        }
        return cycles;
    }

    private static void detectCycle(
            String node,
            Map<String, Set<String>> dependencyGraph,
            Set<String> visited,
            Set<String> visiting,
            List<String> path,
            List<String> cycles) {
        if (visited.contains(node)) {
            return;
        }
        if (visiting.contains(node)) {
            int start = path.indexOf(node);
            List<String> cycle = new ArrayList<>(path.subList(start, path.size()));
            cycle.add(node);
            cycles.add(String.join(" -> ", cycle));
            return;
        }
        visiting.add(node);
        path.add(node);
        for (String dependency : dependencyGraph.getOrDefault(node, Set.of())) {
            detectCycle(dependency, dependencyGraph, visited, visiting, path, cycles);
        }
        path.remove(path.size() - 1);
        visiting.remove(node);
        visited.add(node);
    }

    private static void addText(JsonNode node, Set<String> codes) {
        if (node != null && node.isTextual() && !node.asText().isBlank()) {
            codes.add(node.asText());
        }
    }

    private record ResourceMenuEntry(
            String moduleCode,
            String resourceBizKey,
            String sourcePath,
            String menuCode,
            String parentCode,
            boolean permissionItem) {

        ResourceMenuEntry(String moduleCode, String resourceBizKey, String sourcePath, String menuCode, String parentCode) {
            this(moduleCode, resourceBizKey, sourcePath, menuCode, parentCode, false);
        }
    }
}
