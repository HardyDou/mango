package io.mango.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;

/** Imports Reactor subjects and dependency bytecode while preserving the actual check scope. */
final class ArchUnitImportScope {

    private static final String FILE_URI_SCHEME = "file";
    private final JavaClasses classes;
    private final Set<Path> subjectRoots;

    private ArchUnitImportScope(JavaClasses classes, Set<Path> subjectRoots) {
        this.classes = classes;
        this.subjectRoots = subjectRoots;
    }

    static ArchUnitImportScope load(
            Collection<Path> subjectDirectories, Collection<Path> dependencyClasspath) {
        Set<Path> subjectRoots = normalize(subjectDirectories);
        Set<Path> importPaths = new LinkedHashSet<>(subjectRoots);
        importPaths.addAll(normalize(dependencyClasspath));
        Set<String> expectedSubjects = indexClasses(subjectRoots, importPaths);
        JavaClasses classes;
        try {
            classes = new ClassFileImporter().importPaths(importPaths);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-002 ArchUnit failed to import Reactor bytecode", exception);
        }
        ArchUnitImportScope scope = new ArchUnitImportScope(classes, subjectRoots);
        scope.validateSubjects(expectedSubjects);
        return scope;
    }

    JavaClasses classes() {
        return classes;
    }

    static <T> T valueOf(JavaClass javaClass, Map<Path, T> roots) {
        Path source =
                Path.of(
                                javaClass
                                        .getSource()
                                        .orElseThrow(
                                                () ->
                                                        new IllegalStateException(
                                                                "MANGO-ARCH-ENGINE-004 class has no"
                                                                        + " bytecode source: "
                                                                        + javaClass.getName()))
                                        .getUri())
                        .toAbsolutePath()
                        .normalize();
        return roots.entrySet().stream()
                .filter(entry -> source.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    boolean isSubject(JavaClass javaClass) {
        Optional<com.tngtech.archunit.core.domain.Source> source = javaClass.getSource();
        if (source.isEmpty()
                || !FILE_URI_SCHEME.equalsIgnoreCase(source.get().getUri().getScheme())) {
            return false;
        }
        Path classFile = Path.of(source.get().getUri()).toAbsolutePath().normalize();
        return subjectRoots.stream().anyMatch(classFile::startsWith);
    }

    private static Set<Path> normalize(Collection<Path> paths) {
        Set<Path> normalized = new LinkedHashSet<>();
        paths.stream().map(path -> path.toAbsolutePath().normalize()).forEach(normalized::add);
        return normalized;
    }

    private static Set<String> indexClasses(Set<Path> subjects, Set<Path> importPaths) {
        Map<String, String> owners = new LinkedHashMap<>();
        Set<String> expectedSubjects = new LinkedHashSet<>();
        for (Path root : importPaths) {
            if (Files.isRegularFile(root) && root.getFileName().toString().endsWith(".jar")) {
                indexJar(root, owners);
                continue;
            }
            try (var files = Files.walk(root)) {
                for (Path classFile :
                        files.filter(Files::isRegularFile)
                                .filter(path -> path.getFileName().toString().endsWith(".class"))
                                .toList()) {
                    String className = className(root, classFile);
                    if ("module-info".equals(className)) {
                        continue;
                    }
                    registerClass(owners, className, classFile.toString());
                    if (subjects.contains(root)) {
                        expectedSubjects.add(className);
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "MANGO-ARCH-ENGINE-002 unable to inventory bytecode: " + root,
                        exception);
            }
        }
        if (expectedSubjects.isEmpty()) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-003 Reactor subject bytecode inventory is empty");
        }
        return expectedSubjects;
    }

    private static void indexJar(Path jar, Map<String, String> owners) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName())
                    .filter(name -> name.endsWith(".class"))
                    .filter(name -> !"module-info.class".equals(name))
                    .forEach(name -> registerClass(
                            owners,
                            name.substring(0, name.length() - ".class".length())
                                    .replace('/', '.'),
                            jar + "!/" + name));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-002 unable to inventory bytecode: " + jar,
                    exception);
        }
    }

    private static void registerClass(Map<String, String> owners, String className, String owner) {
        String duplicate = owners.putIfAbsent(className, owner);
        if (duplicate != null && !duplicate.equals(owner)) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-027 duplicate bytecode class "
                            + className
                            + ": "
                            + duplicate
                            + ", "
                            + owner);
        }
    }

    private void validateSubjects(Set<String> expectedSubjects) {
        Set<String> importedSubjects = new LinkedHashSet<>();
        for (JavaClass javaClass : classes) {
            if (isSubject(javaClass)) {
                importedSubjects.add(javaClass.getName());
            }
        }
        Set<String> missing = new LinkedHashSet<>(expectedSubjects);
        missing.removeAll(importedSubjects);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-028 Reactor subject bytecode was not imported: "
                            + missing);
        }
    }

    private static String className(Path root, Path classFile) {
        String relative =
                root.relativize(classFile).toString().replace(File.separatorChar, '/');
        return relative.substring(0, relative.length() - ".class".length()).replace('/', '.');
    }
}
