package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.bootstrap.api.BootstrapRuntimeAuthorityProvider;
import io.mango.infra.bootstrap.api.BootstrapWriteAuthority;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.api.enums.ResourceExecutionPhase;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resource eventual reconciliation worker tests")
class ResourceEventualReconciliationWorkerTest {

    private static final BootstrapWriteAuthority AUTHORITY = new BootstrapWriteAuthority(
            "test", 7L, "a".repeat(64), 11L);

    @Test
    @DisplayName("eventual reconciliation starts after synchronous startup registrars")
    void runsAtTheEndOfStartup() {
        ResourceEventualReconciliationWorker worker = worker(
                () -> Optional.empty(), command -> R.ok(Boolean.TRUE));

        assertThat(worker.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    @DisplayName("runtime without write authority must not mutate resources")
    void missingAuthoritySkipsReconciliation() {
        AtomicInteger attempts = new AtomicInteger();
        ResourceEventualReconciliationWorker worker = worker(
                () -> Optional.empty(), command -> {
                    attempts.incrementAndGet();
                    return R.ok(Boolean.TRUE);
                }, eventualDeclaration("2951300000000000001"));

        worker.reconcileOnce();

        assertThat(attempts).hasValue(0);
    }

    @Test
    @DisplayName("only runtime eventual declarations are fenced and submitted")
    void submitsOnlyRuntimeEventualDeclarations() {
        ResourceDeclaration required = eventualDeclaration("2951300000000000002");
        required.setExecutionPhase(ResourceExecutionPhase.BOOTSTRAP_REQUIRED);
        ResourceDeclaration eventual = eventualDeclaration("2951300000000000003");
        AtomicReference<RegisterResourceDeclarationsCommand> submitted = new AtomicReference<>();
        ResourceEventualReconciliationWorker worker = worker(
                () -> Optional.of(AUTHORITY), command -> {
                    submitted.set(command);
                    return R.ok(Boolean.TRUE);
                }, required, eventual);

        worker.reconcileOnce();

        RegisterResourceDeclarationsCommand command = submitted.get();
        assertThat(command).isNotNull();
        assertThat(command.getEnvironmentKey()).isEqualTo("test");
        assertThat(command.getGeneration()).isEqualTo(7L);
        assertThat(command.getManifestFingerprint()).isEqualTo("a".repeat(64));
        assertThat(command.getFencingToken()).isEqualTo(11L);
        assertThat(command.getApplyMode()).isEqualTo(ResourceApplyMode.EVENTUAL);
        assertThat(command.getDeclarations()).contains("2951300000000000003")
                .doesNotContain("2951300000000000002");
    }

    @Test
    @DisplayName("unchanged declarations are not submitted again after success")
    void unchangedDeclarationsSkipRemoteRegistration() {
        AtomicInteger attempts = new AtomicInteger();
        ResourceEventualReconciliationWorker worker = worker(
                () -> Optional.of(AUTHORITY), command -> {
                    attempts.incrementAndGet();
                    return R.ok(Boolean.TRUE);
                }, eventualDeclaration("2951300000000000004"));

        worker.reconcileOnce();
        worker.reconcileOnce();

        assertThat(attempts).hasValue(1);
    }

    @Test
    @DisplayName("a canonical declaration change is submitted after the previous success")
    void changedDeclarationsAreSubmittedAgain() {
        AtomicInteger attempts = new AtomicInteger();
        ResourceDeclaration declaration = eventualDeclaration("2951300000000000005");
        ResourceEventualReconciliationWorker worker = worker(
                () -> Optional.of(AUTHORITY), command -> {
                    attempts.incrementAndGet();
                    return R.ok(Boolean.TRUE);
                }, declaration);

        worker.reconcileOnce();
        declaration.setVersion(2);
        worker.reconcileOnce();

        assertThat(attempts).hasValue(2);
    }

    @Test
    @DisplayName("a new bootstrap authority submits unchanged declarations again")
    void changedAuthorityIsSubmittedAgain() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<BootstrapWriteAuthority> authority = new AtomicReference<>(AUTHORITY);
        ResourceEventualReconciliationWorker worker = worker(
                () -> Optional.of(authority.get()), command -> {
                    attempts.incrementAndGet();
                    return R.ok(Boolean.TRUE);
                }, eventualDeclaration("2951300000000000006"));

        worker.reconcileOnce();
        authority.set(new BootstrapWriteAuthority("test", 8L, "b".repeat(64), 12L));
        worker.reconcileOnce();

        assertThat(attempts).hasValue(2);
    }

    @Test
    @DisplayName("remote failure is contained and the next reconciliation can converge")
    void failureDoesNotStopNextReconciliation() {
        AtomicInteger attempts = new AtomicInteger();
        ResourceEventualReconciliationWorker worker = worker(
                () -> Optional.of(AUTHORITY), command -> attempts.incrementAndGet() == 1
                        ? R.fail("target dependency is not ready") : R.ok(Boolean.TRUE),
                eventualDeclaration("2951300000000000007"));

        worker.reconcileOnce();
        worker.reconcileOnce();
        worker.reconcileOnce();

        assertThat(attempts).hasValue(2);
    }

    private static ResourceEventualReconciliationWorker worker(
            BootstrapRuntimeAuthorityProvider authorityProvider,
            ResourceDeclarationApi declarationApi,
            ResourceDeclaration... declarations) {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        ResourceProvider provider = () -> List.of(declarations);
        ResourceDeclarationCollector collector = new ResourceDeclarationCollector(
                new ListObjectProvider<>(List.of(provider)));
        return new ResourceEventualReconciliationWorker(
                properties, collector, declarationApi, new ResourceManifestSerializer(),
                new ResourceDeclarationCanonicalizer(new ObjectMapper()),
                authorityProvider, "resource-service");
    }

    private static ResourceDeclaration eventualDeclaration(String id) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType("AUTH_MENU");
        declaration.setModuleCode("authorization");
        declaration.setBizKey("authorization.menu." + id);
        declaration.setTargetModule("authorization");
        declaration.setExecutionPhase(ResourceExecutionPhase.RUNTIME_EVENTUAL);
        return declaration;
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
