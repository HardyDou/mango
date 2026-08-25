package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.FileResourceProvider;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates Resource build artifacts from an intentionally minimal, database-free Spring context.
 *
 * <p>The caller may add deterministic {@link ResourceProvider} beans through
 * {@code mango.resource.registry.artifact-context-sources}. This launcher deliberately does not
 * enable component scanning or Spring Boot auto-configuration.</p>
 */
public final class ResourceManifestBuildApplication {

    static final String OUTPUT_DIRECTORY_PROPERTY =
            "mango.resource.registry.artifact-output-directory";
    static final String CONTEXT_SOURCES_PROPERTY =
            "mango.resource.registry.artifact-context-sources";

    private ResourceManifestBuildApplication() {
    }

    /**
     * Runs the deterministic manifest generator and exits after the files are durable.
     *
     * @param args Spring-style {@code --name=value} build arguments
     */
    public static void main(String[] args) {
        ConfigurableEnvironment environment = buildEnvironment(args);
        Path outputDirectory = outputDirectory(environment);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.register(ResourceManifestBuildConfiguration.class);
            contextSources(environment).forEach(context::register);
            context.refresh();
            context.getBean(ResourceManifestArtifactWriter.class).write(outputDirectory);
        }
    }

    private static ConfigurableEnvironment buildEnvironment(String[] args) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource(args));
        return environment;
    }

    private static Path outputDirectory(Environment environment) {
        String configured = environment.getProperty(OUTPUT_DIRECTORY_PROPERTY);
        if (!StringUtils.hasText(configured)) {
            throw new IllegalStateException(OUTPUT_DIRECTORY_PROPERTY + " is required");
        }
        try {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(OUTPUT_DIRECTORY_PROPERTY + " is invalid: " + configured,
                    exception);
        }
    }

    private static List<Class<?>> contextSources(Environment environment) {
        String configured = environment.getProperty(CONTEXT_SOURCES_PROPERTY);
        if (!StringUtils.hasText(configured)) {
            return List.of();
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> classNames = new LinkedHashSet<>();
        for (String token : configured.split(",")) {
            if (StringUtils.hasText(token)) {
                classNames.add(token.trim());
            }
        }
        List<Class<?>> sources = new ArrayList<>();
        classNames.forEach(className -> sources.add(loadSource(className, classLoader)));
        return List.copyOf(sources);
    }

    private static Class<?> loadSource(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resource artifact context source is not loadable: " + className,
                    exception);
        }
    }

    /** Minimal bean graph used only by the build-time launcher. */
    @Configuration(proxyBeanMethods = false)
    static class ResourceManifestBuildConfiguration {

        @Bean
        ObjectMapper resourceManifestObjectMapper() {
            return JsonMapper.builder().findAndAddModules().build();
        }

        @Bean
        ResourceRegistryProperties resourceRegistryProperties(Environment environment) {
            return Binder.get(environment)
                    .bind("mango.resource.registry", ResourceRegistryProperties.class)
                    .orElseGet(ResourceRegistryProperties::new);
        }

        @Bean
        ResourceDeclarationCanonicalizer resourceDeclarationCanonicalizer(ObjectMapper objectMapper) {
            return new ResourceDeclarationCanonicalizer(objectMapper);
        }

        @Bean
        ResourceDeclarationLoader resourceDeclarationLoader(ObjectMapper objectMapper,
                                                            ResourceRegistryProperties properties) {
            return new ResourceDeclarationLoader(objectMapper, properties);
        }

        @Bean
        FileResourceProvider fileResourceProvider(ResourceDeclarationLoader loader) {
            return new FileResourceProvider(loader);
        }

        @Bean
        ResourceDeclarationCollector resourceDeclarationCollector(ObjectProvider<ResourceProvider> providers) {
            return new ResourceDeclarationCollector(providers);
        }

        @Bean
        ResourceManifestSerializer resourceManifestSerializer() {
            return new ResourceManifestSerializer();
        }

        @Bean
        ResourceManifestArtifactWriter resourceManifestArtifactWriter(
                ResourceDeclarationCollector collector,
                ResourceManifestSerializer serializer,
                ResourceDeclarationCanonicalizer canonicalizer,
                ResourceLoader resourceLoader,
                Environment environment) {
            return new ResourceManifestArtifactWriter(
                collector, serializer, canonicalizer, resourceLoader, environment);
        }
    }
}
