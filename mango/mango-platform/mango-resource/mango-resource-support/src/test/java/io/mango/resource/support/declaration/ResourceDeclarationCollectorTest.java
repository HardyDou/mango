package io.mango.resource.support.declaration;

import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.model.ResourceDeclaration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDeclarationCollectorTest {

    @Test
    void bootstrapCollectionExcludesRuntimeOnlyProviders() {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("stableProvider", provider("stable", true));
        beans.registerSingleton("runtimeProvider", provider("runtime", false));
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                beans.getBeanProvider(ResourceProvider.class));

        assertThat(collector.collectBootstrap())
                .extracting(ResourceDeclaration::getId)
                .containsExactly("stable-resource");
        assertThat(collector.managedBootstrapModuleCodes(collector.collectBootstrap()))
                .containsExactly("stable");
        assertThat(collector.collect())
                .extracting(ResourceDeclaration::getId)
                .containsExactlyInAnyOrder("stable-resource", "runtime-resource");
    }

    private static ResourceProvider provider(String moduleCode, boolean participatesInBootstrap) {
        return new ResourceProvider() {
            @Override
            public boolean participatesInBootstrap() {
                return participatesInBootstrap;
            }

            @Override
            public List<String> moduleCodes() {
                return List.of(moduleCode);
            }

            @Override
            public List<ResourceDeclaration> provide() {
                ResourceDeclaration declaration = new ResourceDeclaration();
                declaration.setId(moduleCode + "-resource");
                declaration.setModuleCode(moduleCode);
                return List.of(declaration);
            }
        };
    }
}
