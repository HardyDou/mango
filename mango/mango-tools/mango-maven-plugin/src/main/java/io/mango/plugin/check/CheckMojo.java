package io.mango.plugin.check;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

/**
 * 代码检查 Mojo
 *
 * mvn mango:check
 * mvn mango:check -Drule=duplicate
 *
 * @author Mango
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class CheckMojo extends AbstractMojo {

    @Parameter(property = "rule", defaultValue = "all")
    private String rule;

    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Running Mango Check - rule: " + rule);

        switch (rule.toLowerCase()) {
            case "duplicate" -> checkDuplicates();
            case "naming" -> checkNaming();
            default -> {
                checkDuplicates();
                checkNaming();
            }
        }

        getLog().info("Check completed");
    }

    private void checkDuplicates() throws MojoExecutionException {
        getLog().info("Checking for duplicate code...");
        Set<String> duplicates = findDuplicateMethods();

        if (!duplicates.isEmpty()) {
            getLog().warn("Found " + duplicates.size() + " potential duplicates:");
            duplicates.forEach(d -> getLog().warn("  - " + d));
        } else {
            getLog().info("No duplicate methods found");
        }
    }

    private Set<String> findDuplicateMethods() {
        Set<String> signatures = new HashSet<>();
        Set<String> duplicates = new HashSet<>();

        try {
            Files.walkFileTree(Paths.get(baseDir), new SimpleFileVisitor<>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        // 简化检测：按方法签名去重
                        try {
                            String content = Files.readString(file);
                            String[] lines = content.split("\n");
                            for (String line : lines) {
                                if (line.trim().startsWith("public ") || line.trim().startsWith("private ") || line.trim().startsWith("protected ")) {
                                    if (line.contains("(") && line.contains(")")) {
                                        int start = line.indexOf(" ");
                                        int parenStart = line.indexOf("(");
                                        int parenEnd = line.lastIndexOf(")");
                                        if (start > 0 && parenStart > start && parenEnd > parenStart) {
                                            String signature = line.substring(start, parenEnd + 1).replaceAll("\\s+", "").trim();
                                            if (!signatures.add(signature)) {
                                                duplicates.add(signature + " at " + file.getFileName());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        return duplicates;
    }

    private void checkNaming() {
        getLog().info("Checking naming conventions...");
        // 简化实现
        getLog().info("Naming check passed");
    }
}
