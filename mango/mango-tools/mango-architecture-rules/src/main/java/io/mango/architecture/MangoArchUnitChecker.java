package io.mango.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Bytecode architecture checks. Every Reactor class directory is imported in one ArchUnit pass. */
public final class MangoArchUnitChecker {

    private static final String REST_CONTROLLER =
            "org.springframework.web.bind.annotation.RestController";
    private static final String SERVICE = "org.springframework.stereotype.Service";
    private static final String MAPPER = "org.apache.ibatis.annotations.Mapper";
    private static final String TABLE_NAME = "com.baomidou.mybatisplus.annotation.TableName";
    private static final String FEIGN_CLIENT =
            "org.springframework.cloud.openfeign.FeignClient";
    private static final String PERSISTENCE_BASE_PACKAGE = "io.mango.infra.persistence.api.entity.";
    private final Set<String> allowedReverseControllers;

    public MangoArchUnitChecker() {
        this(Set.of());
    }

    public MangoArchUnitChecker(Set<String> allowedReverseControllers) {
        this.allowedReverseControllers = Set.copyOf(allowedReverseControllers);
    }

    public List<ArchitectureIssue> check(Map<Path, ModuleRole> classDirectories) {
        Map<Path, ModuleRole> normalized = normalizeAndValidate(classDirectories);
        JavaClasses classes;
        try {
            classes = new ClassFileImporter().importPaths(normalized.keySet());
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-002 ArchUnit failed to import Reactor bytecode", exception);
        }
        if (classes.isEmpty()) {
            throw new IllegalStateException("MANGO-ARCH-ENGINE-003 ArchUnit imported zero classes");
        }
        return check(classes, javaClass -> roleOf(javaClass, normalized));
    }

    public List<ArchitectureIssue> check(
            JavaClasses classes, Function<JavaClass, ModuleRole> roleResolver) {
        List<ArchitectureIssue> issues = new ArrayList<>();
        for (JavaClass javaClass : classes) {
            ModuleRole role = roleResolver.apply(javaClass);
            if (isController(javaClass)) {
                checkController(javaClass, role, issues);
            }
            if (isServiceImplementation(javaClass)) {
                checkService(javaClass, role, issues);
            }
            if (isMapper(javaClass) && role != ModuleRole.CORE) {
                add(issues, "MANGO-ARCH-TYPE-006", javaClass, "Mapper must be located in core");
            }
            if (isEntity(javaClass) && role != ModuleRole.CORE
                    && !javaClass.getName().startsWith(PERSISTENCE_BASE_PACKAGE)) {
                add(issues, "MANGO-ARCH-TYPE-007", javaClass, "Entity must be located in core");
            }
            if (javaClass.isAnnotatedWith(FEIGN_CLIENT) && role != ModuleRole.STARTER_REMOTE) {
                add(issues, "MANGO-ARCH-FEIGN-001", javaClass,
                        "FeignClient must be located in starter-remote");
            }
            if (javaClass.isAnnotatedWith(FEIGN_CLIENT)) {
                checkFeign(javaClass, issues);
            }
        }
        issues.sort(Comparator.comparing(ArchitectureIssue::ruleId)
                .thenComparing(ArchitectureIssue::subject));
        return List.copyOf(issues);
    }

    private Map<Path, ModuleRole> normalizeAndValidate(Map<Path, ModuleRole> classDirectories) {
        if (classDirectories == null || classDirectories.isEmpty()) {
            throw new IllegalStateException("MANGO-ARCH-ENGINE-003 no Reactor class directories configured");
        }
        Map<Path, ModuleRole> result = new LinkedHashMap<>();
        classDirectories.forEach((path, role) -> {
            Path normalized = path.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalized)) {
                throw new IllegalStateException(
                        "MANGO-ARCH-ENGINE-003 missing compiled class directory: " + normalized);
            }
            result.put(normalized, role);
        });
        return result;
    }

    private ModuleRole roleOf(JavaClass javaClass, Map<Path, ModuleRole> roots) {
        URI uri = javaClass.getSource()
                .orElseThrow(() -> new IllegalStateException(
                        "MANGO-ARCH-ENGINE-004 class has no bytecode source: " + javaClass.getName()))
                .getUri();
        Path source = Path.of(uri).toAbsolutePath().normalize();
        return roots.entrySet().stream()
                .filter(entry -> source.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "MANGO-ARCH-ENGINE-004 class is outside Reactor roots: " + source));
    }

    private void checkController(
            JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (role != ModuleRole.STARTER
                && !(role == ModuleRole.STARTER_REMOTE
                && allowedReverseControllers.contains(javaClass.getName()))) {
            add(issues, "MANGO-ARCH-TYPE-001", javaClass,
                    "Controller must be located in starter");
        }
        boolean implementsApi = javaClass.getAllRawInterfaces().stream()
                .anyMatch(type -> type.getSimpleName().endsWith("Api"));
        if (!implementsApi) {
            add(issues, "MANGO-ARCH-TYPE-002", javaClass,
                    "Controller must implement a domain XxxApi interface");
        }
        for (JavaField field : javaClass.getAllFields()) {
            JavaClass type = field.getRawType();
            String name = type.getSimpleName();
            if (name.endsWith("Mapper") || name.endsWith("Entity")
                    || name.endsWith("ServiceImpl") || type.isAnnotatedWith(FEIGN_CLIENT)) {
                issues.add(new ArchitectureIssue(
                        "MANGO-ARCH-TYPE-003", field.getFullName(),
                        "Controller field must depend on a service interface, not " + type.getName()));
            }
        }
    }

    private void checkFeign(JavaClass javaClass, List<ArchitectureIssue> issues) {
        long apiCount = javaClass.getRawInterfaces().stream()
                .filter(type -> type.getSimpleName().endsWith("Api"))
                .count();
        if (apiCount != 1 || javaClass.getRawInterfaces().size() != 1) {
            add(issues, "MANGO-ARCH-FEIGN-002", javaClass,
                    "FeignClient must extend exactly one XxxApi");
        }
        var annotation = javaClass.getAnnotationOfType(FEIGN_CLIENT);
        if (!hasText(annotation.get("name").orElse(null))
                || !hasText(annotation.get("contextId").orElse(null))) {
            add(issues, "MANGO-ARCH-FEIGN-003", javaClass,
                    "FeignClient requires non-empty name and contextId");
        }
        Object path = annotation.get("path").orElse(null);
        if (!(path instanceof String stringPath) || !stringPath.startsWith("/")) {
            add(issues, "MANGO-ARCH-FEIGN-004", javaClass,
                    "FeignClient path must be an absolute HTTP path");
        }
    }

    private boolean hasText(Object value) {
        return value instanceof String text && !text.isBlank();
    }

    private void checkService(JavaClass javaClass, ModuleRole role, List<ArchitectureIssue> issues) {
        if (role != ModuleRole.CORE) {
            add(issues, "MANGO-ARCH-TYPE-004", javaClass,
                    "Service implementation must be located in core");
        }
        if (javaClass.getSimpleName().endsWith("ServiceImpl")) {
            boolean implementsService = javaClass.getAllRawInterfaces().stream()
                    .anyMatch(type -> type.getSimpleName().startsWith("I")
                            && type.getSimpleName().endsWith("Service"));
            if (!implementsService) {
                add(issues, "MANGO-ARCH-TYPE-005", javaClass,
                        "XxxServiceImpl must implement IXxxService");
            }
        }
        if (javaClass.getAllRawInterfaces().stream()
                .anyMatch(type -> type.getSimpleName().endsWith("Api"))) {
            add(issues, "MANGO-ARCH-TYPE-008", javaClass,
                    "Service implementation must not implement an HTTP API");
        }
    }

    private boolean isController(JavaClass javaClass) {
        return javaClass.isAnnotatedWith(REST_CONTROLLER);
    }

    private boolean isServiceImplementation(JavaClass javaClass) {
        return javaClass.isAnnotatedWith(SERVICE) || javaClass.getSimpleName().endsWith("ServiceImpl");
    }

    private boolean isMapper(JavaClass javaClass) {
        return javaClass.isAnnotatedWith(MAPPER) || javaClass.getSimpleName().endsWith("Mapper");
    }

    private boolean isEntity(JavaClass javaClass) {
        return javaClass.isAnnotatedWith(TABLE_NAME) || javaClass.getSimpleName().endsWith("Entity");
    }

    private void add(
            List<ArchitectureIssue> issues, String ruleId, JavaClass javaClass, String message) {
        issues.add(new ArchitectureIssue(ruleId, javaClass.getName(), message));
    }
}
