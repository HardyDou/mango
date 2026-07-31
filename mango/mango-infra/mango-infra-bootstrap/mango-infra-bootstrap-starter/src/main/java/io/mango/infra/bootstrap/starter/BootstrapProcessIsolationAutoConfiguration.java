package io.mango.infra.bootstrap.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashSet;
import java.util.Set;

/** Prevents Runtime startup hooks and schedulers from running in a Bootstrap process. */
@AutoConfiguration(before = BootstrapAutoConfiguration.class)
@ConditionalOnProperty(prefix = "mango.bootstrap", name = "mode", havingValue = "bootstrap")
public class BootstrapProcessIsolationAutoConfiguration {

    private static final String BOOTSTRAP_COMMAND_RUNNER = "bootstrapCommandRunner";
    private static final String SCHEDULED_PROCESSOR =
            "org.springframework.context.annotation.internalScheduledAnnotationProcessor";

    @Bean
    static BeanDefinitionRegistryPostProcessor mangoBootstrapProcessIsolation() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                // Runner types are resolved after all configuration classes have registered their beans.
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
                    throw new IllegalStateException("Mango Bootstrap requires a BeanDefinitionRegistry");
                }
                Set<String> runtimeHooks = new LinkedHashSet<>();
                runtimeHooks.addAll(Set.of(beanFactory.getBeanNamesForType(ApplicationRunner.class, true, false)));
                runtimeHooks.addAll(Set.of(beanFactory.getBeanNamesForType(CommandLineRunner.class, true, false)));
                runtimeHooks.stream()
                        .filter(name -> !BOOTSTRAP_COMMAND_RUNNER.equals(name))
                        .filter(registry::containsBeanDefinition)
                        .forEach(registry::removeBeanDefinition);
                if (registry.containsBeanDefinition(SCHEDULED_PROCESSOR)) {
                    registry.removeBeanDefinition(SCHEDULED_PROCESSOR);
                }
            }
        };
    }
}
