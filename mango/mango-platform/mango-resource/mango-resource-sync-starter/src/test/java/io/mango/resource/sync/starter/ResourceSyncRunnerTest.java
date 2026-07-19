package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.sync.ResourceSynchronizationCompletedEvent;
import io.mango.resource.support.sync.ResourceSynchronizationPrerequisitesReadyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resource sync runner tests")
class ResourceSyncRunnerTest {

    @Test
    @DisplayName("remote failure should not stop startup and scheduled retry should converge")
    void remoteFailureShouldRetryUntilSynchronized() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2951300000000000001");
        declaration.setVersion(1);
        declaration.setResourceType("AUTH_MENU");
        declaration.setModuleCode("authorization");
        declaration.setBizKey("authorization.menu");
        declaration.setTargetModule("authorization");
        ResourceProvider provider = () -> List.of(declaration);
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                new ListObjectProvider<>(List.of(provider)));
        AtomicInteger attempts = new AtomicInteger();
        ResourceDeclarationApi api = new ResourceDeclarationApi() {
            @Override
            public R<Boolean> registerDeclarations(RegisterResourceDeclarationsCommand command) {
                int currentAttempt = attempts.incrementAndGet();
                if (currentAttempt == 1) {
                    return R.fail("target dependency is not ready");
                }
                if (currentAttempt == 2) {
                    return R.ok(Boolean.FALSE);
                }
                return R.ok(Boolean.TRUE);
            }
        };
        List<Object> events = new ArrayList<>();
        ResourceSyncRunner runner = new ResourceSyncRunner(
                properties, collector, api, new ObjectMapper(), "authorization-service", events::add);

        runner.run(new DefaultApplicationArguments(new String[0]));
        assertThat(runner.isSynchronizationComplete()).isFalse();
        runner.onSynchronizationPrerequisitesReady(new ResourceSynchronizationPrerequisitesReadyEvent());
        runner.retryUntilSynchronized();
        runner.retryUntilSynchronized();

        assertThat(attempts).hasValue(3);
        assertThat(runner.isSynchronizationComplete()).isTrue();
        assertThat(events).singleElement().isInstanceOf(ResourceSynchronizationCompletedEvent.class);
    }

    private static final class ListObjectProvider<T> implements ObjectProvider<T> {

        private final List<T> values;

        private ListObjectProvider(List<T> values) {
            this.values = values;
        }

        @Override
        public T getObject(Object... args) {
            return values.getFirst();
        }

        @Override
        public T getIfAvailable() {
            return values.isEmpty() ? null : values.getFirst();
        }

        @Override
        public T getIfUnique() {
            return values.size() == 1 ? values.getFirst() : null;
        }

        @Override
        public T getObject() {
            return values.getFirst();
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
