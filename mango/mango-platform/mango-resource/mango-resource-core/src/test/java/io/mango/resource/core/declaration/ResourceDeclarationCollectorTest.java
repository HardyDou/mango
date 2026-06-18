package io.mango.resource.core.declaration;

import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDeclarationCollectorTest {

    @Test
    void collectMergesFileAndCustomProviderDeclarations() {
        ResourceDeclaration file = declaration("1900000000000000001", "file");
        ResourceDeclaration provided = declaration("1900000000000000002", "provider");
        ResourceDeclaration discovered = declaration("1900000000000000003", "discovered");
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                providers(
                        provider(() -> List.of(file), List.of("file")),
                        provider(() -> List.of(provided), List.of("provider")),
                        provider(() -> List.of(discovered), List.of("discovered"))));

        List<ResourceDeclaration> declarations = collector.collect();

        assertThat(declarations)
                .extracting(ResourceDeclaration::getId)
                .containsExactly("1900000000000000001", "1900000000000000002", "1900000000000000003");
        assertThat(collector.managedModuleCodes(declarations))
                .containsExactly("file", "provider", "discovered");
    }

    private static ResourceDeclaration declaration(String id, String moduleCode) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType("MESSAGE_TEMPLATE");
        declaration.setModuleCode(moduleCode);
        declaration.setBizKey(moduleCode + ".resource.test");
        declaration.setTargetModule("notice");
        return declaration;
    }

    private static ResourceProvider provider(ResourceProvider provider, List<String> moduleCodes) {
        return new ResourceProvider() {
            @Override
            public List<String> moduleCodes() {
                return moduleCodes;
            }

            @Override
            public List<ResourceDeclaration> provide() {
                return provider.provide();
            }
        };
    }

    private static ObjectProvider<ResourceProvider> providers(ResourceProvider... providers) {
        return new ListObjectProvider<>(List.of(providers));
    }

    private static final class ListObjectProvider<T> implements ObjectProvider<T> {

        private final List<T> values;

        private ListObjectProvider(List<T> values) {
            this.values = values;
        }

        @Override
        public T getObject(Object... args) {
            return values.get(0);
        }

        @Override
        public T getIfAvailable() {
            return values.isEmpty() ? null : values.get(0);
        }

        @Override
        public T getIfUnique() {
            return values.size() == 1 ? values.get(0) : null;
        }

        @Override
        public T getObject() {
            return values.get(0);
        }

        @Override
        public Iterator<T> iterator() {
            return values.iterator();
        }

        @Override
        public Stream<T> stream() {
            return values.stream();
        }

        @Override
        public Stream<T> orderedStream() {
            return values.stream();
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            values.forEach(action);
        }
    }
}
