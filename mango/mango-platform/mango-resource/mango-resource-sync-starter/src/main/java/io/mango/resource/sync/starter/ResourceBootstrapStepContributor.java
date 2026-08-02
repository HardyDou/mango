package io.mango.resource.sync.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.result.R;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.model.ResourceDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResourceBootstrapStepContributor implements BootstrapStepContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceBootstrapStepContributor.class);

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ResourceDeclarationApi declarationApi;
    private final ResourceManifestSerializer manifestSerializer;
    private final ResourceDeclarationCanonicalizer canonicalizer;
    private final String applicationName;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The contributor intentionally retains Spring-managed configuration and mapper collaborators")
    public ResourceBootstrapStepContributor(ResourceRegistryProperties properties,
                                            ResourceDeclarationCollector collector,
                                            ResourceDeclarationApi declarationApi,
                                            ResourceManifestSerializer manifestSerializer,
                                            ResourceDeclarationCanonicalizer canonicalizer,
                                            String applicationName) {
        this.properties = properties;
        this.collector = collector;
        this.declarationApi = declarationApi;
        this.manifestSerializer = manifestSerializer;
        this.canonicalizer = canonicalizer;
        this.applicationName = applicationName;
    }

    @Override
    public List<BootstrapStep> contributeSteps() {
        PreparedDeclarations prepared = prepare();
        if (!properties.isEnabled() || prepared.declarations().isEmpty() && prepared.moduleCodes().isEmpty()) {
            return List.of();
        }
        return List.of(new ResourceStep("RESOURCE_REQUIRED", BootstrapPhase.EXPAND,
                        ResourceApplyMode.EXPAND, Set.of(), Set.of("FLYWAY_EXPAND"), prepared),
                new ResourceStep("RESOURCE_FINALIZE", BootstrapPhase.FINALIZE,
                        ResourceApplyMode.FINALIZE, Set.of("RESOURCE_REQUIRED"), Set.of(), prepared));
    }

    private PreparedDeclarations prepare() {
        List<ResourceDeclaration> declarations = collector.collectBootstrap().stream()
                .sorted(Comparator.comparing(ResourceDeclaration::getId))
                .toList();
        List<String> moduleCodes = collector.managedBootstrapModuleCodes(declarations).stream().sorted().toList();
        List<String> semanticInventory = declarations.stream()
                .map(declaration -> declaration.getId() + "=" + canonicalizer.fingerprint(declaration))
                .toList();
        PreparedDeclarations prepared = new PreparedDeclarations(declarations, moduleCodes,
                manifestSerializer.serialize(declarations), semanticInventory);
        LOG.debug("Mango resource bootstrap manifest computed: modules={}, declarations={}",
                prepared.moduleCodes(), prepared.declarations().size());
        return prepared;
    }

    private String appCode() {
        return StringUtils.hasText(properties.getRemote().getAppCode())
                ? properties.getRemote().getAppCode().trim() : requireApplicationName();
    }

    private String serviceCode() {
        return StringUtils.hasText(properties.getRemote().getServiceCode())
                ? properties.getRemote().getServiceCode().trim() : requireApplicationName();
    }

    private String requireApplicationName() {
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalStateException("Resource bootstrap application name is required");
        }
        return applicationName.trim();
    }

    private final class ResourceStep implements BootstrapStep {

        private final String code;
        private final BootstrapPhase phase;
        private final ResourceApplyMode applyMode;
        private final Set<String> dependencies;
        private final Set<String> optionalDependencies;
        private final PreparedDeclarations prepared;

        private ResourceStep(String code, BootstrapPhase phase, ResourceApplyMode applyMode,
                             Set<String> dependencies, Set<String> optionalDependencies,
                             PreparedDeclarations prepared) {
            this.code = code;
            this.phase = phase;
            this.applyMode = applyMode;
            this.dependencies = dependencies;
            this.optionalDependencies = optionalDependencies;
            this.prepared = prepared;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public BootstrapPhase phase() {
            return phase;
        }

        @Override
        public Set<String> dependencies() {
            return dependencies;
        }

        @Override
        public Set<String> optionalDependencies() {
            return optionalDependencies;
        }

        @Override
        public String fingerprintMaterial() {
            return "resource-v2|" + applyMode + "|" + appCode() + "|" + serviceCode() + "|"
                    + prepared.moduleCodes() + "|inventory=" + prepared.semanticInventory();
        }

        @Override
        public BootstrapStepResult execute(BootstrapExecutionContext context) {
            RegisterResourceDeclarationsCommand command = new RegisterResourceDeclarationsCommand();
            command.setAppCode(appCode());
            command.setServiceCode(serviceCode());
            command.setModuleCodes(prepared.moduleCodes());
            command.setDeclarations(prepared.json());
            command.setEnvironmentKey(context.environmentKey());
            command.setGeneration(context.generation());
            command.setManifestFingerprint(context.manifestFingerprint());
            command.setFencingToken(context.fencingToken());
            command.setApplyMode(applyMode);
            R<Boolean> response = declarationApi.registerDeclarations(command);
            if (response == null || !response.isSuccess() || !Boolean.TRUE.equals(response.getData())) {
                throw new IllegalStateException("Resource bootstrap did not complete: mode=" + applyMode
                        + ", response=" + (response == null ? "null" : response.getMsg()));
            }
            return new BootstrapStepResult("Resource " + applyMode + " synchronized",
                    Map.of("declarations", prepared.declarations().size(),
                            "managedModules", prepared.moduleCodes().size()));
        }
    }

    private record PreparedDeclarations(
            List<ResourceDeclaration> declarations,
            List<String> moduleCodes,
            String json,
            List<String> semanticInventory) {
    }
}
