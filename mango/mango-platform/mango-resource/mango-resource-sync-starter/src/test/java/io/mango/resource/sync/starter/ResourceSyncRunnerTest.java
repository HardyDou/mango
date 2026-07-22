package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.InvalidResourceDeclarationException;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.sync.ResourceSynchronizationCompletedEvent;
import io.mango.resource.support.sync.ResourceSynchronizationPrerequisitesReadyEvent;
import io.mango.resource.support.sync.StartupReadinessChangedEvent;
import io.mango.resource.support.sync.StartupReadinessState;
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
        runner.onSynchronizationPrerequisitesReady(new ResourceSynchronizationPrerequisitesReadyEvent());

        assertThat(attempts).hasValue(3);
        assertThat(runner.isSynchronizationComplete()).isTrue();
        assertThat(runner.getReadinessState()).isEqualTo(StartupReadinessState.READY);
        assertThat(events.stream().filter(ResourceSynchronizationCompletedEvent.class::isInstance)).hasSize(1);
    }

    @Test
    @DisplayName("permanent failure should execute once until declaration snapshot changes")
    void permanentFailureShouldWaitForSnapshotChange() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2951300000000000002");
        declaration.setVersion(1);
        declaration.setResourceType("ORG_POST");
        declaration.setModuleCode("org");
        declaration.setBizKey("org.post.admin");
        declaration.setTargetModule("org");
        ResourceProvider provider = () -> List.of(declaration);
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                new ListObjectProvider<>(List.of(provider)));
        AtomicInteger attempts = new AtomicInteger();
        ResourceDeclarationApi api = command -> {
            attempts.incrementAndGet();
            return R.fail(409, "declaration conflict");
        };
        ResourceSyncRunner runner = new ResourceSyncRunner(
                properties, collector, api, new ObjectMapper(), "org-service", event -> { });

        runner.run(new DefaultApplicationArguments(new String[0]));
        runner.retryUntilSynchronized();
        runner.onSynchronizationPrerequisitesReady(new ResourceSynchronizationPrerequisitesReadyEvent());

        assertThat(attempts).hasValue(1);
        assertThat(runner.getReadinessState()).isEqualTo(StartupReadinessState.PERMANENT_FAILED);

        declaration.setVersion(2);
        runner.retryUntilSynchronized();

        assertThat(attempts).hasValue(2);
    }

    @Test
    @DisplayName("invalid local declaration should remain permanently failed until provider content changes")
    void invalidLocalDeclarationShouldNotReachRemoteApiAndShouldRecoverAfterChange() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        AtomicInteger providerAttempts = new AtomicInteger();
        ResourceDeclaration valid = new ResourceDeclaration();
        valid.setId("2951300000000000003");
        valid.setVersion(1);
        valid.setResourceType("AUTH_MENU");
        valid.setModuleCode("authorization");
        valid.setBizKey("authorization.valid");
        valid.setTargetModule("authorization");
        ResourceProvider provider = () -> {
            if (providerAttempts.incrementAndGet() < 3) {
                throw new InvalidResourceDeclarationException("same invalid declaration");
            }
            return List.of(valid);
        };
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                new ListObjectProvider<>(List.of(provider)));
        AtomicInteger remoteAttempts = new AtomicInteger();
        ResourceDeclarationApi api = command -> {
            remoteAttempts.incrementAndGet();
            return R.ok(Boolean.TRUE);
        };
        List<Object> events = new ArrayList<>();
        ResourceSyncRunner runner = new ResourceSyncRunner(
                properties, collector, api, new ObjectMapper(), "authorization-service", events::add);

        runner.run(new DefaultApplicationArguments(new String[0]));
        runner.retryUntilSynchronized();

        assertThat(remoteAttempts).hasValue(0);
        assertThat(events.stream()
                .filter(StartupReadinessChangedEvent.class::isInstance)
                .map(StartupReadinessChangedEvent.class::cast)
                .filter(event -> event.state() == StartupReadinessState.PERMANENT_FAILED))
                .hasSize(1);

        runner.retryUntilSynchronized();

        assertThat(remoteAttempts).hasValue(1);
        assertThat(runner.getReadinessState()).isEqualTo(StartupReadinessState.READY);
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
