package io.mango.plugin.check;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 从插件自身 jar 提取内置规则集。
 *
 * <p>业务仓以 {@code io.mango:mango-parent} 作为 parent 时，仓库里没有 Mango 主仓的
 * {@code mango-tools/...} 目录，不能再用文件系统路径引用规则集；规则集必须来自插件 jar。</p>
 */
final class BundledRulesetResolver {

    static final String CHECKSTYLE_RESOURCE = "rulesets/java/checkstyle.xml";
    static final String SPOTBUGS_RESOURCE = "rulesets/java/spotbugs-security.xml";
    static final String PMD_RESOURCE = "rulesets/java/pmd-mango.xml";
    static final String OUTPUT_DIRECTORY = "target/mango-check";

    private final ClassLoader classLoader;

    BundledRulesetResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 解析规则集路径：显式配置 &gt; rulesetDir &gt; 项目 config/quality 文件 &gt; 内置规则集。
     */
    String resolve(Path rootPath, String explicitLocation, String rulesetDir, String resource,
                   String outputName, String... projectCandidates) throws MojoExecutionException {
        String resolved = resolveExplicitOrProject(rootPath, explicitLocation, rulesetDir, outputName, projectCandidates);
        if (resolved != null) {
            return resolved;
        }
        return extract(rootPath, resource, outputName);
    }

    /**
     * 解析显式配置、rulesetDir 或项目级规则文件，均不存在时返回 {@code null}。
     */
    String resolveExplicitOrProject(Path rootPath, String explicitLocation, String rulesetDir,
                                    String outputName, String... projectCandidates) {
        String explicit = normalized(explicitLocation);
        if (explicit != null) {
            return resolveExistingFile(rootPath, explicit);
        }
        String fromDirectory = resolveFromDirectory(rootPath, rulesetDir, outputName);
        if (fromDirectory != null) {
            return fromDirectory;
        }
        return resolveProjectFile(rootPath, projectCandidates);
    }

    /**
     * 从统一的规则目录解析指定文件名，目录未配置或文件不存在时返回 {@code null}。
     */
    String resolveFromDirectory(Path rootPath, String rulesetDir, String fileName) {
        String directory = normalized(rulesetDir);
        if (directory == null) {
            return null;
        }
        Path configuredDirectory = Path.of(directory);
        Path resolvedDirectory = configuredDirectory.isAbsolute()
                ? configuredDirectory.normalize()
                : rootPath.resolve(configuredDirectory).normalize();
        Path candidate = resolvedDirectory.resolve(fileName).normalize();
        return Files.isRegularFile(candidate) ? candidate.toAbsolutePath().toString() : null;
    }

    /**
     * 在业务仓 config/quality 下按顺序查找规则文件，未命中时返回 {@code null}。
     */
    String resolveProjectFile(Path rootPath, String... candidates) {
        for (String candidate : candidates) {
            Path path = rootPath.resolve(candidate).normalize();
            if (Files.isRegularFile(path)) {
                return path.toAbsolutePath().toString();
            }
        }
        return null;
    }

    String extract(Path rootPath, String resource, String outputName) throws MojoExecutionException {
        Path output = rootPath.resolve(OUTPUT_DIRECTORY).resolve(outputName).normalize();
        try (InputStream input = classLoader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new MojoExecutionException("Bundled Mango ruleset is missing: " + resource);
            }
            Files.createDirectories(output.getParent());
            Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
            return output.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new MojoExecutionException("Failed to prepare bundled Mango ruleset: " + resource, exception);
        }
    }

    private String resolveExistingFile(Path rootPath, String configuredLocation) {
        Path configuredPath = Path.of(configuredLocation);
        Path resolvedPath = configuredPath.normalize();
        if (!configuredPath.isAbsolute()) {
            resolvedPath = rootPath.resolve(configuredPath).normalize();
        }
        if (Files.isRegularFile(resolvedPath)) {
            return resolvedPath.toAbsolutePath().toString();
        }
        return configuredLocation;
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
